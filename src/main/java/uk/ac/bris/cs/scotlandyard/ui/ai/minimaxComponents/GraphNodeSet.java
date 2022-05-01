package uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents;

import java.util.*;
import java.util.stream.Collectors;

public class GraphNodeSet {
    //node : distance
    TreeMap<Integer, Integer> internal;
    public GraphNodeSet(){
        this.internal = new TreeMap<>();
    }
    public GraphNodeSet(GraphNode... nodes){
        this();
        for (GraphNode node : nodes) add(node);
    }
    public boolean contains(GraphNode node) {
        return this.internal.containsKey(node.getKey());
    }
    public boolean contains(int key){
        return this.internal.containsKey(key);
    }
    public void add(GraphNode node){
        if (this.contains(node)) {
            if (this.internal.get(node.getKey()) < node.getValue()) return;
            this.remove(node);
        }
        this.internal.put(node.getKey(), node.getValue());
    }
    public boolean remove(GraphNode node) {
        if (this.internal.size() == 0) throw new IllegalArgumentException("cannot remove from empty set");
        if (!this.contains(node)) return false;
        this.internal.remove(node.getKey());
        return true;
    }
    public List<GraphNode> getNodes(){
        if (this.internal.size() == 0) return List.of();
        TreeMap<Integer, Integer> byValues = new TreeMap<>((o1, o2) -> Integer.compare(o2, o1));
        byValues.putAll(this.internal);
        return byValues.entrySet().stream().map(entry -> new GraphNode(entry.getKey(), entry.getValue())).toList();
    }

    public GraphNode pop(){
        if (this.internal.size() == 0) throw new IndexOutOfBoundsException("cannot pop from empty set");
        GraphNode node = this.getNodes().get(0);
        this.internal.remove(node.getKey());
        return node;
    }
    public GraphNode get(int key){
        if (!this.internal.containsKey(key)) throw new IllegalArgumentException("key=" + key + " could not be got from set");
        return new GraphNode(key, this.internal.get(key));
    }
    public void set(int key, int value){
        if (!this.internal.containsKey(key)) throw new IllegalArgumentException("key=" + key + " could not be got from set");
        this.internal.put(key, value);
    }
    public int size(){
        return this.internal.size();
    }
}
