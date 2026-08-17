package games.descent2e.pcg_clean.qd;

import java.util.List;

/** N-dimensional archive coordinate, independent of any particular descriptor. */
public record BehaviorCell(List<Integer> bins) implements Comparable<BehaviorCell> {
    public BehaviorCell { bins = List.copyOf(bins); }

    @Override
    public int compareTo(BehaviorCell other) {
        int common = Math.min(bins.size(), other.bins.size());
        for (int i = 0; i < common; i++) {
            int comparison = Integer.compare(bins.get(i), other.bins.get(i));
            if (comparison != 0) return comparison;
        }
        return Integer.compare(bins.size(), other.bins.size());
    }
}
