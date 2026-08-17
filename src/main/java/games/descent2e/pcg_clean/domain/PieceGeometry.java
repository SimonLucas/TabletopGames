package games.descent2e.pcg_clean.domain;

import java.util.List;

/** Geometry shared by atomic tiles, compiled macros and their rotations. */
public interface PieceGeometry {
    int width();
    int height();
    List<Cell> cells();
    List<Port> ports();

    default Cell cellAt(int x, int y) { return cells().get(y * width() + x); }

    default Port port(int index) {
        return ports().stream().filter(port -> port.index() == index).findFirst().orElseThrow();
    }
}
