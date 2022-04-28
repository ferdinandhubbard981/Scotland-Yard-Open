package uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.List;

public class GameStateWrapper implements MinimaxGameState<Move> {
    private List<? extends MinimaxGameState<Move>> childStates;
    private boolean isGameFinished;
    private int score;
    private Move moveLeadingToState;

    public GameStateWrapper(Board.GameState gameState, int layer, Move move){
        if (layer >= 4) return;
        this.childStates = gameState.getAvailableMoves()
                .stream()
                .map(availableMove -> new GameStateWrapper(gameState, layer+1, availableMove))
                .toList();
        this.isGameFinished = isGameFinishedCalculator(gameState);
        if (layer != 0){
            this.score = 999;
            this.moveLeadingToState = move;
            return;
        }
        this.score = 0;
        this.moveLeadingToState = null;
    }

    private boolean isGameFinishedCalculator(Board.GameState gameState){
        return !gameState.getWinner().isEmpty();
    }

    @Override
    public List<? extends MinimaxGameState<Move>> getChildStates() {
        return this.childStates;
    }

    @Override
    public boolean isGameFinished() {
        return this.isGameFinished;
    }

    @Override
    public int getScore() {
        return this.score;
    }

    @Override
    public Move getMoveLeadingToState() {
        return this.moveLeadingToState;
    }
}
