package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.*;


/**
 * @see <a href="https://en.wikipedia.org/wiki/Floyd%E2%80%93Warshall_algorithm">Floyd's Algorithm</a>
 */
public class Floyd {
    ArrayList<ArrayList<Integer>> minimumDistances;
    ArrayList<ArrayList<Integer>> nextNodes;
    ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> referenceGraph;
    boolean isMrX;

    /**
     * @param graph the game board
     * @param isMrX the perspective of the current player
     */
    public Floyd(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph, boolean isMrX){
        for (int i = 0; i < 200; i++){
            ArrayList<Integer> distances = new ArrayList<>(Collections.nCopies(200, Integer.MAX_VALUE));
            ArrayList<Integer> nodes = new ArrayList<>(Collections.nCopies(200, null));
            minimumDistances.add(i, distances);
            nextNodes.add(i, nodes);
        }
        this.referenceGraph = graph;
        this.isMrX = isMrX;
    }

    /**
     * @apiNote only needs to be run once
     */
    public void setInitialEdgesDistances(){
        this.referenceGraph.edges().forEach(endpointPair -> {
            int outboundNode = endpointPair.nodeU();
            int inboundNode = endpointPair.nodeV();
            ImmutableSet<ScotlandYard.Transport> methodsForNode = this.referenceGraph
                    .edgeValue(outboundNode, inboundNode)
                    .orElse(null);
            int distance = 1;
            minimumDistances.get(outboundNode).set(inboundNode, distance);
            nextNodes.get(outboundNode).set(inboundNode, inboundNode);
        });
    }

    /**
     *
     * @param type -1 if replacing original with a greater distance, else 1 for lesser
     * @param start starting node of optimisation
     * @param end ending node of optimisation
     * @param alt potential alternative node to use
     */
    private void setDistances(int type, int start, int end, int alt){
        final int potentialAlt = minimumDistances.get(start).get(alt) + minimumDistances.get(alt).get(end);
        if(minimumDistances.get(start).get(end)/*original*/.compareTo(potentialAlt) == type){
            minimumDistances.get(start).set(end, potentialAlt);
            nextNodes.get(start).set(end, nextNodes.get(start).get(alt));
        }
    }

    /**
     * @apiNote only needs to be run once
     */
    public void optimise(){
        for (int k = 0; k < 200; k++){
            for (int i = 0; i < 200; i++){
                for (int j = 0; j < 200; j++){
                    setDistances(this.isMrX ? -1 : 1, i, j, k);
                }
            }
        }
    }

    /**
     * @param start current position of player
     * @param end position of destination player
     * @return position of the best node to go to.
     */
    public int getNextNode(int start, int end){
        return nextNodes.get(start).get(end);
    }
}
