package games.descent2e.pcg_clean.layout;

/** Whether JSON OPEN connector cells occupy physical board space. */
public enum ConnectorDepth {
    /** Physical Descent layout: ports are zero-depth interfaces between adjacent terrain cells. */
    ZERO,
    /** Diagnostic compatibility mode: a connected port occupies one complete grid cell. */
    FULL_CELL
}
