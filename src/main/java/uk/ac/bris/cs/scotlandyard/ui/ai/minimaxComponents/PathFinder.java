package uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents;

import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents.GraphNode;

import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListSet;

interface NaivePathFinder {
    //naive pathfinder, not including tickets
    void search();
    int getDistance();
}

//T is for the edge values of the graph
public class PathFinder<T> implements NaivePathFinder{
    private ImmutableValueGraph<Integer, T> graph;
    private HashMap<Integer, Integer> previous = new HashMap<>(); //backtracking
    private GraphNodeSet queue = new GraphNodeSet();//will be of form (node : distance), unvisited set
    private GraphNodeSet visited = new GraphNodeSet();
    private int destination;
    private int start;

    public PathFinder(ImmutableValueGraph<Integer, T> graph, int start, int dest){
        this.graph = graph;
        for (int i = 1; i < graph.nodes().size(); i++){
            queue.add(new GraphNode(i, Integer.MAX_VALUE));
            previous.put(i, Integer.MAX_VALUE);
        }
        previous.put(start, null);
        queue.add(new GraphNode(start, 0));
        this.start = start;
        this.destination = dest;
    }

    @Override
    public void search() {
        while(queue.size() > 0) {
            GraphNode closest = queue.pop();
            visited.add(closest);
            if (closest.getKey() == this.destination) return;
            int newDistance = closest.getValue() + 1;
            for (int node : this.graph.adjacentNodes(closest.getKey()).stream().filter(node -> queue.contains(node)).toList()) {
                GraphNode currentNeighbour = queue.get(node);
                if (newDistance < currentNeighbour.getValue()) {
                    queue.set(node, newDistance);
                    this.previous.put(node, closest.getKey());
                }
            }
        }
    }

    @Override
    public int getDistance() {
        return visited.get(destination).getValue();
    }
}
