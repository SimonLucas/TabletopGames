package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.domain.*;
import games.descent2e.pcg_clean.layout.ConnectorDepth;
import games.descent2e.pcg_clean.layout.PortGeometry;

import java.util.*;

/**
 * Valid-by-construction anchored graft for atomic or composite definitions. One port pair
 * determines placement; any other unambiguous coincident pairs close naturally occurring loops.
 */
public final class MacroGraftOperator {
    private final MacroPieceComposer composer;

    public MacroGraftOperator() { this(new MacroPieceComposer()); }
    public MacroGraftOperator(MacroPieceComposer composer) { this.composer = Objects.requireNonNull(composer); }

    public List<MacroPieceDefinition> graft(String idPrefix, PieceDefinition fixed, PieceDefinition donor) {
        Map<String, MacroPieceDefinition> distinct = new LinkedHashMap<>();
        RotatedTile fixedGeometry = fixed.rotate(0);
        int sequence = 0;
        for (Port fixedPort : fixedGeometry.ports()) for (int turns = 0; turns < 4; turns++) {
            RotatedTile donorGeometry = donor.rotate(turns);
            for (Port donorPort : donorGeometry.ports()) {
                GridPoint donorOrigin = PortGeometry.attachedOrigin(fixedPort, new GridPoint(0, 0),
                        donorPort, ConnectorDepth.ZERO);
                if (donorOrigin == null) continue;
                List<MacroConnection> connections = coincidentConnections(
                        fixedGeometry, donorGeometry, donorOrigin);
                if (connections.isEmpty()) continue;
                try {
                    MacroPieceDefinition child = composer.compose(idPrefix + '-' + sequence++, List.of(
                                    new MacroPlacement(0, fixed, 0, 0, 0),
                                    new MacroPlacement(1, donor, donorOrigin.x(), donorOrigin.y(), turns)),
                            connections);
                    distinct.putIfAbsent(child.canonicalSignature(), child);
                } catch (IllegalArgumentException ignored) {
                    // Another part of the two irregular footprints collided; this transform is not a valid graft.
                }
            }
        }
        return List.copyOf(distinct.values());
    }

    /** Returns no proposal when one port would connect to multiple ports in the same placement. */
    private List<MacroConnection> coincidentConnections(RotatedTile fixed, RotatedTile donor,
                                                        GridPoint donorOrigin) {
        List<MacroConnection> matches = new ArrayList<>();
        Map<Integer, Integer> fixedUses = new HashMap<>();
        Map<Integer, Integer> donorUses = new HashMap<>();
        for (Port fixedPort : fixed.ports()) for (Port donorPort : donor.ports()) {
            if (!PortGeometry.aligned(fixedPort, new GridPoint(0, 0), donorPort, donorOrigin,
                    ConnectorDepth.ZERO)) continue;
            matches.add(new MacroConnection(0, fixedPort.index(), 1, donorPort.index()));
            fixedUses.merge(fixedPort.index(), 1, Integer::sum);
            donorUses.merge(donorPort.index(), 1, Integer::sum);
        }
        if (fixedUses.values().stream().anyMatch(uses -> uses > 1)
                || donorUses.values().stream().anyMatch(uses -> uses > 1)) return List.of();
        return matches.stream().sorted(Comparator.comparingInt(MacroConnection::firstPort)
                .thenComparingInt(MacroConnection::secondPort)).toList();
    }
}
