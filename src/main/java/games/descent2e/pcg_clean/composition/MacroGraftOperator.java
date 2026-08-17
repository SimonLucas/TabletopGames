package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.domain.*;
import games.descent2e.pcg_clean.layout.ConnectorDepth;
import games.descent2e.pcg_clean.layout.PortGeometry;

import java.util.*;

/**
 * Valid-by-construction one-port graft for atomic or composite definitions.
 * Every returned child has aligned ports, connected internals and no terrain overlap.
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
                try {
                    MacroPieceDefinition child = composer.compose(idPrefix + '-' + sequence++, List.of(
                                    new MacroPlacement(0, fixed, 0, 0, 0),
                                    new MacroPlacement(1, donor, donorOrigin.x(), donorOrigin.y(), turns)),
                            List.of(new MacroConnection(0, fixedPort.index(), 1, donorPort.index())));
                    distinct.putIfAbsent(child.canonicalSignature(), child);
                } catch (IllegalArgumentException ignored) {
                    // Another part of the two irregular footprints collided; this transform is not a valid graft.
                }
            }
        }
        return List.copyOf(distinct.values());
    }
}
