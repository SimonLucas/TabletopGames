package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.domain.*;
import games.descent2e.pcg_clean.layout.ConnectorDepth;
import games.descent2e.pcg_clean.layout.PortGeometry;

import java.util.*;

/** Compiles positioned child definitions into one immutable, reusable piece definition. */
public final class MacroPieceComposer {
    private record PortRef(int instance, int port) {}
    private record Child(MacroPlacement placement, RotatedTile geometry, GridPoint origin) {}
    private record ExposedPort(Port port, int childInstance, int childPort) {}
    private record NormalizedPorts(List<Port> ports, List<MacroPortSource> sources) {}

    public MacroPieceDefinition compose(String id, List<MacroPlacement> placements,
                                        List<MacroConnection> connections) {
        if (placements.isEmpty()) throw new IllegalArgumentException("A macro needs at least one child");
        Map<Integer, Child> children = index(placements);
        Set<PortRef> consumed = validateConnections(children, connections);
        ensureConnected(children.keySet(), connections);

        Map<GridPoint, Cell> occupied = mergeCells(children.values());
        List<ExposedPort> exposed = exposedPorts(children.values(), consumed);
        Bounds bounds = bounds(occupied.keySet(), exposed);
        int width = bounds.maxX - bounds.minX + 1;
        int height = bounds.maxY - bounds.minY + 1;
        List<Cell> cells = denseCells(occupied, bounds, width, height);
        NormalizedPorts normalizedPorts = normalizePorts(exposed, bounds);
        List<Port> ports = normalizedPorts.ports();
        List<MacroPlacement> normalizedChildren = placements.stream().map(placement -> new MacroPlacement(
                placement.instanceId(), placement.definition(), placement.x() - bounds.minX,
                placement.y() - bounds.minY, placement.quarterTurns())).toList();
        PieceInventory inventory = placements.stream().map(placement -> placement.definition().inventory())
                .reduce(PieceInventory.empty(), PieceInventory::plus);
        int nestedBranches = placements.stream().filter(placement -> placement.definition() instanceof MacroPieceDefinition)
                .map(placement -> (MacroPieceDefinition) placement.definition())
                .mapToInt(macro -> macro.metrics().internalBranches()).sum();
        int nestedCycles = placements.stream().filter(placement -> placement.definition() instanceof MacroPieceDefinition)
                .map(placement -> (MacroPieceDefinition) placement.definition())
                .mapToInt(macro -> macro.metrics().internalCycles()).sum();
        int branches = nestedBranches + branchCount(children.keySet(), connections);
        int cycles = nestedCycles + connections.size() - children.size() + 1;
        RotatedTile geometry = new RotatedTile(id, width, height, cells, ports);
        PieceMetrics metrics = new PieceMetricCalculator().calculate(geometry, inventory, branches, cycles);
        String signature = canonicalSignature(geometry, inventory);
        return new MacroPieceDefinition(id, width, height, cells, ports, normalizedChildren,
                connections, normalizedPorts.sources(), inventory, metrics, signature);
    }

    private Map<Integer, Child> index(List<MacroPlacement> placements) {
        Map<Integer, Child> result = new LinkedHashMap<>();
        for (MacroPlacement placement : placements) {
            Child child = new Child(placement, placement.definition().rotate(placement.quarterTurns()),
                    new GridPoint(placement.x(), placement.y()));
            if (result.putIfAbsent(placement.instanceId(), child) != null)
                throw new IllegalArgumentException("Duplicate macro child " + placement.instanceId());
        }
        return result;
    }

    private Set<PortRef> validateConnections(Map<Integer, Child> children,
                                             List<MacroConnection> connections) {
        Set<PortRef> consumed = new HashSet<>();
        for (MacroConnection connection : connections) {
            Child first = require(children, connection.firstInstance());
            Child second = require(children, connection.secondInstance());
            Port a = first.geometry.port(connection.firstPort());
            Port b = second.geometry.port(connection.secondPort());
            if (!PortGeometry.aligned(a, first.origin, b, second.origin, ConnectorDepth.ZERO))
                throw new IllegalArgumentException("Misaligned macro connection " + connection);
            if (!consumed.add(new PortRef(connection.firstInstance(), connection.firstPort()))
                    || !consumed.add(new PortRef(connection.secondInstance(), connection.secondPort())))
                throw new IllegalArgumentException("A macro port is used more than once");
        }
        return consumed;
    }

    private Child require(Map<Integer, Child> children, int id) {
        Child child = children.get(id);
        if (child == null) throw new IllegalArgumentException("Unknown macro child " + id);
        return child;
    }

    private void ensureConnected(Set<Integer> ids, List<MacroConnection> connections) {
        Map<Integer, Set<Integer>> graph = new LinkedHashMap<>();
        ids.forEach(id -> graph.put(id, new LinkedHashSet<>()));
        connections.forEach(connection -> {
            if (!graph.containsKey(connection.firstInstance()) || !graph.containsKey(connection.secondInstance()))
                throw new IllegalArgumentException("Connection references an unknown macro child");
            graph.get(connection.firstInstance()).add(connection.secondInstance());
            graph.get(connection.secondInstance()).add(connection.firstInstance());
        });
        Set<Integer> reached = new HashSet<>();
        visit(ids.iterator().next(), graph, reached);
        if (reached.size() != ids.size()) throw new IllegalArgumentException("Macro composition is disconnected");
    }

    private void visit(int id, Map<Integer, Set<Integer>> graph, Set<Integer> reached) {
        if (!reached.add(id)) return;
        graph.getOrDefault(id, Set.of()).forEach(next -> visit(next, graph, reached));
    }

