package uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents;

import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.Comparator;
import java.util.List;

/**
 *
 * @param <T> replace with Move on real implementation
 */
public class MinimaxGame<T> {
    Node<T> root;
    public void buildTree(MinimaxGameState<T> gameState, boolean isMrXPerspective) {
        root = new Node<>(gameState, isMrXPerspective, 0, null);
        buildTree(root);
    }
    private void buildTree(Node<T> parent){
        List<? extends MinimaxGameState<T>> possibleGameStates = parent.gameState.getChildStates();
        boolean isChildMaximising = !parent.isMaximisingPlayer();
        if (parent.gameState.isGameFinished() || possibleGameStates == null || possibleGameStates.size() == 0) return;
        possibleGameStates.forEach(gamestate -> {
            Node<T> childNode = new Node<>(gamestate, isChildMaximising, parent.depth + 1, parent);
            parent.addChildNode(childNode);
            if (childNode.depth < 4){
                buildTree(childNode);
            }
        });
    }

    public Node<T> getBestNode(){
        return getBestNode(root);
    }
    private Node<T> getBestNode(Node<T> node){
        //if no children, then return the current move
        if (node.getChildNodes().size() == 0) return node;
        //if children, then return the best move of all the children
        return node.getChildNodes().stream()
                .map(this::getBestNode)
                .max(node.isMaximisingPlayer() ?
                        Comparator.comparing(Node<T>::getScore) :
                        Comparator.comparing(Node<T>::getScore).reversed())
                .orElseThrow(); //optimise getScore on each childNode
    }
    public T getBestMove(){
        Node<T> bestNode = getBestNode();
        return traceMove(bestNode);
    }
    //returns the move that will lead to subtree containing game state
    private T traceMove(Node<T> childNode){
        if (childNode.getParentNode() == null)
            return childNode.gameState.getMoveLeadingToState();
        return traceMove(childNode.getParentNode());
    }
}
