package uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents;

import java.util.Comparator;

/**
 *
 * @param <T> type of the Move
 * @param <S> type of the gameState
 */
public class MinimaxGame<T, S> {
    Node<T, S> root;
    public void buildTree(MinimaxGameState<T, S> gameState, boolean isMaximisingPlayer) {
        root = new Node<>(gameState, isMaximisingPlayer, 0, null);
        buildTree(root);
    }
    private void buildTree(Node<T, S> parent){
        boolean isChildMaximising = !parent.isMaximisingPlayer();
        if (parent.gameState.isGameFinished()) return;
        parent.getGameState().getAvailableMoves().forEach(move -> {
            Node<T, S> childNode = new Node<>(
                    parent.getGameState().useMove(move),
                    isChildMaximising,
                    parent.depth + 1,
                    parent
            );
            parent.addChildNode(childNode);
            if (childNode.depth < 4) buildTree(childNode);
        });
    }

    public Node<T, S> getBestNode(){
        return getBestNode(root);
    }
    private Node<T, S> getBestNode(Node<T, S> node){
        //if no children, then return the current move
        if (node.getChildNodes().size() == 0) return node;
        //if children, then return the best move of all the children
        return node.getChildNodes().stream()
                .map(this::getBestNode)
                .max(node.isMaximisingPlayer() ?
                        Comparator.comparing(Node<T,S>::getScore) :
                        Comparator.comparing(Node<T,S>::getScore).reversed())
                .orElseThrow(); //optimise getScore on each childNode
    }
    public T getBestMove(){
        Node<T, S> bestNode = getBestNode();
        return traceMove(bestNode);
    }
    //returns the move that will lead to subtree containing game state
    private T traceMove(Node<T, S> childNode){
        if (childNode.getParentNode() == null)
            return childNode.gameState.getMoveLeadingToState();
        return traceMove(childNode.getParentNode());
    }
}
