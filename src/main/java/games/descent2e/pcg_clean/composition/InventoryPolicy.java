package games.descent2e.pcg_clean.composition;

/** How physical component availability affects a composed piece or complete board. */
public enum InventoryPolicy {
    IGNORE,
    HARD_LIMIT,
    SOFT_PENALTY
}
