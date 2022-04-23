package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;

import java.util.List;

public class TrainingEntry {
    private Board.GameState gameState;
    private List<Float> policyValues;
    private Integer gameOutcome; //1 for mrX won -1 for det won

    public TrainingEntry(Board.GameState gameState, List<Float> policyValues, int gameOutcome) {
        this.gameState = gameState;
        this.policyValues = policyValues;
        this.gameOutcome = gameOutcome;
    }

    public Board.GameState getGameState() {
        return this.gameState;
    }

    public List<Float> getPolicyValues() {
        return this.policyValues;
    }

    public Integer getGameOutcome() {
        return this.gameOutcome;
    }

    public void setGameOutcome(Integer gameOutcome) {
        this.gameOutcome = gameOutcome;
    }
}
