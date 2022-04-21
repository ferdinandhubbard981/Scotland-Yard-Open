package uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents;

import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.List;

interface MinimaxGameState {
    List<MinimaxGameState> getChildStates();
    boolean isGameFinished();
    int getScore();
    Move getMoveLeadingToState();
}

class Node {
    int score;
    boolean isMaximisingPlayer;
    MinimaxGameState gameState;
    List<Node> childNodes;
    int depth;

    public Node(MinimaxGameState gameState, boolean isMaximisingPlayer, int depth) {
        this.gameState = gameState;
        this.isMaximisingPlayer = isMaximisingPlayer;
        this.depth = depth;
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
    public void addChildNode(Node childNode){
        this.childNodes.add(childNode);
    }
}

public class MinimaxGame {
    Node root;
    public void buildTree(MinimaxGameState gameState, boolean isMrXPerspective) {
        root = new Node(gameState, isMrXPerspective, 0);
        buildTree(root);
    }
    private void buildTree(Node parent){
        List<MinimaxGameState> possibleGameStates = parent.gameState.getChildStates();
        boolean isChildMaximising = !parent.isMaximisingPlayer();
        if (parent.gameState.isGameFinished()) return;
        possibleGameStates.forEach(gamestate -> {
            Node childNode = new Node(gamestate, isChildMaximising, parent.depth + 1);
            parent.addChildNode(childNode);
            if (!parent.gameState.isGameFinished()){
                buildTree(childNode);
            }
        });
    }

    public Move getBestMove(){
        return getBestMove(root);
    }
    private Move getBestMove(Node node){
        //if no children, then return the current move
        if (node.getChildNodes().size() == 0) return node.gameState.getMoveLeadingToState();
        //if children, then return the best move of all the children
        for (Node childNode : node.getChildNodes()){
            if (childNode.getChildNodes().size() > 0){
                return getBestMove(childNode);
            }
        }
        return node.getChildNodes().stream().max(); //maximise the getScore on each childNode


    }
}
