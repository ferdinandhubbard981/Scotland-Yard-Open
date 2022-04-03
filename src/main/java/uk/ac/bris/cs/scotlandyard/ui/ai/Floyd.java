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
    ArrayList<ArrayList<Integer>> nextNode;
    ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> referenceGraph;

    public Floyd(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
        for (int i = 0; i < 200; i++){
            ArrayList<Integer> distances = new ArrayList<>(Collections.nCopies(200, Integer.MAX_VALUE));
            ArrayList<Integer> nodes = new ArrayList<>(Collections.nCopies(200, null));
            minimumDistances.add(i, distances);
            nextNode.add(i, nodes);
        }
        this.referenceGraph = graph;
    }
    public void setInitialEdgesDistances(){
        this.referenceGraph.edges().forEach(endpointPair -> {
            int outboundNode = endpointPair.nodeU();
            int inboundNode = endpointPair.nodeV();
            ImmutableSet<ScotlandYard.Transport> methodsForNode = this.referenceGraph
                    .edgeValue(outboundNode, inboundNode)
                    .orElse(null);
            int distance = 1; //can set what to minimise
            //if (methodsForNode == null)
            //distance is minimum number of tickets needed to traverse edge?
            minimumDistances.get(outboundNode).set(inboundNode, distance);
            nextNode.get(outboundNode).set(inboundNode, inboundNode);
        });
    }
    //this is for any loops inside the map (which I don't believe there are)
    public void setLoopDistances(){
        this.referenceGraph.edges()
                .stream()
                .filter(pair -> pair.nodeU().equals(pair.nodeV()))
                .forEach(pair -> {
                    minimumDistances.get(pair.nodeU()).set(pair.nodeU(), 0);
                    nextNode.get(pair.nodeU()).set(pair.nodeU(), pair.nodeU());
        });
    }
    public void useFloyd(){
        for (int k = 0; k < 200; k++){
            for (int i = 0; i < 200; i++){
                for (int j = 0; j < 200; j++){
                    if (minimumDistances.get(i).get(j) > minimumDistances.get(i).get(k) + minimumDistances.get(k).get(j)){
                        minimumDistances.get(i).set(j,
                                minimumDistances.get(i).get(k) + minimumDistances.get(k).get(j));
                        nextNode.get(i).set(j, nextNode.get(i).get(k));
                    }
                }
            }
        }
    }
    public ImmutableList<Integer> getPath(int start, int end){
        //probably handles start/end not in graph
        if (nextNode.get(start).get(end) == null) return ImmutableList.of();
        List<Integer> path = List.of(start);
        while(start != end){
            start = nextNode.get(start).get(end);
            path.add(start);
        }
        return ImmutableList.copyOf(path);
    }
}
