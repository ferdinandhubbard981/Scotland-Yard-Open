package uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents;

import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.Comparator;
import java.util.List;

public class MinimaxGame {
    Node root;
    public void buildTree(MinimaxGameState gameState, boolean isMrXPerspective) {
        root = new Node(gameState, isMrXPerspective, 0, null);
        buildTree(root);
    }
    private void buildTree(Node parent){
        List<MinimaxGameState> possibleGameStates = parent.gameState.getChildStates();
        boolean isChildMaximising = !parent.isMaximisingPlayer();
        if (parent.gameState.isGameFinished()) return;
        possibleGameStates.forEach(gamestate -> {
            Node childNode = new Node(gamestate, isChildMaximising, parent.depth + 1, parent);
            parent.addChildNode(childNode);
            if (childNode.depth < 4){
                buildTree(childNode);
            }
        });
    }

    public Node getBestNode(){
        return getBestNode(root);
    }
    private Node getBestNode(Node node){
        //if no children, then return the current move
        if (node.getChildNodes().size() == 0) return node;
        //if children, then return the best move of all the children
        return node.getChildNodes().stream()
                .map(this::getBestNode)
                .max(node.isMaximisingPlayer() ?
                        Comparator.comparing(Node::getScore) :
                        Comparator.comparing(Node::getScore).reversed())
                .orElseThrow(); //optimise getScore on each childNode
    }
    public Move getBestMove(){
        Node bestNode = getBestNode();
        return traceMove(bestNode);
    }
    private Move traceMove(Node childNode){
        if (childNode.getParentNode() == null) return childNode.gameState.getMoveLeadingToState();
        return traceMove(childNode.getParentNode());
    }
}
