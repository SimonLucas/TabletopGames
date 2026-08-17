package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.qd.*;

import java.util.*;
import java.util.stream.IntStream;

/** Branch/cycle niches measured on the expanded physical graph, never on macro instance count. */
public final class ExpandedGraphStructureDescriptor implements QualityDescriptor<ExpandedBoardCandidate> {
    private static final int BRANCH_BINS = 6;
    private static final int CYCLE_BINS = 5;
    private final List<DescriptorAxis> axes = List.of(
            new DescriptorAxis("physical-branches", "Physical branching pieces", labels(BRANCH_BINS)),
            new DescriptorAxis("physical-cycles", "Physical graph cycles", labels(CYCLE_BINS)));

    @Override public List<DescriptorAxis> axes() { return axes; }

    @Override
    public BehaviorCell describe(ExpandedBoardCandidate candidate) {
        Map<Integer, Set<Integer>> graph = candidate.evaluation().expanded().layout().graph();
        int branches = (int) graph.values().stream().filter(neighbours -> neighbours.size() >= 3).count();
        // Distinct ports may connect the same pair of physical pieces. The neighbour graph
        // intentionally collapses those parallel edges, but each is topology-bearing for E-V+C.
        int edges = candidate.evaluation().expanded().genome().connections().size();
        int cycles = Math.max(0, edges - graph.size() + components(graph));
        return new BehaviorCell(List.of(Math.min(branches, BRANCH_BINS - 1),
                Math.min(cycles, CYCLE_BINS - 1)));
    }

    private int components(Map<Integer, Set<Integer>> graph) {
        Set<Integer> remaining = new HashSet<>(graph.keySet());
        int components = 0;
        while (!remaining.isEmpty()) {
            components++;
            visit(remaining.iterator().next(), graph, remaining);
        }
        return components;
    }

    private void visit(int id, Map<Integer, Set<Integer>> graph, Set<Integer> remaining) {
        if (!remaining.remove(id)) return;
        graph.getOrDefault(id, Set.of()).forEach(next -> visit(next, graph, remaining));
    }

    private static List<String> labels(int bins) {
        return IntStream.range(0, bins).mapToObj(bin -> bin == bins - 1 ? bin + "+" : Integer.toString(bin)).toList();
    }
}
