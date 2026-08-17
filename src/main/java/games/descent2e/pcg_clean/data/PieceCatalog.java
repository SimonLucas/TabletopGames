package games.descent2e.pcg_clean.data;

import games.descent2e.pcg_clean.domain.PieceDefinition;

/** Definition lookup shared by atomic-only and generated macro libraries. */
public interface PieceCatalog {
    PieceDefinition require(String id);
}
