package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.domain.*;

import java.util.*;

public final class PieceMetricCalculator {
    public PieceMetrics calculate(PieceDefinition definition, int internalBranches, int internalCycles) {
        return calculate(definition, definition.inventory(), internalBranches, internalCycles);
    }

    public PieceMetrics calculate(PieceGeometry geometry, PieceInventory inventory,
                                  int internalBranches, int internalCycles) {
        EnumMap<Cell, Integer> terrainCounts = new EnumMap<>(Cell.class);
        int terrain = 0;
        int traversable = 0;
        for (Cell cell : geometry.cells()) {
            if (cell == Cell.VOID || cell == Cell.OPEN) continue;
            terrain++;
            if (cell.traversable()) traversable++;
            terrainCounts.merge(cell, 1, Integer::sum);
        }
        EnumMap<Cell, Double> composition = new EnumMap<>(Cell.class);
        int totalTerrain = terrain;
        if (totalTerrain > 0) terrainCounts.forEach((cell, count) ->
                composition.put(cell, count / (double) totalTerrain));
        EnumMap<Direction, Integer> directions = new EnumMap<>(Direction.class);
        geometry.ports().forEach(port -> directions.merge(port.direction(), 1, Integer::sum));
        long boxArea = (long) geometry.width() * geometry.height();
        return new PieceMetrics(inventory.totalPieces(), traversable,
                geometry.width(), geometry.height(), boxArea == 0 ? 0 : terrain / (double) boxArea,
                geometry.ports().size(), directions, internalBranches, internalCycles,
                composition, inventory);
    }
}
