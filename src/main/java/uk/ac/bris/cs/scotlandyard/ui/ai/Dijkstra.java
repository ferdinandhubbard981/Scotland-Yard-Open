package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.ImmutableValueGraph;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

interface DijkstraAlgorithm {
    void search();
    void backtrack(HashMap<Integer, Integer> backtrackMap);

    HashMap<Integer, Integer> getBacktrackMap();
    ImmutableList<Integer> getTrail();
}

public class Dijkstra<T> implements DijkstraAlgorithm {

    interface GraphNode {
        <T> T getNodeKey();
        <T> T getNodeValue();
    }

    static class Node<K extends Comparable<K>> implements GraphNode, Comparable<Node<K>> {
        K key;
        K value;
        public Node(K key, K value){
            this.key = key;
            this.value = value;
        }

        @Override
        public K getNodeKey() {
            return this.key;
        }

        @Override
        public K getNodeValue() {
            return this.value;
        }

        @Override
        public int compareTo(Node<K> o) {
            return this.value.compareTo(o.value);
        }
    }

    ImmutableValueGraph<Integer, T> graph;
    int start, end;
    HashMap<Integer, Integer> backtrackMap;
    ImmutableList<Integer> trail;
    Set<Integer> unvisited;
    HashMap<Integer, Integer> distances;

    int currentNode;
    TreeSet<Integer> neighbours;

    public Dijkstra(ImmutableValueGraph<Integer, T> graph, int start, int end, int totalNodes){
        this.graph = graph;
        this.start = start;
        this.end = end;
        this.unvisited = IntStream.range(1,200).boxed().collect(Collectors.toSet());
        for (int i = 1; i < totalNodes; i++){
            this.distances.put(i, Integer.MAX_VALUE);
        }
        this.distances.put(start, 0);
        this.currentNode = -1;
        this.neighbours = new TreeSet<>();
        this.neighbours.add(start);
    }

    @Override
    public void search() {
        while(!this.neighbours.isEmpty()){
            this.currentNode = this.neighbours.first();
            this.neighbours.remove(this.currentNode);
            if (this.currentNode == this.end) return;

            this.neighbours.addAll(this.graph.adjacentNodes(this.currentNode));
        }

    }

    @Override
    public void backtrack(HashMap<Integer, Integer> backtrackMap) {

    }

    @Override
    public HashMap<Integer, Integer> getBacktrackMap() {
        return null;
    }

    @Override
    public ImmutableList<Integer> getTrail() {
        return null;
    }
}
