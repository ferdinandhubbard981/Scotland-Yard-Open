package uk.ac.bris.cs.scotlandyard.ui.ai;


import com.google.common.collect.*;
import uk.ac.bris.cs.scotlandyard.model.*;
import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.Board;
import java.util.*;
import java.util.stream.Collectors;

public class MCTSNode {
    protected Board.GameState gameState;
    protected MCTSNode parent;
    protected ImmutableList<MCTSNode> children;
    protected int numberOfVisits;
    protected float sumOfStateVals;
    protected float explorationConstant = (float) Math.sqrt(2); //hyper parameter for exploration vs exploitation
    int moveDepth = 3; //numOfMovesToExploreInDijkstra
    protected boolean mrXToMove;
    protected Move incidentMove;
    MCTSNode(Board.GameState gameState, MCTSNode parent, List<MCTSNode> children, boolean mrXMadeMove, Move incidentMove) {
        this.gameState = gameState;
        this.parent = parent;
        this.children = ImmutableList.copyOf(children);
        this.numberOfVisits = 0;
        this.sumOfStateVals = 0; //set to -2 means it hasn't been calculated yet
        this.mrXToMove = !mrXMadeMove;
        this.incidentMove = incidentMove;
    }

    Move getIncidentMove() {
        return this.incidentMove;
    }
    int getNumberOfVisits() {
        return this.numberOfVisits;
    }

    int getNumOfParents() {
        if (this.parent != null) return 1 + this.parent.getNumOfParents();
        else return 0;
    }

    float getUCB1Val() {
        if (this.parent == null) return Float.MIN_VALUE;
        if (this.numberOfVisits == 0) {
//            int numOfParents = this.getNumOfParents();
            return (float) Math.pow(2, 10);// - numOfParents;
        }
        float exploitation = this.sumOfStateVals /this.numberOfVisits;
        float exploration = this.explorationConstant * (float) Math.sqrt(Math.log(this.parent.getNumberOfVisits()) / this.numberOfVisits);
        return exploitation + exploration;
    }

    float getSumOfStateVals() {
        return this.sumOfStateVals;
    }

    MCTSNode getNodeWithHighestUCBVal(MCTSNode highestValNode, boolean recursive) {
        //check if no parent node and no children at the same time?
        if (this.children.isEmpty() || !recursive) {
            if (this.getUCB1Val() > highestValNode.getUCB1Val()) return this;
            else return highestValNode;
        }
        highestValNode = this.children.stream().findAny().get();
        for (MCTSNode child : this.children) {
            highestValNode = child.getNodeWithHighestUCBVal(highestValNode, false);
        }
        highestValNode = highestValNode.getNodeWithHighestUCBVal(highestValNode, true);
        return highestValNode;
    }

    ImmutableList<MCTSNode> findChildren() {
        List<MCTSNode> children = new ArrayList<>();
        ImmutableSet<Move> moves = this.gameState.getAvailableMoves();
        for (Move move : moves) {
            Board.GameState newGameState = this.gameState.advance(move);
            MCTSNode newChild = new MCTSNode(newGameState, this, ImmutableList.of(), move.commencedBy().isMrX(), move);
            children.add(newChild);
        }
        return ImmutableList.copyOf(children);
    }

    MCTSNode getParent() {
        return this.parent;
    }

    Board.GameState getState() {
        return this.gameState;
    }

    ImmutableList<MCTSNode> getChildren() {
        return this.children;
    }
    void setChildren(ImmutableList<MCTSNode> children) {
        this.children = children;
    }

    boolean isLeafNode() {
        if (this.children.isEmpty()) return true;
        else return false;
    }

    //play a random game to get a value for monteCarloVal
    void rollout() {
        Board.GameState current = this.gameState;
        while (current.getWinner().isEmpty()) {
            //get moves
            Set<Move> moves = current.getAvailableMoves();
            //choose random move
            Move move = moves.stream().toList().get(new Random().nextInt(moves.size()));
            //play move
            current = current.advance(move); //don't know how this works when we don't even know where mrX is
        }
        // we want to maximise whoever's move it is to play in the parent node
        if (this.parent == null) {
            this.backPropogate(0);
            return;
        };
        int gameVal = (current.getWinner().stream().findAny().get().isMrX() == this.parent.mrXToMove) ? 1 : 0; //might be the cause of some problems
        this.backPropogate(gameVal);
    }

    void stateRollout() {
        //get average distance between mrX and detectives
        float averageDistance = 0;
        Piece mrX = this.gameState.getPlayers().stream().filter(piece -> piece.isMrX()).findFirst().get();
        Set<Piece> detectives = this.gameState.getPlayers().stream().filter(piece -> piece.isDetective()).collect(Collectors.toSet());
        for (Piece detective : detectives) {
            Set<Move> moveSet = Dijkstra.getShortestMoveSet(detective, mrX, this.gameState, this.moveDepth);
            int moveLen = (moveSet.size() == 0) ? this.moveDepth: moveSet.size();
            averageDistance += moveSet.size();
        }
        averageDistance /= detectives.size();
        averageDistance /= this.moveDepth;
        float gameVal = (this.mrXToMove) ? averageDistance : 1 - averageDistance; //might be the cause of some problems
        backPropogate(gameVal);
    }

    void backPropogate(float gameVal) {
        this.sumOfStateVals += gameVal;
        this.numberOfVisits++;
        if (this.parent == null){
            return;
        }
        if (this.parent.parent == null) {
            this.backPropogate(0);
        }
        else if (this.parent.parent.mrXToMove != this.parent.mrXToMove) gameVal = 1 - gameVal;
        this.parent.backPropogate(gameVal);
    }

    void backPropogate(int gameVal) {
        this.sumOfStateVals += gameVal;
        this.numberOfVisits++;
        if (this.parent == null){
            return;
        }
        if (this.parent.parent == null) {
            this.parent.backPropogate(0);
        }
        else if (this.parent.parent.mrXToMove != this.parent.mrXToMove) gameVal = 1 - gameVal;
        this.parent.backPropogate(gameVal);
    }

    boolean isTerminalState() {
        return !this.gameState.getWinner().isEmpty();
    }

}
