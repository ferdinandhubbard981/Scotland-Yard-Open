package uk.ac.bris.cs.scotlandyard.ui.ai.minimaxComponents;

import java.util.Comparator;

public class GraphNode implements Comparable<GraphNode> {
    private int key;
    private int value;

    public GraphNode(int key, int value) {
        this.key = key;
        this.value = value;
    }

    public int getKey() {
        return key;
    }

    public int getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return this.key;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof GraphNode) && ((GraphNode) o).getKey() == (this.key);
    }

    @Override
    public int compareTo(GraphNode o) {
        return (this.equals(o) ? 0 : this.value < o.getValue() ? -1 : 1);
    }
}
