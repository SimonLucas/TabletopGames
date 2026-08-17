package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.domain.PieceInventory;

import java.util.Objects;

public record MacroEvolutionConfig(int initializationAttempts, int iterations,
                                   int maximumAtomicPieces, long seed,
                                   InventoryPolicy inventoryPolicy,
                                   PieceInventory availableInventory) {
    public MacroEvolutionConfig {
        if (initializationAttempts < 1 || iterations < 0 || maximumAtomicPieces < 2)
            throw new IllegalArgumentException("Invalid macro evolution configuration");
        inventoryPolicy = Objects.requireNonNull(inventoryPolicy);
        availableInventory = Objects.requireNonNull(availableInventory);
    }
}
