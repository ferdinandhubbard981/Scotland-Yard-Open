package uk.ac.bris.cs.scotlandyard.ui.ai;


import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

interface ImmutableNodeWithTransport {
    Integer getNode();

    Transport getTransportMethod();
}

class ImmutableNodeTransportPair implements ImmutableNodeWithTransport {
    Integer node;
    Transport transportMethod;

    public ImmutableNodeTransportPair(Integer node, Transport transportMethod) {
        this.node = node;
        this.transportMethod = transportMethod;
    }

    @Override
    public Integer getNode() {
        return this.node;
    }

    @Override
    public Transport getTransportMethod() {
        return this.transportMethod;
    }
}

//use Floyd's Algorithm????????
//use floyd first to get a map of the shortest distances to all nodes from every other node
//but when searching keep state of tickets at each node, and deduct tickets as and when needed
//also ensure that other detectives are not in the same position
public class Dijkstra {

    public static void considerNeighbours(
            int currentNode,
            ImmutableValueGraph<Integer, ImmutableSet<Transport>> graph,
            HashMap<Integer, ImmutableNodeTransportPair> previousNodeMap,
            HashMap<Integer, Boolean> visited,
            HashMap<Integer, Integer> distances) {
        Set<Integer> neighbours = new TreeSet<>(); //order based on distance from
        for (int i = 0; i < graph.outDegree(currentNode); i++){

        }
    }

    public static ImmutableNodeWithTransport getSmallestUnvisitedNode(HashMap<Integer, Boolean> visited, HashMap<Integer, Integer> distances){
        return null;
    }

    public static HashMap<Integer, Integer> useDijkstra(ImmutableValueGraph<Integer, ImmutableSet<Transport>> graph, int start, int end) {
        HashMap<Integer, ImmutableNodeTransportPair> previousNodeMap = new HashMap<>();
        previousNodeMap.put(start, null);

        HashMap<Integer, Integer> distances = new HashMap<>();
        HashMap<Integer, Boolean> visited = new HashMap<>();
        for (int i = 1; i < 200; i++) {
            distances.put(i, Integer.MAX_VALUE);
            visited.put(i, false);
        }
        distances.put(start, 0);
        visited.put(start, true);

        int currentNode = start;
        /**
         * while currentNode exists,
         * if the same, return previousNodeMap
         * else consider neighbours
         * get the closest node by distance
         */
        return null;
    }
}
