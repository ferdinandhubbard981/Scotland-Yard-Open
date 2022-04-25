package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.javatuples.Pair;

import java.util.*;

import static uk.ac.bris.cs.scotlandyard.ui.ai.Game.POSSIBLEMOVES;

public class MCTS {
    private static final float EXPLORATIONCONSTANT = (float) Math.sqrt(2);
    NeuralNet mrXNnet;
    NeuralNet detNnet;
    Game permGame;
    Map<Pair<String, Integer>, Float> qsa; //Pair<GameState, Move> -> QValue
    Map<Pair<String, Integer>, Integer> nsa; //Pair<GameState, Move> -> numOfVisits
    Map<String, Integer> ns; //GameState -> numOfVisits
    Map<String, List<Float>> ps; //GameState -> policy
    Map<String, List<Integer>> vs; //GameState -> valid moves
    Map<String, Integer> es; // GameState -> gameStatus 0 = running 1 = current won, -1 = current lost
    Map<String, Boolean> pxs; //GameState -> ?parent current player is mrX?

    public MCTS(NeuralNet mrXNnet, NeuralNet detNnet) {
        //to ensure I typed them the right way around
        assert(mrXNnet.isMrX);
        assert (!detNnet.isMrX);
        //initializing variables
        this.mrXNnet = mrXNnet;
        this.detNnet = detNnet;
        this.qsa = new HashMap<>();
        this.nsa = new HashMap<>();
        this.ns = new HashMap<>();
        this.ps = new HashMap<>();
        this.vs = new HashMap<>();
        this.es = new HashMap<>();
        this.pxs = new HashMap<>();
    }

    public List<Float> getActionProb(Game game, int numOfSims) {
        this.permGame = game;
        //performs $numOfSims iterations of MCTS from $gameState
        //returns policy vector

        //perform MCTS searches
        for (int i = 0; i < numOfSims; i++) this.search();
        final String s = this.permGame.stringRepresentation();

        //get moveMap of MoveVisits
        List<Integer> counts = new ArrayList<>();
        for (int a = 0; a < POSSIBLEMOVES; a++) {
            Pair<String, Integer> pair = new Pair<>(s, a);
            if (this.nsa.containsKey(pair)) counts.add(nsa.get(pair));
            else counts.add(0);
        }

        //TODO flatten function?

        Float countsSum = counts.stream().reduce(0, (total, element) -> total += element).floatValue();
        List<Float> probs = counts.stream().map(x -> x / countsSum).toList();
        return probs;
    }

    public Integer search() {
        //string representation of gamestate for hashmaps
        final String s = this.permGame.stringRepresentation();

        //check game ended
        if (es.containsKey(s)) es.put(s, this.permGame.getGameEnded());
        if (es.get(s) != 0) {
            if (this.pxs.get(s) != this.permGame.currentIsMrX) return -1 * es.get(s);
            else return es.get(s);
        }

        //check if leaf node
        if (!this.ps.containsKey(s)) {
            //this means s is a leaf node
            Pair<List<Float>, Float> predPair;
            if (this.permGame.currentIsMrX) predPair = this.mrXNnet.predict(new NnetInput(this.permGame));
            else predPair = this.detNnet.predict(new NnetInput(this.permGame));
            this.ps.put(s, predPair.getValue0());
            float v = predPair.getValue1();
            List<Integer> validMoveTable = this.permGame.getValidMoveTable();
            List<Float> maskedPolicy = this.ps.get(s);
            //policy is masked by setting invalid moves to a policy value of 0f (nnet is not 100% accurate)
            for (int i = 0; i < maskedPolicy.size(); i++) {
                if (validMoveTable.get(i) == 0) maskedPolicy.set(i, 0f);
            }
//          update policy in hashmap with the new masked policy
            this.ps.put(s, maskedPolicy);
            //sum of policy
            Float pssSum = this.ps.get(s).stream().reduce(0f, (total, element) -> total += element);
            //normalize policy
            if (pssSum > 0) this.ps.put(s, this.ps.get(s).stream().map(val -> val / pssSum).toList());
            else {
                System.err.println("\n\nall moves were masked nnet might be inadequate\n\n");
                //putting validMoveTable as policy + normalizing them
                this.ps.put(s, validMoveTable.stream().map(val -> val / pssSum).toList());
            }
            this.vs.put(s, validMoveTable);
            this.ns.put(s, 0);
        }

        List<Integer> validMoveIndexes = this.vs.get(s);
        float bestUCBVal = Float.MIN_VALUE;
        int bestMoveIndex = -1;

        //pick move with highest ucbval
        //TODO split into functions
        for (int moveIndex = 0; moveIndex < POSSIBLEMOVES; moveIndex++) {
            if (validMoveIndexes.get(moveIndex) != 0) {
                Pair<String, Integer> stateActionPair = new Pair<>(s, moveIndex);
                float ucbVal;
                if (this.qsa.containsKey(stateActionPair)) {
                    ucbVal = (float) (this.qsa.get(stateActionPair) + EXPLORATIONCONSTANT * this.ps.get(s).get(moveIndex) * Math.sqrt(this.ns.get(s))
                            / (1 + this.nsa.get(stateActionPair)));
                }
                else ucbVal = (float)(EXPLORATIONCONSTANT * this.ps.get(s).get(moveIndex) * Math.sqrt(this.ns.get(s) + 1e-8));
                if (ucbVal > bestUCBVal) {
                    bestUCBVal = ucbVal;
                    bestMoveIndex = moveIndex;
                }
            }
        }
        Boolean currentIsMrX = this.permGame.currentIsMrX; //saving who current player is because this.game is updated to next state
        this.permGame.getNextState(bestMoveIndex); //AT THIS POINT THIS.PERMGAME IS UPDATED to the next state

        //storing current player of next state parent in hashmap
        this.pxs.put(this.permGame.stringRepresentation(), this.permGame.currentIsMrX);

        //TODO recursion optimization. Delete game instance for this when we go to next
        Integer v = this.search();
        Pair<String, Integer> stateActionPair = new Pair<>(s, bestMoveIndex);
        if (this.qsa.containsKey(stateActionPair)) {
            //is not leaf node
            //updating qval and nval
            //(n * q + v) / (n+1)
            Float newQVal = (this.nsa.get(stateActionPair) * this.qsa.get(stateActionPair) + v) / (this.nsa.get(stateActionPair) + 1);

            this.qsa.put(stateActionPair, newQVal);
            this.nsa.put(stateActionPair, this.nsa.get(stateActionPair)+1);
        }
        else {
            //is leaf node
            //creating qval and nval
            this.qsa.put(stateActionPair, v.floatValue());
            this.nsa.put(stateActionPair, 1);
        }
        this.ns.put(s, this.ns.get(s)+1);
        if (this.pxs.get(s) != currentIsMrX) return -1 * v;
        else return v;
    }
}
