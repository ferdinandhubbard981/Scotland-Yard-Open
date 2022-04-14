package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.LogEntry;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.Random;

interface PathFinder{
    /**
     * returns the next best move for the current player
     * @param board
     * @param isMrXPlaying
     * @return best move
     */
    Move nextMove(Board board, boolean isMrXPlaying);
}

public class PathCalculator implements PathFinder{
    @Override
    public Move nextMove(Board board, boolean isMrXPlaying) {
        return defaultMove(board);
    }

    private Move defaultMove(Board board) {
        var moves = board.getAvailableMoves().asList();
        return moves.get(new Random().nextInt(moves.size()));
    }

    private int getMrXLocation(Board board, boolean isMrXPlaying){
        if (isMrXPlaying) return board.getAvailableMoves().asList().get(0).source();

        for (LogEntry entry : board.getMrXTravelLog().reverse()){
            int location = entry.location().orElse(-1);
            if (location != -1) return location;
        }
        return new Random().nextInt(1,200); //returns random location because we don't know where he is at
    }
}
