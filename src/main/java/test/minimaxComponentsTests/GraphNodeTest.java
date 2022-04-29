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

    @Test public void testNoDuplicatesInSet(){
        GraphNode node1 = new GraphNode(131231, 1);
        GraphNode node2 = new GraphNode(131231, 132321);
        Set<GraphNode> set = new HashSet<>();
        set.add(node1);
        set.add(node2);

        assert set.size() == 1;
    }

    @Test public void testCanHaveSameDistanceInSet(){
        GraphNode node1 = new GraphNode(1, 1000);
        GraphNode node2 = new GraphNode(2, 1000);
        Set<GraphNode> set = new HashSet<>();
        set.add(node1);
        set.add(node2);
        assert set.size() == 2;
    }

    @Test public void testNodesOrderedByValue(){
        GraphNode node1 = new GraphNode(1, 4);
        GraphNode node2 = new GraphNode(2, 3);
        Set<GraphNode> set = new TreeSet<>();
        set.add(node1);
        set.add(node2);
        ArrayList<GraphNode> list = new ArrayList<>(set);
        assert list.get(0).equals(node2); //this is a fluke
        assert list.get(1).equals(node1);
    }

    @Test public void test(){
        GraphNode node1 = new GraphNode(1, 4);
        GraphNode node2 = new GraphNode(2, 3);
        GraphNode node3 = new GraphNode(2, 1000);
        Set<GraphNode> set = new ConcurrentSkipListSet<>(); //scalably work
        set.add(node1);
        set.add(node2);
        set.add(node3);
        assert set.size() == 2;
        ArrayList<GraphNode> list = new ArrayList<>(set);
        assert list.get(0).equals(node2);
        assert list.get(1).equals(node1);
    }
}
