package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.domain.BoardGenome;
import games.descent2e.pcg_clean.domain.PieceInventory;
import games.descent2e.pcg_clean.layout.BoardLayout;

/** Complete atomic view of a board whose genetic representation may contain macros. */
public record ExpandedBoard(BoardGenome genome, BoardLayout layout, PieceInventory inventory) {}
