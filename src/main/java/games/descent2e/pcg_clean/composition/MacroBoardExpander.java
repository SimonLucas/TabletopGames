package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.data.PieceCatalog;
import games.descent2e.pcg_clean.domain.*;
import games.descent2e.pcg_clean.layout.*;

import java.util.*;

/** Recursively expands macro instances into a deterministic atomic board graph. */
public final class MacroBoardExpander {
    private record Endpoint(int tile, int port) {}
    private record TopPort(int instance, int port) {}
    private record Expansion(Map<Integer, Endpoint> exposedPorts) {}

    private final PieceCatalog catalog;
    private final List<PlacedTile> atomicTiles = new ArrayList<>();
    private final List<TileConnection> atomicConnections = new ArrayList<>();
    private final Map<Integer, GridPoint> expectedOrigins = new LinkedHashMap<>();
    private PieceInventory inventory = PieceInventory.empty();

    public MacroBoardExpander(PieceCatalog catalog) { this.catalog = Objects.requireNonNull(catalog); }

    public ExpandedBoard expand(BoardGenome genome) {
        atomicTiles.clear();
        atomicConnections.clear();
        expectedOrigins.clear();
        inventory = PieceInventory.empty();
        BoardLayout composed = new BoardLayoutEngine(catalog).layout(genome);
        if (!composed.assembled())
            throw new IllegalArgumentException("Cannot expand an invalid composed board: " + composed.problems());

        Map<TopPort, Endpoint> topPorts = new HashMap<>();
        for (PlacedTile placement : genome.tiles()) {
            PieceDefinition definition = catalog.require(placement.tileId());
            GridPoint origin = composed.origins().get(placement.instanceId());
            Expansion expansion = expandDefinition(definition, origin, placement.quarterTurns());
            expansion.exposedPorts.forEach((port, endpoint) ->
                    topPorts.put(new TopPort(placement.instanceId(), port), endpoint));
        }
        for (TileConnection connection : genome.connections()) {
            Endpoint first = require(topPorts, new TopPort(connection.firstTile(), connection.firstPort()));
            Endpoint second = require(topPorts, new TopPort(connection.secondTile(), connection.secondPort()));
            atomicConnections.add(new TileConnection(first.tile, first.port, second.tile, second.port));
        }
        BoardGenome expandedGenome = new BoardGenome(atomicTiles, atomicConnections);
        BoardLayout expandedLayout = new BoardLayoutEngine(catalog).layout(expandedGenome);
        if (!expandedLayout.assembled())
            throw new IllegalStateException("Expanded atomic board is invalid: " + expandedLayout.problems());
        verifyRelativeOrigins(expandedLayout.origins());
        return new ExpandedBoard(expandedGenome, expandedLayout, inventory);
    }

    private Expansion expandDefinition(PieceDefinition definition, GridPoint origin, int quarterTurns) {
        int turns = Math.floorMod(quarterTurns, 4);
        if (!(definition instanceof MacroPieceDefinition macro)) return atomic(definition, origin, turns);

        Map<Integer, Expansion> children = new LinkedHashMap<>();
        for (MacroPlacement placement : macro.children().stream()
                .sorted(Comparator.comparingInt(MacroPlacement::instanceId)).toList()) {
            RotatedTile childGeometry = placement.definition().rotate(placement.quarterTurns());
            GridPoint rotatedOrigin = rotateRectangleOrigin(macro.width(), macro.height(), placement.x(), placement.y(),
                    childGeometry.width(), childGeometry.height(), turns).plus(origin);
            children.put(placement.instanceId(), expandDefinition(placement.definition(), rotatedOrigin,
                    placement.quarterTurns() + turns));
        }
        for (MacroConnection connection : macro.internalConnections()) {
            Endpoint first = childEndpoint(children, connection.firstInstance(), connection.firstPort());
            Endpoint second = childEndpoint(children, connection.secondInstance(), connection.secondPort());
            atomicConnections.add(new TileConnection(first.tile, first.port, second.tile, second.port));
        }
        Map<Integer, Endpoint> exposed = new LinkedHashMap<>();
        for (MacroPortSource source : macro.portSources())
            exposed.put(source.macroPort(), childEndpoint(children, source.childInstance(), source.childPort()));
        return new Expansion(exposed);
    }

    private Expansion atomic(PieceDefinition definition, GridPoint origin, int turns) {
        int id = atomicTiles.size();
        atomicTiles.add(new PlacedTile(id, definition.id(), turns));
        expectedOrigins.put(id, origin);
        inventory = inventory.plus(definition.inventory());
        Map<Integer, Endpoint> ports = new LinkedHashMap<>();
        definition.ports().forEach(port -> ports.put(port.index(), new Endpoint(id, port.index())));
        return new Expansion(ports);
    }

    private Endpoint childEndpoint(Map<Integer, Expansion> children, int child, int port) {
        Expansion expansion = children.get(child);
        if (expansion == null) throw new IllegalStateException("Unknown expanded child " + child);
        Endpoint endpoint = expansion.exposedPorts.get(port);
        if (endpoint == null) throw new IllegalStateException("Unknown expanded child port " + child + ':' + port);
        return endpoint;
    }

    private Endpoint require(Map<TopPort, Endpoint> ports, TopPort port) {
        Endpoint endpoint = ports.get(port);
        if (endpoint == null) throw new IllegalArgumentException("Unknown composed board port " + port);
        return endpoint;
    }

    /** Rotates a child bounding rectangle inside its parent's unrotated bounding rectangle. */
    private GridPoint rotateRectangleOrigin(int parentWidth, int parentHeight, int x, int y,
                                            int childWidth, int childHeight, int turns) {
        return switch (turns) {
            case 0 -> new GridPoint(x, y);
            case 1 -> new GridPoint(parentHeight - y - childHeight, x);
            case 2 -> new GridPoint(parentWidth - x - childWidth, parentHeight - y - childHeight);
            case 3 -> new GridPoint(y, parentWidth - x - childWidth);
            default -> throw new IllegalStateException();
        };
    }

    private void verifyRelativeOrigins(Map<Integer, GridPoint> actual) {
        if (actual.size() != expectedOrigins.size())
            throw new IllegalStateException("Expanded layout lost atomic pieces");
        int root = atomicTiles.get(0).instanceId();
        GridPoint expectedRoot = expectedOrigins.get(root);
        GridPoint actualRoot = actual.get(root);
        int dx = actualRoot.x() - expectedRoot.x();
        int dy = actualRoot.y() - expectedRoot.y();
        expectedOrigins.forEach((id, expected) -> {
            GridPoint positioned = actual.get(id);
            if (positioned.x() != expected.x() + dx || positioned.y() != expected.y() + dy)
                throw new IllegalStateException("Expanded provenance disagrees with port geometry for atom " + id);
        });
    }
}
