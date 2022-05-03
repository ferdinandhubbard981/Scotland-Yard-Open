package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.javatuples.Pair;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static uk.ac.bris.cs.scotlandyard.ui.ai.Game.POSSIBLEMOVES;

public class MCTS {
    private static final float EXPLORATIONCONSTANT = (float) Math.sqrt(2);
    private static final long MINMILLISECREMAINING = 1000;
    NeuralNet mrXNnet;
    NeuralNet detNnet;
    Game game;
    Map<Pair<String, Integer>, Float> qsa; //Pair<GameState, Move> -> QValue
    Map<Pair<String, Integer>, Integer> nsa; //Pair<GameState, Move> -> numOfVisits
    Map<String, Integer> ns; //GameState -> numOfVisits
    Map<String, Map<Integer, Float>> ps; //GameState -> policy only record non 0 values
    Map<String, Set<Integer>> vs; //GameState -> validMoveIndexes only on non 0 values
    Map<String, Integer> es; // GameState -> gameStatus 0 = running 1 = current won, -1 = current lost
    Map<String, Boolean> pxs; //GameState -> ?parent current player is mrX?

    public MCTS(NeuralNet mrXNnet, NeuralNet detNnet) {
        //to ensure I typed them the right way around
        if (!mrXNnet.isMrX) throw new IllegalArgumentException();
        if (detNnet.isMrX) throw new IllegalArgumentException();
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

    public List<Float> getActionProb(Game game, int numOfSims, long endTime) {
        numOfSims = (endTime != 0) ? (int)Math.pow(10, 7) : numOfSims;
        final Game permGame = new Game(game); //this game never changes so we can always refer back to the root node
        //if time != 0 then numOfSims is ignored
        this.game = new Game(permGame);
        //performs $numOfSims iterations of MCTS from $gameState
        //returns policy vector

        final String s = this.game.stringRepresentation();
        //initializing parent isCurrentMrX of root node. This has not effect it's just to fix an error. we don't care about the output of this.search() for root node
        this.pxs.put(s, true);
        //perform MCTS searches
        int i = 0;
        for (; i < numOfSims && hasTimeLeft(endTime); i++) {
//            if (!s.equals(this.game.stringRepresentation())) throw new IllegalArgumentException();
            //perform single MCTS simulation
            long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long freeMemory = Runtime.getRuntime().maxMemory() - usedMemory;
            if (freeMemory > 1 * Math.pow(10, 8)) {//10MB
                this.search();
            }
            else {
                System.out.printf("out of memory\n");
                this.ps = null;
                this.ns = null;
                this.qsa = null;
                break;
            }
                //free up some memory

            //reset game
            this.game = new Game(permGame);
        }

        //get moveMap of MoveVisits & normalize
        Float countsSum = nsa.values().stream().reduce(0, (total, element) -> total += element).floatValue();
        List<Float> probs = new ArrayList<>(Collections.nCopies(POSSIBLEMOVES, 0f));
        for (Pair<String, Integer> key : nsa.keySet()) {
            probs.set(key.getValue1(), nsa.get(key).floatValue()/countsSum);
        }
        // there should be no invalid moves here
        return probs;
    }

    public Float search() {
        //string representation of gamestate for hashmaps
        final String s = this.game.stringRepresentation();

        //check game ended
        if (!es.containsKey(s)) es.put(s, this.game.getGameEnded());
        if (es.get(s) != 0) {
            if (this.pxs.get(s) != this.game.currentIsMrX) return -1 * es.get(s).floatValue();
            else return es.get(s).floatValue();
        }

        //check if leaf node
        if (!this.ps.containsKey(s)) {
            //this means s is a leaf node

            //update valid move indexes
            List<Integer> validMoveTable = this.game.getValidMoveTable();
            Set<Integer> validMoveIndexes = validMoveTable.stream().map(val -> (val != 0) ? validMoveTable.indexOf(val) : -1) //set all 1 val to their own index and 0 to -1
                    .filter(val -> val != -1).collect(Collectors.toSet()); //remove -1 values
            this.vs.put(s, validMoveIndexes);

            //make prediction
            Pair<List<Float>, Float> predPair;
            if (this.game.currentIsMrX) predPair = this.mrXNnet.predict(new NnetInput(this.game));
            else predPair = this.detNnet.predict(new NnetInput(this.game));
            float v = predPair.getValue1();

            //put non-0 moves in hashmap
            Map<Integer, Float> validMovePolicyMap = new HashMap<>();
            for (int index : validMoveIndexes) validMovePolicyMap.put(index, predPair.getValue0().get(index));
            for (int i = 0; i < POSSIBLEMOVES; i++) {
                if (predPair.getValue0().get(i) != 0) validMovePolicyMap.put(i, predPair.getValue0().get(i));
            }

            //sum of policy
            Float pssSum = validMovePolicyMap.values().stream().reduce(0f, (total, element) -> total += element);
            //normalize policy
            if (pssSum > 0) {
                for (Integer index : validMovePolicyMap.keySet()) {
                    validMovePolicyMap.compute(index, (k, val) -> val/pssSum);
                }
            }
            else {
                System.err.println("all output moves were invalid, nnet might be inadequate\n");
                //putting validMoveTable as policy + normalizing them
                for (int validIndex : validMoveIndexes) {
                    //add normalized valid move
                    validMovePolicyMap.put(validIndex, 1f/validMoveIndexes.size());
                }
            }
            this.ps.put(s, validMovePolicyMap);
            this.ns.put(s, 0);
            if (this.pxs.get(s) != this.game.currentIsMrX) return -1 * v;
            else return v;
        }

        Set<Integer> validMoveTable = this.vs.get(s);
        if (validMoveTable.isEmpty())
            throw new IllegalArgumentException();
        float bestUCBVal = -Float.MAX_VALUE;
        int bestMoveIndex = -1;

        //pick move with highest ucbval
        //TODO split into functions
        for (int moveIndex : validMoveTable) {
            Pair<String, Integer> stateActionPair = new Pair<>(s, moveIndex);
            float ucbVal;
            if (this.qsa.containsKey(stateActionPair)) {
                ucbVal = (float) (this.qsa.get(stateActionPair) + EXPLORATIONCONSTANT * this.ps.get(s).get(moveIndex) * Math.log(this.ns.get(s))
                        / (1 + this.nsa.get(stateActionPair)));
            }
            else
//                    ucbVal = (float)(EXPLORATIONCONSTANT * this.ps.get(s).get(moveIndex) * Math.sqrt(this.ns.get(s) + 1e-8));
                ucbVal = Float.MAX_VALUE;
            if (ucbVal > bestUCBVal) {
                bestUCBVal = ucbVal;
                bestMoveIndex = moveIndex;
            }

        }
        Boolean currentIsMrX = this.game.currentIsMrX; //saving who current player is because this.game is updated to next state

        this.game.getNextState(bestMoveIndex); //AT THIS POINT THIS.PERMGAME IS UPDATED to the next state


        //storing current player of next state parent in hashmap
        this.pxs.put(this.game.stringRepresentation(), this.game.currentIsMrX);

        //TODO recursion optimization. Delete game instance for this when we go to next
        Float v = this.search();
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
            this.qsa.put(stateActionPair, v);
            this.nsa.put(stateActionPair, 1);
        }
        this.ns.put(s, this.ns.get(s)+1);
        if (this.pxs.get(s) != currentIsMrX) return -1 * v;
        else return v;
    }

    public boolean hasTimeLeft(long endtime) {
        if (endtime == 0) return true;
        if (endtime - System.nanoTime() > TimeUnit.MILLISECONDS.toNanos(MINMILLISECREMAINING)) return true;
        return false;
    }
}
