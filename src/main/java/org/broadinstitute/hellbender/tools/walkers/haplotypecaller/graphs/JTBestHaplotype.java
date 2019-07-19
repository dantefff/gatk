package org.broadinstitute.hellbender.tools.walkers.haplotypecaller.graphs;

import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.tools.walkers.haplotypecaller.readthreading.ExperimentalReadThreadingGraph;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A best haplotype object for being used with junction trees.
 *
 * Each path holds a list of all the junction trees describing its current path. This list consists of pointers to nodes
 * in the junction trees corresponding to the paths that have already been taken by the JTBestHaplotype object.
 *
 * In order to invoke the junction trees simply call {@link #getApplicableNextEdgesBasedOnJunctionTrees} which will return a list
 * of cloned path objects corresponding to each path present in the eldest tree. This method handles popping old trees with insufficient
 * data off of the list as well as incrementing all of the trees in the list to point at the next element based on the chosen path.
 */
public class JTBestHaplotype<T extends BaseVertex, E extends BaseEdge> extends KBestHaplotype<T, E> {
    private JunctionTreeView treesInQueue; // An object for storing and managing operations on the queue of junction trees active for this path

    public JTBestHaplotype(final JTBestHaplotype p, final List<E> edgesToExtend, final double edgePenalty) {
        super(p, edgesToExtend, edgePenalty);
        treesInQueue = p.treesInQueue.clone();
//        edgesTaken = new HashSet<>(edgesTaken);
    }

    // Constructor to be used for internal calls from {@link #getApplicableNextEdgesBasedOnJunctionTrees()}
    public JTBestHaplotype(final JTBestHaplotype p, final List<E> chain, final int edgeMultiplicity, final int totalOutgoingMultiplicity) {
        super(p, chain, computeLogPenaltyScore( edgeMultiplicity, totalOutgoingMultiplicity));
        treesInQueue = p.treesInQueue.clone();
        // Ensure that the relevant edge has been traversed
        treesInQueue.takeEdge(chain.get(chain.size() - 1));
//        edgesTaken =  new HashSet<>(edgesTaken);
    }

    public JTBestHaplotype(final T initialVertex, final BaseGraph<T,E> graph) {
        super(initialVertex, graph);
        treesInQueue = new JunctionTreeView();
//        edgesTaken = new HashSet<>();
    }

    //TODO this needs to be the same logic as the blow method, this is temporary
    // returns true if there is a symbolic edge pointing to the reference end or if there is insufficient node data
    public boolean hasStoppingEvidence(final int weightThreshold) {
        int currentActiveNodeIndex = 0;
        ExperimentalReadThreadingGraph.ThreadingNode eldestTree = treesInQueue.isEmpty() ? null : treesInQueue.get(currentActiveNodeIndex);
        int totalOut = getTotalOutForBranch(eldestTree);

        // Keep removing trees until we find one under our threshold TODO this should be in a helper method
        while (eldestTree != null && totalOut < weightThreshold) {
            // We remove old branches from the tree only if they no longer have any evidence, otherwise we look at younger branches
            if (totalOut <= 0) {
                treesInQueue.removeEldestTree();
            } else { // Otherwise look at the next tree in the list
                currentActiveNodeIndex++;
            }
            eldestTree = currentActiveNodeIndex >= treesInQueue.size() ? null : treesInQueue.get(currentActiveNodeIndex);
            totalOut = getTotalOutForBranch(eldestTree);
        }

        if (eldestTree != null) {
            for ( ExperimentalReadThreadingGraph.ThreadingNode node : eldestTree.getChildrenNodes().values()) {
                if (node.isSymbolicEnd()) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }


    private int getTotalOutForBranch(ExperimentalReadThreadingGraph.ThreadingNode eldestTree) {
        int totalOut = 0;
        if (eldestTree != null) {
            for (ExperimentalReadThreadingGraph.ThreadingNode node : eldestTree.getChildrenNodes().values()) {
                totalOut += node.getCount();
            }
        }
        return totalOut;
    }

    /**
     * This method is the primary logic of deciding how to traverse junction paths and with what score.
     *
     * TODO this will likely change to use the eldest tree regardless of threshold passage
     * This method checks the list of junction tree nodes, looking first at the eldest tree to perform the following:
     *  - Checks the total outgoing weight, if its below weight threshold then the tree is popped and a new tree is considered
     *  - For each path in the oldest tree clones this path with the chain edges added, taking the edge target for each path present in the tree.
     *
     * @param chain List of edges to add between the current path and the junction tree edge
     * @param weightThreshold threshold of evidence under which old junction trees are discarded.
     * @return A list of new RTBestHaplotypeObjects corresponding to each path chosen from the exisitng junction trees,
     *         or an empty list if there is no path illuminated by junction trees.
     */
    //TODO for reviewer - is this the best way to structure this? I'm not sure how to decide about end nodes based on this, passing them back seesm wrong
    @SuppressWarnings({"unchecked"})
    public List<JTBestHaplotype<T, E>> getApplicableNextEdgesBasedOnJunctionTrees(final List<E> chain, final Set<E> outgoingEdges, final int weightThreshold) {
        Set<MultiSampleEdge> edgesAccountedForByJunctionTrees = new HashSet<>(); // Since we check multiple junction trees for paths, make sure we are getting
        List<JTBestHaplotype<T, E>> output = new ArrayList<>();
        int currentActiveNodeIndex = 0;
        ExperimentalReadThreadingGraph.ThreadingNode eldestTree = treesInQueue.isEmpty() ? null : treesInQueue.get(currentActiveNodeIndex);
        while (eldestTree != null) {
            //TODO this can be better, need to create a tree "view" object that tracks the current node more sanely
            int totalOut = getTotalOutForBranch(eldestTree);

            // If the total evidence emerging from a given branch
            if (totalOut >= weightThreshold) {
                //TODO add SOME sanity check to ensure that the vertex we stand on and the edges we are polling line up
                for (Map.Entry<MultiSampleEdge, ExperimentalReadThreadingGraph.ThreadingNode> childNode : eldestTree.getChildrenNodes().entrySet()) {
                    if (!outgoingEdges.contains(childNode.getKey())) {
                        throw new GATKException("While constructing graph, there was an incongruity between a JunctionTree edge and the edge present on graph traversal");
                    }

                    // Don't add edges to the symbolic end vertex here at all, thats handled elsewhere
                    if (!childNode.getValue().isSymbolicEnd() && !edgesAccountedForByJunctionTrees.contains(childNode.getKey())) {
                        edgesAccountedForByJunctionTrees.add(childNode.getKey());
                        ExperimentalReadThreadingGraph.ThreadingNode child = childNode.getValue();
                        List<E> chainCopy = new ArrayList<>(chain);
                        chainCopy.add((E) childNode.getKey());
                        output.add(new JTBestHaplotype<>(this, chainCopy, child.getCount(), totalOut));
                    }
                }
                return output;

            // If there isn't enough outgoing evidence, then we
            } else {
                // We remove old branches from the tree only if they no longer have any evidence, otherwise we look at younger branches
                if (totalOut <= 0) {
                    treesInQueue.removeEldestTree();
                } else { // Otherwise look at the next tree in the list
                    currentActiveNodeIndex++;
                }
                eldestTree = currentActiveNodeIndex >= treesInQueue.size() ? null : treesInQueue.get(currentActiveNodeIndex);
            }
        }
        // If we hit this point, then eldestTree == null, suggesting that none of the nodes exceeded our threshold for evidence (though some may have found evidence)
        // Standard behavior from the old GraphBasedKBestHaplotypeFinder, base our next path on the edge weights instead
        int totalOutgoingMultiplicity = 0;
        for (final BaseEdge edge : outgoingEdges) {
            totalOutgoingMultiplicity += edge.getMultiplicity();
        }

        // Add all valid edges to the graph
        for (final E edge : outgoingEdges) {
            // Don't traverse an edge if it only has reference evidence supporting it (unless there is no other evidence whatsoever)
            if (!edgesAccountedForByJunctionTrees.contains((MultiSampleEdge) edge) &&
                    totalOutgoingMultiplicity != 0 && edge.getMultiplicity() != 0) {
                // only include

                List<E> chainCopy = new ArrayList<>(chain);
                chainCopy.add(edge);
                output.add(new JTBestHaplotype<>(this, chainCopy, edge.getMultiplicity(), totalOutgoingMultiplicity));
            }
        }
        return output;
    }

    /**
     * Add a junction tree (corresponding to the current vertex for traversal, note that if a tree has already been visited by this path then it is ignored)
     * @param junctionTreeForNode
     */
    public void addJunctionTree(final ExperimentalReadThreadingGraph.ThreadingTree junctionTreeForNode) {
        treesInQueue.addJunctionTree(junctionTreeForNode);
    }

    /**
     * A helper class for managing the various junction tree operations that need to be done by JTBestHaplotypeFinder
     */
    private class JunctionTreeView {
        Set<ExperimentalReadThreadingGraph.ThreadingTree> visitedTrees;
        List<ExperimentalReadThreadingGraph.ThreadingNode> activeNodes;

        protected JunctionTreeView() {
            visitedTrees = new HashSet<>();
            activeNodes = new ArrayList<>(5);
        }

        protected JunctionTreeView(HashSet<ExperimentalReadThreadingGraph.ThreadingTree> threadingTrees, ArrayList<ExperimentalReadThreadingGraph.ThreadingNode> es) {
            this.visitedTrees = threadingTrees;
            this.activeNodes = es;
        }

        protected JunctionTreeView clone() {
            return new JunctionTreeView(new HashSet<>(visitedTrees), new ArrayList<>(activeNodes));
        }

        // Add a junction tree, ensuring that there is a valid tree in ordre to check.
        public void addJunctionTree(final ExperimentalReadThreadingGraph.ThreadingTree junctionTreeForNode) {
            visitedTrees.add(junctionTreeForNode);
            activeNodes.add(junctionTreeForNode.getRootNode());
        }

        // method to handle incrementing all of the nodes in the tree simultaniously
        public void takeEdge(E edgeTaken) {
            activeNodes = activeNodes.stream().map(node -> {
                if (!node.getChildrenNodes().containsKey(edgeTaken)) {
                    return null;
                }
                return node.getChildrenNodes().get(edgeTaken);
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }

        private ExperimentalReadThreadingGraph.ThreadingNode get(int i) {
            return activeNodes.get(i);
        }

        private boolean isEmpty() {
            return activeNodes == null || activeNodes.isEmpty();
        }

        private int size() {
            return activeNodes == null ? 0 : activeNodes.size();
        }

        private void removeEldestTree() {
            activeNodes.remove(0);
        }
    }
}
