package uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents;

import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.List;

/**
 * getChildStates should be a function returning all gamestates that have a move applied to them
 * isGameFinished should return whether or not there are winners
 * getScore should return the value of the state relative to others
 * getMoveLeadingToState should return the move that was applied to the previous gamestate to get
 * to current.
 * @implNote use Move class as parameter to interface
 */
public interface MinimaxGameState<T> {

    boolean isGameFinished();

    int getScore();

    T getMoveLeadingToState();
}
