package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.domain.PieceInventory;

import java.util.Objects;

public record HierarchicalEvolutionConfig(int catalogueBootstrapAttempts, int boardIterations,
                                          int maximumBoardAtomicPieces, int maximumCatalogueAtomicPieces,
                                          double catalogueEvolutionProbability,
                                          double catalogueDonorProbability, long seed,
                                          InventoryPolicy inventoryPolicy,
                                          PieceInventory availableInventory) {
    public HierarchicalEvolutionConfig {
        if (catalogueBootstrapAttempts < 1 || boardIterations < 0 || maximumBoardAtomicPieces < 2
                || maximumCatalogueAtomicPieces < 2)
            throw new IllegalArgumentException("Invalid hierarchical evolution sizes");
        if (catalogueEvolutionProbability < 0 || catalogueEvolutionProbability > 1
                || catalogueDonorProbability < 0 || catalogueDonorProbability > 1)
            throw new IllegalArgumentException("Probabilities must be in [0,1]");
        inventoryPolicy = Objects.requireNonNull(inventoryPolicy);
        availableInventory = Objects.requireNonNull(availableInventory);
    }
}
