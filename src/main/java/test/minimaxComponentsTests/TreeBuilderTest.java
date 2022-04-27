package test.minimaxComponentsTests;

import org.junit.jupiter.api.Test;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents.MinimaxGame;
import uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents.MinimaxGameState;
import uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents.Node;

import java.util.List;

public class TreeBuilderTest {
    static class DummyMinimaxGameState implements MinimaxGameState {
        private final List<MinimaxGameState> childStates;
        private final boolean isGameFinished;
        private final int score;
        private final Move moveLeadingToState;
        @Override
        public List<MinimaxGameState> getChildStates() {
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

        public DummyMinimaxGameState(boolean isGameFinished, int score, Move moveLeadingToState, MinimaxGameState ...children){
            this.childStates = List.of(children);
            this.isGameFinished = isGameFinished;
            this.moveLeadingToState = moveLeadingToState;
            this.score = score;
        }
    }

    @Test
    public void testDummyGameStateCanBuild(){
        MinimaxGame game = new MinimaxGame();
        game.buildTree(new DummyMinimaxGameState(true, 10, null, null), true);
        assert game.getBestMove() == null;
        Node best = game.getBestNode();
        assert best.isMaximisingPlayer();
        assert best.getChildNodes() == null;
        assert best.getParentNode() == null;

    }

    @Test
    public void testGameStateChildrenCanBuild(){
        MinimaxGame game = new MinimaxGame();

    }


}
