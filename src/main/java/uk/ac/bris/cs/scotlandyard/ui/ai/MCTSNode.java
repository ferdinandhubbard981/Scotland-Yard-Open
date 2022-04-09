package uk.ac.bris.cs.scotlandyard.ui.ai;


import com.esotericsoftware.kryo.util.Null;
import com.google.common.collect.*;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.*;
import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.Board;

import java.util.*;

public class MCTSNode {
    protected Board.GameState gameState;
    protected MCTSNode parent;
    protected ImmutableList<MCTSNode> children;
    protected int numberOfVisits;
    protected float monteCarloVal; //[-1, 1]
    protected float explorationConstant = 1000000000; //hyper parameter for exploration vs exploitation
    protected boolean mrXToMove;
    protected Move incidentMove;
    MCTSNode(Board.GameState gameState, MCTSNode parent, List<MCTSNode> children, boolean mrXMadeMove, Move incidentMove) {
        this.gameState = gameState;
        this.parent = parent;
        this.children = ImmutableList.copyOf(children);
        this.numberOfVisits = 0;
        this.monteCarloVal = -2; //set to -2 means it hasn't been calculated yet
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
        if (this.monteCarloVal == -2 || this.numberOfVisits == 0) {
            int numOfParents = this.getNumOfParents();
            return (float) Math.pow(2, 10) - numOfParents;
        }
        return this.monteCarloVal + this.explorationConstant * (float) Math.sqrt(Math.log(this.parent.getNumberOfVisits()) / this.numberOfVisits);
    }

    float getMonteCarloVal() {
        return this.monteCarloVal;
    }

    MCTSNode getNodeWithHighestUCBVal(MCTSNode highestValNode) {
        //check if no parent node and no children at the same time?
        if (this.children.isEmpty()) {
            if (this.getUCB1Val() > highestValNode.getUCB1Val()) return this;
            else return highestValNode;
        }

        for (MCTSNode child : this.children) {
            highestValNode = child.getNodeWithHighestUCBVal(highestValNode);
        }
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
            //play move
            Set<Move> moves = current.getAvailableMoves();
            //choose random move
            Move move = moves.stream().toList().get(new Random().nextInt(moves.size()));
            //play move
            current = current.advance(move);
        }
        int gameVal = (current.getWinner().stream().findAny().get().isMrX() == this.mrXToMove) ? 1 : -1;
        this.backPropogate(gameVal);
    }

    void backPropogate(int gameVal) {
        this.monteCarloVal = (this.monteCarloVal * this.numberOfVisits + gameVal) / ++this.numberOfVisits;
        if (this.parent != null) {
            this.parent.backPropogate(gameVal);
        }
    }

    boolean isTerminalState() {
        return !this.gameState.getWinner().isEmpty();
    }

}
