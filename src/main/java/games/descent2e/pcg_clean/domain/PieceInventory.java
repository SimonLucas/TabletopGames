package games.descent2e.pcg_clean.domain;

import java.util.*;

/** Immutable multiset of physical cardboard components used by a definition or assembly. */
public record PieceInventory(Map<String, Integer> quantities) {
    public PieceInventory {
        TreeMap<String, Integer> normalized = new TreeMap<>();
        quantities.forEach((id, count) -> {
            if (id == null || id.isBlank() || count == null || count < 0)
                throw new IllegalArgumentException("Invalid inventory entry");
            if (count > 0) normalized.merge(id.toLowerCase(Locale.ROOT), count, Integer::sum);
        });
        quantities = Collections.unmodifiableMap(normalized);
    }

    public static PieceInventory empty() { return new PieceInventory(Map.of()); }
    public static PieceInventory one(String physicalId) { return new PieceInventory(Map.of(physicalId, 1)); }

    public PieceInventory plus(PieceInventory other) {
        Map<String, Integer> result = new TreeMap<>(quantities);
        other.quantities.forEach((id, count) -> result.merge(id, count, Integer::sum));
        return new PieceInventory(result);
    }

    public int totalPieces() { return quantities.values().stream().mapToInt(Integer::intValue).sum(); }

    /** A/B faces are two faces of one physical component. */
    public static String physicalId(String definitionId) {
        if (definitionId.length() > 1) {
            char suffix = Character.toUpperCase(definitionId.charAt(definitionId.length() - 1));
            if (suffix == 'A' || suffix == 'B')
                return definitionId.substring(0, definitionId.length() - 1).toLowerCase(Locale.ROOT);
        }
        return definitionId.toLowerCase(Locale.ROOT);
    }
}
