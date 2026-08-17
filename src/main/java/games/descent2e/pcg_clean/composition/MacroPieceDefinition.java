package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.domain.*;

import java.util.*;

/** A compiled composite whose public geometry is indistinguishable from an atomic definition. */
public record MacroPieceDefinition(String id, int width, int height, List<Cell> cells,
                                   List<Port> ports, List<MacroPlacement> children,
                                   List<MacroConnection> internalConnections,
                                   List<MacroPortSource> portSources,
                                   PieceInventory inventory, PieceMetrics metrics,
                                   String canonicalSignature) implements PieceDefinition {
    public MacroPieceDefinition {
        if (id == null || id.isBlank() || width <= 0 || height <= 0 || cells.size() != width * height)
            throw new IllegalArgumentException("Invalid macro definition");
        cells = List.copyOf(cells);
        ports = List.copyOf(ports);
        children = List.copyOf(children);
        internalConnections = List.copyOf(internalConnections);
        portSources = List.copyOf(portSources);
        if (portSources.size() != ports.size())
            throw new IllegalArgumentException("Every macro port needs one child source");
        for (int i = 0; i < portSources.size(); i++)
            if (portSources.get(i).macroPort() != i)
                throw new IllegalArgumentException("Macro port sources must follow stable port order");
        inventory = Objects.requireNonNull(inventory);
        metrics = Objects.requireNonNull(metrics);
        canonicalSignature = Objects.requireNonNull(canonicalSignature);
    }

    @Override
    public RotatedTile rotate(int quarterTurns) {
        // TileDefinition's rotation is geometry-only and is shared safely by macros.
        return new TileDefinition(id, width, height, cells, ports).rotate(quarterTurns);
    }
}
