package test.minimaxComponentsTests;

import org.junit.jupiter.api.Test;
import uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents.MinimaxGame;
import uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents.MinimaxGameState;
import uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents.Node;

import java.util.List;

public class TreeBuilderTest {
    static class DummyMove {
        private final int moveNumber;
        public DummyMove(int dummyMoveNumber) {this.moveNumber = dummyMoveNumber;}

        public int getMoveNumber() {
            return moveNumber;
        }
    }
    static class DummyMinimaxGameState implements MinimaxGameState<DummyMove> {
        private final List<MinimaxGameState<DummyMove>> childStates;
        private final boolean isGameFinished;
        private final int score;
        private final DummyMove moveLeadingToState;
        @Override
        public List<MinimaxGameState<DummyMove>> getChildStates() {
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
        public DummyMove getMoveLeadingToState() {
            return this.moveLeadingToState;
        }

        public DummyMinimaxGameState(boolean isGameFinished, int score, DummyMove moveLeadingToState, List<MinimaxGameState<DummyMove>> children){
            this.childStates = children;
            this.isGameFinished = isGameFinished;
            this.moveLeadingToState = moveLeadingToState;
            this.score = score;
        }
    }

    @Test
    public void testDummyGameStateCanBuild(){
        MinimaxGame<DummyMove> game = new MinimaxGame<>();
        game.buildTree(new DummyMinimaxGameState(true, 10, null, null), true);
        assert game.getBestMove() == null;
        Node<DummyMove> best = game.getBestNode();
        assert best.isMaximisingPlayer();
        assert best.getChildNodes() == null;
        assert best.getParentNode() == null;
    }

    @Test
    public void testGameStateWithChildrenCanBuild(){
        MinimaxGame<DummyMove> game = new MinimaxGame<>();
        DummyMinimaxGameState parent = new DummyMinimaxGameState(
                false,
                15,
                null,
                List.of(new DummyMinimaxGameState(
                        false, 20, new DummyMove(93132), null)
                ));
        game.buildTree(parent, true);
        Node<DummyMove> best = game.getBestNode();
        assert best.getParentNode().getScore() == 15;
        assert best.getParentNode().isMaximisingPlayer();
        assert best.getParentNode().getParentNode() == null;
        assert !best.isMaximisingPlayer();
        assert best.getChildNodes() == null;
        assert best.getScore() == 20;
        assert game.getBestMove().getMoveNumber() == 93132;
    }

    @Test
    public void testGameStateReturnsBestNodeTwoLayer(){
        MinimaxGame<DummyMove> game = new MinimaxGame<>();
        DummyMinimaxGameState root = new DummyMinimaxGameState(
                false,
                30,
                null,
                List.of(
                        new DummyMinimaxGameState(false, -5, new DummyMove(1), List.of()),
                        new DummyMinimaxGameState(false, 10, new DummyMove(2), List.of()),
                        new DummyMinimaxGameState(true, 5, new DummyMove(3), List.of())
                )
        );
        game.buildTree(root, true);
        Node<DummyMove> bestNode = game.getBestNode();
        assert !bestNode.isMaximisingPlayer();
        assert bestNode.getScore() == 10;
        assert !bestNode.getGameState().isGameFinished();
        assert game.getBestMove().getMoveNumber() == 2;
    }

    @Test
    public void testGameStateOnUnevenTree(){
        MinimaxGame<DummyMove> game = new MinimaxGame<>();
        DummyMinimaxGameState root = new DummyMinimaxGameState(
                false,
                -12313,
                null,
                List.of(
                        new DummyMinimaxGameState(true, -10000000, new DummyMove(1), null),
                        new DummyMinimaxGameState(false, 10, new DummyMove(2), null),
                        new DummyMinimaxGameState(false, -125, new DummyMove(3),
                                List.of(new DummyMinimaxGameState(false, 70000, new DummyMove(4), null)))
                ));
        game.buildTree(root, false);
        Node<DummyMove> best = game.getBestNode();
        assert best.isMaximisingPlayer();
        assert best.getScore() == 70000;
        assert !best.getGameState().isGameFinished();
        assert best.getGameState().getMoveLeadingToState().getMoveNumber() == 4;
        assert game.getBestMove().getMoveNumber() == 3;
    }


}
