package uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents;

import java.util.List;

public class Node {
    int score;
    boolean isMaximisingPlayer;
    MinimaxGameState gameState;
    List<Node> childNodes;
    Node parentNode;
    int depth;

    public Node(MinimaxGameState gameState, boolean isMaximisingPlayer, int depth, Node parentNode) {
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

    public List<Node> getChildNodes() {
        return childNodes;
    }

    public void addChildNode(Node childNode) {
        this.childNodes.add(childNode);
    }

    public Node getParentNode() {
        return parentNode;
    }

    public void setParentNode(Node parentNode) {
        this.parentNode = parentNode;
    }
}
