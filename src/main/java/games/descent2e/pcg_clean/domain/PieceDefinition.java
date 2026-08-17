package games.descent2e.pcg_clean.domain;

/** A reusable, variable-sized piece definition, atomic or dynamically composed. */
public interface PieceDefinition extends PieceGeometry {
    String id();
    RotatedTile rotate(int quarterTurns);
    PieceInventory inventory();
}
