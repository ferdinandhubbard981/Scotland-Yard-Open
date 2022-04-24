package uk.ac.bris.cs.scotlandyard.ui.ai.old;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.Player;;


import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class Dijkstra {
    static int mrXUknownVal = 10; //mrX position when he hasn't yet appeared on the map
//    @param piece1
//    @param piece2
//    @param gameState
//    @param moveDepth
//    @param return set of moves to go from piece1 to piece 2 (if none found then set of random moves)

    static int getPlayerLocation(Board.GameState gameState, Piece piece, Set<Move> availableMoves) {
        if (piece.isMrX()) {
            if (availableMoves.stream().findAny().get().commencedBy().isMrX())
                return availableMoves.stream().findAny().get().source();

            int lastIndex = gameState.getMrXTravelLog().asList().size()-1;
            if (lastIndex == -1) return mrXUknownVal;
            Optional<Integer> mrXLocation = gameState.getMrXTravelLog().get(lastIndex).location();
            if (mrXLocation.isEmpty()) return mrXUknownVal;
            return mrXLocation.get();
        }
        else {
            return gameState.getDetectiveLocation((Piece.Detective) piece).get();
        }
    }
    public static Set<Move> getShortestMoveSet(Piece piece1, Piece piece2, Board.GameState gameState, int moveDepth) {
        //when we reach maxMoveDepth we return empty set showing we have not found shortestMoveSet
        if (moveDepth == 0) return Set.of();
        else {
            Set<Move> availableMoves = gameState.getAvailableMoves();
            for (Move move : availableMoves) {
                //get gameState after move
                Board.GameState newGameState = gameState.advance(move);
                //if player2 found return move
                if (getPlayerLocation(gameState, piece1, availableMoves) == getPlayerLocation(gameState, piece2, availableMoves)) return Set.of(move);
                //recursively call this if we haven't found the player2
                Set<Move> moveSubset = getShortestMoveSet(piece1, piece2, newGameState, moveDepth-1);
                //if we found player2 in the line above we prepend the current move to the output and return
                if (!moveSubset.isEmpty()) {
                    Set<Move> moveSet = new HashSet<>();
                    moveSet.add(move);
                    moveSet.addAll(moveSubset);
                    return moveSet;
                }
            }
            return Set.of();
        }
    }
}
