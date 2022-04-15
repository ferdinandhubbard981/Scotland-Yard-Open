package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.ImmutableValueGraph;

import java.util.*;

interface DijkstraAlgorithm {
    void search();
    void backtrack(HashMap<Integer, Integer> backtrackMap);

    HashMap<Integer, Integer> getBacktrackMap();
    ImmutableList<Integer> getTrail();
}

public class Dijkstra<T> implements DijkstraAlgorithm {

    interface GraphNode<M> {
        M getNodeValue();
        M getDistance();
    }

    static class Node implements GraphNode<Integer>, Comparable<Node> {
        private final int node;
        private final int distance;
        public Node(int node, int distance){
            this.node = node;
            this.distance = distance;
        }

        @Override
        public Integer getNodeValue() {
            return this.node;
        }

        @Override
        public Integer getDistance() {
            return this.distance;
        }

        @Override
        public int compareTo(Node o) {
            if (this.equals(o)) return 0; //for removing duplicates
            return this.getDistance() < o.getDistance() ? -1 : 1; //sorts based on distances
        }

        @Override
        public int hashCode(){
            return this.node;
        }

        @Override
        public boolean equals(Object o){
            return (o instanceof Node) && ((Node)o).node == this.node;
        }
    }

    ImmutableValueGraph<Integer, T> graph;
    int start, end;
    HashMap<Integer, Integer> backtrackMap = new HashMap<>();
    List<Integer> trail = new ArrayList<>();
    HashMap<Integer, Boolean> unvisited;
    HashMap<Integer, Integer> distances = new HashMap<>();

    Node currentNode;
    TreeSet<Node> neighbours;

    public Dijkstra(ImmutableValueGraph<Integer, T> graph, int start, int end, int totalNodes){
        this.graph = graph;
        this.start = start;
        this.end = end;
        //creating unvisited set and initialising tentative distances
        for (int i = 1; i < totalNodes; i++){
            this.unvisited.put(i, true);
            this.distances.put(i, Integer.MAX_VALUE);
        }
        this.distances.put(start, 0);
        //adding start node as entry point to algorithm
        this.neighbours = new TreeSet<>();
        this.neighbours.add(new Node(start, 0));
        this.backtrackMap.put(start, null);
    }

    @Override
    public void search() {
        while(!this.neighbours.isEmpty()){
            this.currentNode = this.neighbours.first();
            if (this.currentNode.getNodeValue() == this.end) return;
            //not found
            this.neighbours.remove(this.currentNode);

            //for each unvisited neighbour, if unvisited and new distance is less than previous, add to neighbour list
            ////check that unvisited neighbours that have been improved are not duplicated
            int dist = this.currentNode.getDistance() + 1;
            for (int adjacentNode : this.graph.adjacentNodes(this.currentNode.getNodeValue())){
                if (!unvisited.get(adjacentNode) || dist > distances.get(adjacentNode)) continue;
                distances.put(adjacentNode, dist);
                backtrackMap.put(adjacentNode, currentNode.getNodeValue());
                Node dummy = new Node(adjacentNode, -1);
                if (neighbours.contains(dummy)) {
                    //for adding an improved route to node
                    neighbours.remove(dummy);
                }
                neighbours.add(new Node(adjacentNode, dist));
            }
            this.unvisited.put(this.currentNode.getNodeValue(), false);
        }

    }

    @Override
    public void backtrack(HashMap<Integer, Integer> backtrackMap) {
        int current = this.start;
        this.trail.add(current);
        Integer next;
        while((next = backtrackMap.get(current)) != null){
            this.trail.add(next);
            current = next;
        }
    }

    @Override
    public HashMap<Integer, Integer> getBacktrackMap() {
        return this.backtrackMap;
    }

    @Override
    public ImmutableList<Integer> getTrail() {
        return ImmutableList.copyOf(this.trail);
    }
}
