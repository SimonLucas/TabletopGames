package games.descent2e.pcg_clean.layout;

import games.descent2e.pcg_clean.data.PieceCatalog;
import games.descent2e.pcg_clean.domain.*;

import java.util.HashSet;
import java.util.Set;

/** Spatial operations on rotated ports, shared by layout validation and loop closure. */
public final class PortGeometry {
    private PortGeometry() {}

    public static boolean aligned(PlacedTile first, GridPoint firstOrigin, int firstPort,
                                  PlacedTile second, GridPoint secondOrigin, int secondPort,
                                  PieceCatalog catalog) {
        return aligned(first, firstOrigin, firstPort, second, secondOrigin, secondPort,
                catalog, ConnectorDepth.ZERO);
    }

    public static boolean aligned(PlacedTile first, GridPoint firstOrigin, int firstPort,
                                  PlacedTile second, GridPoint secondOrigin, int secondPort,
                                  PieceCatalog catalog, ConnectorDepth connectorDepth) {
        Port a = catalog.require(first.tileId()).rotate(first.quarterTurns()).port(firstPort);
        Port b = catalog.require(second.tileId()).rotate(second.quarterTurns()).port(secondPort);
        if (a.direction() != b.direction().opposite() || a.cells().size() != b.cells().size()) return false;
        return aligned(a, firstOrigin, b, secondOrigin, connectorDepth);
    }

    public static boolean aligned(Port fixed, GridPoint fixedOrigin, Port moving, GridPoint movingOrigin,
                                  ConnectorDepth connectorDepth) {
        if (fixed.direction() != moving.direction().opposite()
                || fixed.cells().size() != moving.cells().size()) return false;
        Set<GridPoint> target = globalCells(fixed, fixedOrigin);
        if (connectorDepth == ConnectorDepth.ZERO)
            target = shifted(target, -fixed.direction().dx(), -fixed.direction().dy());
        return target.equals(globalCells(moving, movingOrigin));
    }

    public static GridPoint attachedOrigin(Port fixed, GridPoint fixedOrigin, Port moving,
                                           ConnectorDepth connectorDepth) {
        if (fixed.direction() != moving.direction().opposite()
                || fixed.cells().size() != moving.cells().size()) return null;
        GridPoint target = fixed.cells().get(0).plus(fixedOrigin);
        if (connectorDepth == ConnectorDepth.ZERO)
            target = target.plus(new GridPoint(-fixed.direction().dx(), -fixed.direction().dy()));
        for (GridPoint anchor : moving.cells()) {
            GridPoint candidate = new GridPoint(target.x() - anchor.x(), target.y() - anchor.y());
            if (aligned(fixed, fixedOrigin, moving, candidate, connectorDepth)) return candidate;
        }
        return null;
    }

    public static Set<GridPoint> globalCells(Port port, GridPoint origin) {
        Set<GridPoint> result = new HashSet<>();
        port.cells().stream().map(origin::plus).forEach(result::add);
        return result;
    }

    private static Set<GridPoint> shifted(Set<GridPoint> points, int dx, int dy) {
        Set<GridPoint> result = new HashSet<>();
        points.stream().map(point -> point.plus(new GridPoint(dx, dy))).forEach(result::add);
        return result;
    }
}
