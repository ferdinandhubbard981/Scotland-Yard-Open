package uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.List;

/**
 * getChildStates should be a function returning all gamestates that have a move applied to them
 * isGameFinished should return whether or not there are winners
 * getScore should return the value of the state relative to others
 * getMoveLeadingToState should return the move that was applied to the previous gamestate to get
 * to current.
 * @param <T> the type of the move
 * @param <K> the type of the gameState
 */
public interface MinimaxGameState<T, K> {

    boolean isGameFinished();

    int getScore();

    T getMoveLeadingToState();

    K getUnderlyingGameState();

    ImmutableSet<T> getAvailableMoves();

    MinimaxGameState<T, K> useMove(T move);

    boolean isMaximisingPlayer();
}

