package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.domain.PieceInventory;

import java.util.*;

/** Policy-neutral inventory comparison; callers decide whether excess is fatal or penalised. */
public record InventoryAssessment(Map<String, Integer> excess, int totalExcess, int exceededTypes) {
    public InventoryAssessment {
        excess = Collections.unmodifiableMap(new TreeMap<>(excess));
    }

    public static InventoryAssessment compare(PieceInventory used, PieceInventory available) {
        Map<String, Integer> excess = new TreeMap<>();
        used.quantities().forEach((id, count) -> {
            int extra = count - available.quantities().getOrDefault(id, 0);
            if (extra > 0) excess.put(id, extra);
        });
        return new InventoryAssessment(excess,
                excess.values().stream().mapToInt(Integer::intValue).sum(), excess.size());
    }

    public boolean withinLimits() { return totalExcess == 0; }

    /** Smooth score in (0,1], suitable for SOFT_PENALTY. */
    public double score() { return 1.0 / (1.0 + totalExcess); }
}
