package test.aiTests;

import uk.ac.bris.cs.scotlandyard.ui.ai.MCTS;

import java.util.List;
import java.util.Objects;

public class MCTSTest {
    public testMethods<Float> testerMethods = new testMethods<>();

    public void testGetActionProb1() {
        MCTS testObj = new MCTS(null, null);
        assert testerMethods.hasSameValues(testObj.getActionProb(null, 0), List.of());
    }

    public void testGetActionProb2() {
        MCTS testObj = new MCTS(null, null);
        assert testerMethods.hasSameValues(testObj.getActionProb(null, 0), List.of());
    }

    public void testGetActionProb3() {
        MCTS testObj = new MCTS(null, null);
        assert testerMethods.hasSameValues(testObj.getActionProb(null, 0), List.of());
    }

    public void testGetActionProb4() {
        MCTS testObj = new MCTS(null, null);
        assert testerMethods.hasSameValues(testObj.getActionProb(null, 0), List.of());
    }
}
