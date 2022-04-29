package test.minimaxComponentsTests;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents.GraphNode;
import uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents.GraphNodeSet;

import java.util.List;

public class GraphNodeSetTest {
    @Test public void testCanInitialise(){
        GraphNodeSet set = new GraphNodeSet();
        assert set.size() == 0;
    }
    @Test public void testCanAddWithConstructor(){
        GraphNodeSet set = new GraphNodeSet(new GraphNode(1,0));
        assert set.size() == 1;
    }
    @Test public void testCanAddGraphNode(){
        GraphNodeSet set = new GraphNodeSet();
        GraphNode node = new GraphNode(1,0);
        set.add(node);
        assert set.size() == 1;
    }
    @Test public void testCanRemove(){
        GraphNodeSet set = new GraphNodeSet();
        set.add(new GraphNode(1,0));
        set.remove(new GraphNode(1,6));
        assert set.size() == 0;
    }
    @Test()
    public void testExceptionThrownOnEmptySetRemoval() {
        GraphNodeSet set = new GraphNodeSet();
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> set.remove(new GraphNode(1,0)));
        assert e.getMessage().equals("cannot remove from empty set");
    }
    @Test public void testCanAddBetterGraphNode(){
        GraphNodeSet set = new GraphNodeSet();
        set.add(new GraphNode(1,1000));
        set.add(new GraphNode(1, 10));
        assert set.size() == 1;
        assert set.get(1).strictEquality(new GraphNode(1, 10));
    }
    @Test public void testWillIgnoreWorseOptions(){
        GraphNodeSet set = new GraphNodeSet(new GraphNode(1, 1000));
        set.add(new GraphNode(1, 10000000));
        assert set.size() == 1;
        assert set.get(1).strictEquality(new GraphNode(1, 1000));
    }
    @Test public void testContains(){
        GraphNodeSet set = new GraphNodeSet(new GraphNode(1,0));
        assert set.contains(new GraphNode(1,213321));
        assert set.contains(new GraphNode(1,0));
    }
    @Test public void testCanGetNode(){
        GraphNodeSet set = new GraphNodeSet(new GraphNode(1,0));
        GraphNode expected = new GraphNode(1,0);
        assert set.get(1).strictEquality(expected);
    }
    @Test public void testCannotGetInvalidNode(){
        GraphNodeSet set = new GraphNodeSet();
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> set.get(1));
        assert e.getMessage().equals("key=1 could not be got from set");
    }
    @Test public void testGetEmptyGraphNodeSet(){
        GraphNodeSet set = new GraphNodeSet();
        assert set.getNodes().size() == 0;
    }
    @Test public void testGetHasElementsNodeSet(){
        GraphNodeSet set = new GraphNodeSet(
                new GraphNode(1,12321),
                new GraphNode(1, 10),
                new GraphNode(5, 123),
                new GraphNode(2, 9),
                new GraphNode(5, -1),
                new GraphNode(2, 10000)
        );
        List<GraphNode> ordered = set.getNodes();
        assert ordered.size() == 3;
        assert ordered.get(0).strictEquality(new GraphNode(5,-1));
        assert ordered.get(1).strictEquality(new GraphNode(2,9));
        assert ordered.get(2).strictEquality(new GraphNode(1,10));
    }
    @Test public void testEmptyPopWillThrow(){
        GraphNodeSet set = new GraphNodeSet();
        IndexOutOfBoundsException e = Assert.assertThrows(IndexOutOfBoundsException.class, set::pop);
        assert e.getMessage().equals("cannot pop from empty set");
    }
    @Test public void testPopOnSingleElement(){
        GraphNodeSet set = new GraphNodeSet(new GraphNode(1,0));
        GraphNode node = set.pop();
        assert node.strictEquality(new GraphNode(1,0));
        assert set.size() == 0;
    }
    @Test public void testPopOnMultiElement(){
        GraphNodeSet set = new GraphNodeSet(
                new GraphNode(1,100),
                new GraphNode(5,0),
                new GraphNode(1,5),
                new GraphNode(2,3)
        );
        GraphNode node = set.pop();
        assert set.size() == 2;
        assert node.strictEquality(new GraphNode(5,0));
        assert set.pop().strictEquality(new GraphNode(2,3));
        assert set.pop().strictEquality(new GraphNode(1,5));
    }
}
