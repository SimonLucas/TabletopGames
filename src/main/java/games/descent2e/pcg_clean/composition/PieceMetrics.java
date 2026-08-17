package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.domain.Cell;
import games.descent2e.pcg_clean.domain.Direction;
import games.descent2e.pcg_clean.domain.PieceInventory;

import java.util.*;

/** Intrinsic measurements cached on atomic or composed definitions. */
public record PieceMetrics(int atomicPieces, int traversableCells, int width, int height,
                           double compactness, int exposedPorts,
                           Map<Direction, Integer> portsByDirection,
                           int internalBranches, int internalCycles,
                           Map<Cell, Double> terrainComposition, PieceInventory inventory) {
    public PieceMetrics {
        portsByDirection = Collections.unmodifiableMap(new EnumMap<>(portsByDirection));
        terrainComposition = Collections.unmodifiableMap(new EnumMap<>(terrainComposition));
    }
}
