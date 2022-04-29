package test.minimaxComponentsTests;

import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import org.junit.jupiter.api.Test;
import uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents.PathFinder;


public class PathFinderTest {
    @Test public void testCanFindNode9(){
        MutableValueGraph<Integer, Integer> graph = ValueGraphBuilder.undirected().build();
        for (int i = 0; i < 10; i++) {
            graph.addNode(i);
        }
        graph.putEdgeValue(1,2, 5);
        graph.putEdgeValue(1,3, 6);
        graph.putEdgeValue(2,4, 3);
        graph.putEdgeValue(2,5, 1);
        graph.putEdgeValue(3,6, 3);
        graph.putEdgeValue(3,7, 10);
        graph.putEdgeValue(3,8, 7);
        graph.putEdgeValue(8,9, 2);
        graph.putEdgeValue(1,10, 20);

        PathFinder<Integer> finder = new PathFinder<>(ImmutableValueGraph.copyOf(graph), 1, 9);
        finder.search();
        assert finder.getDistance() == 3;
    }

    @Test public void testCanFindNode6(){
        MutableValueGraph<Integer, Integer> graph = ValueGraphBuilder.undirected().build();
        for (int i = 0; i < 6; i++) graph.addNode(i);
        graph.putEdgeValue(1,2,4);
        graph.putEdgeValue(1,3,10);
        graph.putEdgeValue(2,4, 5);
        graph.putEdgeValue(4, 5, 9);
        graph.putEdgeValue(5,6, 11);
        PathFinder<Integer> finder = new PathFinder<>(ImmutableValueGraph.copyOf(graph), 1, 6);
        finder.search();
        assert finder.getDistance() == 4;
    }
}
