package uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents;

import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.List;

/**
 *
 * @param <T> use Move with real implementation
 */
public class Node<T> {
    int score;
    boolean isMaximisingPlayer;
    MinimaxGameState<T> gameState;
    List<Node<T>> childNodes;
    Node<T> parentNode;
    int depth;

    public Node(MinimaxGameState<T> gameState, boolean isMaximisingPlayer, int depth, Node<T> parentNode) {
        this.gameState = gameState;
        this.isMaximisingPlayer = isMaximisingPlayer;
        this.depth = depth;
        this.parentNode = parentNode;
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

    public List<Node<T>> getChildNodes() {
        return childNodes;
    }

    public void addChildNode(Node<T> childNode) {
        this.childNodes.add(childNode);
    }

    public Node<T> getParentNode() {
        return parentNode;
    }

    /*public void setParentNode(Node<T> parentNode) {
        this.parentNode = parentNode;
    }*/

    public MinimaxGameState<T> getGameState() {
        return gameState;
    }
}
