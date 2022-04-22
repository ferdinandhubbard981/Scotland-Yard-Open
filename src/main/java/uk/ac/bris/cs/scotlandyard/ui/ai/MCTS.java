package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.javatuples.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;

import java.util.*;

public class MCTS {
    private static final float EXPLORATIONCONSTANT = (float) Math.sqrt(2);
    Game game;
    NeuralNet nnet;
    Map<Pair<String, Integer>, Float> qsa; //Pair<GameState, Move> -> QValue
    Map<Pair<String, Integer>, Integer> nsa; //Pair<GameState, Move> -> numOfVisits
    Map<String, Integer> ns; //GameState -> numOfVisits
    Map<String, List<Float>> ps; //GameState -> policy
    Map<String, Integer> es; //GameState -> (int)?gameEnded?
    Map<String, List<Integer>> vs; //GameState -> valid moves
    Map<String, Boolean> pxs; //GameState -> ?parent maximising for mrX?

    public MCTS(Game game, NeuralNet nnet) {
        this.game = game;
        this.nnet = nnet;
        this.qsa = new HashMap<>();
        this.nsa = new HashMap<>();
        this.ns = new HashMap<>();
        this.ps = new HashMap<>();
        this.es = new HashMap<>();
        this.vs = new HashMap<>();
        this.pxs = new HashMap<>();
    }

    public List<Float> getActionProb(Board.GameState gameState, int numOfSims) {
        //performs $numOfSims iterations of MCTS from $gameState
        //returns policy vector

        //perform MCTS searches
        for (int i = 0; i < numOfSims; i++) this.search(gameState);
        String s = this.game.stringRepresentation(gameState);

        //get moveMap of MoveVisits
        List<Integer> counts = new ArrayList<>();
        for (int a = 0; a < game.getActionSize(); a++) {
            Pair<String, Integer> pair = new Pair<>(s, a);
            if (this.nsa.containsKey(pair)) counts.add(nsa.get(pair));
            else counts.add(0);
        }

        //TODO flatten function?

        Float countsSum = counts.stream().reduce(0, (total, element) -> total += element).floatValue();
        List<Float> probs = counts.stream().map(x -> x / countsSum).toList();
        return probs;
    }

    public Float search(Board.GameState gameState) {
        //update valid moves so we only have to do it once
        this.game.setValidMoves(gameState);
        String s = this.game.stringRepresentation(gameState);

        if (!this.es.containsKey(s)) this.es.put(s, this.game.getGameEnded(gameState));
        if (this.es.get(s) != 0) {
            if (this.pxs.get(s) != this.game.isMaximisingForMrX()) return -1 * this.es.get(s).floatValue();
            else return this.es.get(s).floatValue();
        }

        if (!this.ps.containsKey(s)) {
            //this means s is a leaf node
            Pair<List<Float>, Float> predPair = this.nnet.predict(gameState);
            this.ps.put(s, predPair.getValue0());
            float v = predPair.getValue1();
            List<Integer> valids = this.game.getValidMoveIndexes(gameState);
            List<Float> maskedPolicy = this.ps.get(s);
            for (int i = 0; i < maskedPolicy.size(); i++) {
                if (valids.get(i) == 0) maskedPolicy.set(i, 0f);
            }
            this.ps.put(s, maskedPolicy); //removing invalid moves from policy (that came from nnet)
            //sum of policy
            Float pssSum = this.ps.get(s).stream().reduce(0f, (total, element) -> total += element);
            //normalize policy
            if (pssSum > 0) this.ps.put(s, this.ps.get(s).stream().map(val -> val / pssSum).toList());
            else {
                System.err.println("\n\nall moves were masked nnet might be inadequate\n\n");
                //putting valids as policy + normalizing them
                this.ps.put(s, valids.stream().map(val -> val / pssSum).toList());
            }
            this.vs.put(s, valids);
            this.ns.put(s, 0);
        }

        List<Integer> valids = this.vs.get(s);
        float bestMoveVal = Float.MIN_VALUE;
        int bestMoveIndex = -1;

        //pick move with highest ucbval
        //TODO split into functions
        for (int moveIndex = 0; moveIndex < this.game.getActionSize(); moveIndex++) {
            if (valids.get(moveIndex) != 0) {
                Pair<String, Integer> stateActionPair = new Pair<>(s, moveIndex);
                float u = 0;
                if (this.qsa.containsKey(stateActionPair)) {
                    u = (float) (this.qsa.get(stateActionPair) + EXPLORATIONCONSTANT * this.ps.get(s).get(moveIndex) * Math.sqrt(this.ns.get(s))
                            / (1 + this.nsa.get(stateActionPair)));
                }
                else u = (float)(EXPLORATIONCONSTANT * this.ps.get(s).get(moveIndex) * Math.sqrt(this.ns.get(s) + 1e-8));
                if (u > bestMoveVal) {
                    bestMoveVal = u;
                    bestMoveIndex = moveIndex;
                }
            }
        }
        Board.GameState nextState = this.game.getNextState(gameState, bestMoveIndex);
        //updating parent var of nextState
        this.pxs.put(this.game.stringRepresentation(nextState), (this.game.isMaximisingForMrX()));
        float v = this.search(nextState);
        Pair<String, Integer> stateActionPair = new Pair<>(s, bestMoveIndex);
        if (this.qsa.containsKey(stateActionPair)) {
            Float newQVal = (this.nsa.get(stateActionPair) * this.qsa.get(stateActionPair) + v) / (this.nsa.get(stateActionPair) + 1);
            this.qsa.put(stateActionPair, newQVal);
            this.nsa.put(stateActionPair, this.nsa.get(stateActionPair)+1);
        }
        else {
            this.qsa.put(stateActionPair, v);
            this.nsa.put(stateActionPair, 1);
        }
        this.ns.put(s, this.ns.get(s)+1);
        if (this.pxs.get(s) != this.game.isMaximisingForMrX()) return -1 * this.es.get(s).floatValue();
        else return this.es.get(s).floatValue();
    }
}
