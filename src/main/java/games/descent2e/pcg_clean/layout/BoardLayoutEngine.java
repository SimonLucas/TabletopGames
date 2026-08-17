package games.descent2e.pcg_clean.layout;

import games.descent2e.pcg_clean.data.PieceCatalog;
import games.descent2e.pcg_clean.domain.*;

import java.util.*;

/** Derives a grid embedding and a piece graph from a compact genome. */
public final class BoardLayoutEngine {
    private final PieceCatalog catalog;
    private final ConnectorDepth connectorDepth;

    public BoardLayoutEngine(PieceCatalog catalog) { this(catalog, ConnectorDepth.ZERO); }

    public BoardLayoutEngine(PieceCatalog catalog, ConnectorDepth connectorDepth) {
        this.catalog = catalog;
        this.connectorDepth = Objects.requireNonNull(connectorDepth);
    }

    public BoardLayout layout(BoardGenome genome) {
        Map<Integer, PlacedTile> tiles = new LinkedHashMap<>();
        genome.tiles().forEach(tile -> {
            if (tiles.putIfAbsent(tile.instanceId(), tile) != null)
                throw new IllegalArgumentException("Duplicate tile instance: " + tile.instanceId());
        });
        Map<Integer, GridPoint> origins = placeTiles(genome, tiles);
        List<String> problems = new ArrayList<>();
        if (origins.size() != tiles.size()) problems.add("Connection graph does not reach every tile");
        validateConnections(genome, tiles, origins, problems);
        Map<GridPoint, Cell> cells = render(tiles, origins, problems);
        return new BoardLayout(origins, cells, graph(genome), problems);
    }

    private void validateConnections(BoardGenome genome, Map<Integer, PlacedTile> tiles,
                                     Map<Integer, GridPoint> origins, List<String> problems) {
        for (TileConnection edge : genome.connections()) {
            PlacedTile first = tiles.get(edge.firstTile());
            PlacedTile second = tiles.get(edge.secondTile());
            GridPoint firstOrigin = origins.get(edge.firstTile());
            GridPoint secondOrigin = origins.get(edge.secondTile());
            if (first == null || second == null) {
                problems.add("Connection references an unknown tile: " + edge);
            } else if (firstOrigin != null && secondOrigin != null && !PortGeometry.aligned(
                    first, firstOrigin, edge.firstPort(), second, secondOrigin, edge.secondPort(),
                    catalog, connectorDepth)) {
                problems.add("Connection ports are not physically aligned: " + edge);
            }
        }
    }

    private Map<Integer, GridPoint> placeTiles(BoardGenome genome, Map<Integer, PlacedTile> tiles) {
        Map<Integer, GridPoint> origins = new LinkedHashMap<>();
        origins.put(genome.tiles().get(0).instanceId(), new GridPoint(0, 0));
        boolean changed;
        do {
            changed = false;
            for (TileConnection edge : genome.connections()) {
                boolean firstKnown = origins.containsKey(edge.firstTile());
                boolean secondKnown = origins.containsKey(edge.secondTile());
                if (firstKnown == secondKnown) continue;
                if (firstKnown) origins.put(edge.secondTile(), attachedOrigin(tiles.get(edge.firstTile()), origins.get(edge.firstTile()),
                        edge.firstPort(), tiles.get(edge.secondTile()), edge.secondPort()));
                else origins.put(edge.firstTile(), attachedOrigin(tiles.get(edge.secondTile()), origins.get(edge.secondTile()),
                        edge.secondPort(), tiles.get(edge.firstTile()), edge.firstPort()));
                changed = true;
            }
        } while (changed);
        return origins;
    }

    private GridPoint attachedOrigin(PlacedTile fixed, GridPoint fixedOrigin, int fixedPort,
                                     PlacedTile moving, int movingPort) {
        Port a = rotated(fixed).port(fixedPort);
        Port b = rotated(moving).port(movingPort);
        if (a.direction() != b.direction().opposite() || a.cells().size() != b.cells().size())
            return new GridPoint(Integer.MAX_VALUE / 4, Integer.MAX_VALUE / 4);
        GridPoint origin = PortGeometry.attachedOrigin(a, fixedOrigin, b, connectorDepth);
        return origin == null ? new GridPoint(Integer.MAX_VALUE / 4, Integer.MAX_VALUE / 4) : origin;
    }

    private Map<GridPoint, Cell> render(Map<Integer, PlacedTile> tiles, Map<Integer, GridPoint> origins,
                                        List<String> problems) {
        Map<GridPoint, Cell> result = new LinkedHashMap<>();
        for (PlacedTile placement : tiles.values()) {
            GridPoint origin = origins.get(placement.instanceId());
            if (origin == null) continue;
            RotatedTile tile = rotated(placement);
            for (int y = 0; y < tile.height(); y++) for (int x = 0; x < tile.width(); x++) {
                Cell cell = tile.cellAt(x, y);
                if (cell == Cell.VOID || (connectorDepth == ConnectorDepth.ZERO && cell == Cell.OPEN)) continue;
                GridPoint location = origin.plus(new GridPoint(x, y));
                Cell existing = result.putIfAbsent(location, cell);
                if (existing != null && !(existing == Cell.OPEN && cell == Cell.OPEN))
                    problems.add("Tiles overlap at " + location);
            }
        }
        return result;
    }

    private Map<Integer, Set<Integer>> graph(BoardGenome genome) {
        Map<Integer, Set<Integer>> graph = new LinkedHashMap<>();
        genome.tiles().forEach(t -> graph.put(t.instanceId(), new LinkedHashSet<>()));
        genome.connections().forEach(e -> {
            graph.computeIfAbsent(e.firstTile(), ignored -> new LinkedHashSet<>()).add(e.secondTile());
            graph.computeIfAbsent(e.secondTile(), ignored -> new LinkedHashSet<>()).add(e.firstTile());
        });
        return graph;
    }

    private RotatedTile rotated(PlacedTile tile) {
        if (tile == null) throw new IllegalArgumentException("Connection references an unknown tile");
        return catalog.require(tile.tileId()).rotate(tile.quarterTurns());
    }
}
