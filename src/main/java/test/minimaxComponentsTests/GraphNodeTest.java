package test.minimaxComponentsTests;

import org.junit.jupiter.api.Test;
import uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents.GraphNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

public class GraphNodeTest {
    @Test public void testEqualWhenBothEqual(){
        GraphNode node1 = new GraphNode(123,0);
        GraphNode node2 = new GraphNode(123,0);
        assert node1.equals(node2);
    }

    @Test public void testEqualWhenOnlyKeysEqual(){
        GraphNode node1 = new GraphNode(123,-1);
        GraphNode node2 = new GraphNode(123,21212);
        assert node1.equals(node2);
    }

    @Test public void testNotEqualWhenOnlyValuesEqual(){
        GraphNode node1 = new GraphNode(1312321,21);
        GraphNode node2 = new GraphNode(121,21);
        assert !node1.equals(node2);
    }

    @Test public void testNotEqualWhenNeitherEqual(){
        GraphNode node1 = new GraphNode(1312321,21);
        GraphNode node2 = new GraphNode(121,2113);
        assert !node1.equals(node2);
    }
}
