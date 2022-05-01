package uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents;

import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @param <T> the type of the move
 * @param <K> the type of the gameState
 */
public class Node<T, K> {
    int score;
    boolean isMaximisingPlayer;
    MinimaxGameState<T, K> gameState; //underlying gameState for current node
    List<Node<T, K>> childNodes;
    Node<T, K> parentNode;
    int depth;

    public Node(MinimaxGameState<T, K> gameState, boolean isMaximisingPlayer, int depth, Node<T, K> parentNode) {
        this.gameState = gameState;
        this.isMaximisingPlayer = isMaximisingPlayer;
        this.depth = depth;
        this.parentNode = parentNode;
        this.childNodes = new ArrayList<>();
    }

    public boolean isMaximisingPlayer() {
        return isMaximisingPlayer;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<Node<T, K>> getChildNodes() {
        return childNodes;
    }

    public void addChildNode(Node<T, K> childNode) {
        this.childNodes.add(childNode);
    }

    public Node<T, K> getParentNode() {
        return parentNode;
    }

    public MinimaxGameState<T, K> getGameState() {
        return gameState;
    }
}
