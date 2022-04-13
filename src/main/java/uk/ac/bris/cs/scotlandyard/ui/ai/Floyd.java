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
    HashMap<Integer, HashMap<Integer, Integer>> minimumDistances = new HashMap<>();
    HashMap<Integer, HashMap<Integer, Integer>> nextNodes = new HashMap<>();
    ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> referenceGraph;
    boolean isMrX;

    /**
     * @param graph the game board
     * @param isMrX the perspective of the current player
     */
    public Floyd(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph, boolean isMrX){
        for (int i = 1; i < 200; i++){
            HashMap<Integer, Integer> distances = new HashMap<>();
            HashMap<Integer, Integer> nodes = new HashMap<>();
            for (int j = 1; i < 200; j++){
                distances.put(j, Integer.MAX_VALUE);
                nodes.put(j, null);
            }
            minimumDistances.put(i, distances);
            nextNodes.put(i, nodes);
        }
        this.referenceGraph = graph;
        this.isMrX = isMrX;
        this.setInitialEdgesDistances();
        this.optimise();
    }

    /**
     * @apiNote only needs to be run once
     */
    private void setInitialEdgesDistances(){
        this.referenceGraph.edges().forEach(endpointPair -> {
            int outboundNode = endpointPair.nodeU();
            int inboundNode = endpointPair.nodeV();
//            ImmutableSet<ScotlandYard.Transport> methodsForNode = this.referenceGraph
//                    .edgeValue(outboundNode, inboundNode)
//                    .orElse(null); //if we want to do something using number of transport methods
            int distance = 1;
            minimumDistances.get(outboundNode).put(inboundNode, distance);
            nextNodes.get(outboundNode).put(inboundNode, inboundNode);
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
            minimumDistances.get(start).put(end, potentialAlt);
            nextNodes.get(start).put(end, nextNodes.get(start).get(alt));
        }
    }

    /**
     * @apiNote only needs to be run once
     */
    private void optimise(){
        for (int k = 1; k < 200; k++){
            for (int i = 1; i < 200; i++){
                for (int j = 1; j < 200; j++){
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