    private Map<GridPoint, Cell> mergeCells(Collection<Child> children) {
        Map<GridPoint, Cell> result = new LinkedHashMap<>();
        for (Child child : children) for (int y = 0; y < child.geometry.height(); y++)
            for (int x = 0; x < child.geometry.width(); x++) {
                Cell cell = child.geometry.cellAt(x, y);
                if (cell == Cell.VOID || cell == Cell.OPEN) continue;
                GridPoint point = child.origin.plus(new GridPoint(x, y));
                if (result.putIfAbsent(point, cell) != null)
                    throw new IllegalArgumentException("Macro children overlap at " + point);
            }
        if (result.isEmpty()) throw new IllegalArgumentException("Macro has no occupied cells");
        return result;
    }

    private List<ExposedPort> exposedPorts(Collection<Child> children, Set<PortRef> consumed) {
        List<ExposedPort> result = new ArrayList<>();
        for (Child child : children) for (Port port : child.geometry.ports()) {
            if (consumed.contains(new PortRef(child.placement.instanceId(), port.index()))) continue;
            result.add(new ExposedPort(new Port(-1, port.direction(),
                    port.cells().stream().map(child.origin::plus).toList()),
                    child.placement.instanceId(), port.index()));
        }
        return result;
    }

    private record Bounds(int minX, int minY, int maxX, int maxY) {}

    private Bounds bounds(Set<GridPoint> occupied, List<ExposedPort> ports) {
        List<GridPoint> all = new ArrayList<>(occupied);
        ports.forEach(port -> all.addAll(port.port().cells()));
        return new Bounds(all.stream().mapToInt(GridPoint::x).min().orElseThrow(),
                all.stream().mapToInt(GridPoint::y).min().orElseThrow(),
                all.stream().mapToInt(GridPoint::x).max().orElseThrow(),
                all.stream().mapToInt(GridPoint::y).max().orElseThrow());
    }

    private List<Cell> denseCells(Map<GridPoint, Cell> occupied, Bounds bounds, int width, int height) {
        List<Cell> cells = new ArrayList<>(Collections.nCopies(width * height, Cell.VOID));
        occupied.forEach((point, cell) -> {
            int x = point.x() - bounds.minX;
            int y = point.y() - bounds.minY;
            cells.set(y * width + x, cell);
        });
        return cells;
    }

    private NormalizedPorts normalizePorts(List<ExposedPort> ports, Bounds bounds) {
        Comparator<ExposedPort> order = Comparator
                .comparingInt((ExposedPort exposed) -> exposed.port().direction().ordinal())
                .thenComparingInt(exposed -> exposed.port().cells().stream().mapToInt(GridPoint::y).min().orElse(0))
                .thenComparingInt(exposed -> exposed.port().cells().stream().mapToInt(GridPoint::x).min().orElse(0))
                .thenComparingInt(ExposedPort::childInstance).thenComparingInt(ExposedPort::childPort);
        List<ExposedPort> sorted = ports.stream().map(exposed -> new ExposedPort(
                        new Port(-1, exposed.port().direction(), exposed.port().cells().stream()
                                .map(point -> new GridPoint(point.x() - bounds.minX, point.y() - bounds.minY)).toList()),
                        exposed.childInstance(), exposed.childPort()))
                .sorted(order).toList();
        List<Port> normalized = new ArrayList<>();
        List<MacroPortSource> sources = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            ExposedPort exposed = sorted.get(i);
            normalized.add(new Port(i, exposed.port().direction(), exposed.port().cells()));
            sources.add(new MacroPortSource(i, exposed.childInstance(), exposed.childPort()));
        }
        return new NormalizedPorts(normalized, sources);
    }

    private int branchCount(Set<Integer> ids, List<MacroConnection> connections) {
        Map<Integer, Integer> degrees = new HashMap<>();
        ids.forEach(id -> degrees.put(id, 0));
        connections.forEach(connection -> {
            degrees.merge(connection.firstInstance(), 1, Integer::sum);
            degrees.merge(connection.secondInstance(), 1, Integer::sum);
        });
        return (int) degrees.values().stream().filter(degree -> degree >= 3).count();
    }

    private String canonicalSignature(RotatedTile geometry, PieceInventory inventory) {
        List<String> rotations = new ArrayList<>();
        TileDefinition temporary = new TileDefinition("signature", geometry.width(), geometry.height(),
                geometry.cells(), geometry.ports());
        for (int turns = 0; turns < 4; turns++) rotations.add(signature(temporary.rotate(turns), inventory));
        return rotations.stream().min(String::compareTo).orElseThrow();
    }

    private String signature(RotatedTile geometry, PieceInventory inventory) {
        StringBuilder result = new StringBuilder().append(geometry.width()).append('x')
                .append(geometry.height()).append(':');
        geometry.cells().forEach(cell -> result.append((char) ('A' + cell.ordinal())));
        result.append(':');
        geometry.ports().stream().map(port -> new Port(port.index(), port.direction(), port.cells().stream()
                        .sorted(Comparator.comparingInt(GridPoint::y).thenComparingInt(GridPoint::x)).toList()))
                .sorted(Comparator.comparingInt((Port port) -> port.direction().ordinal())
                        .thenComparing(port -> port.cells().toString()))
                .forEach(port -> result.append(port.direction().ordinal()).append(port.cells()).append(';'));
        return result.append(':').append(inventory.quantities()).toString();
    }
}
