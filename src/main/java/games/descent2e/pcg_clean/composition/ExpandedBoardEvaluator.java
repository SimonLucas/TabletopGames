package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.data.*;
import games.descent2e.pcg_clean.domain.*;
import games.descent2e.pcg_clean.evaluation.*;

import java.util.*;

/** Evaluates macro genomes on their flattened atomic cells and physical piece graph. */
public final class ExpandedBoardEvaluator {
    private final MacroBoardExpander expander;
    private final BoardEvaluator physicalEvaluator;
    private final TileCatalog atomicTiles;
    private final InventoryPolicy inventoryPolicy;
    private final PieceInventory availableInventory;

    public ExpandedBoardEvaluator(PieceCatalog definitions, TileCatalog atomicTiles,
                                  List<Constraint> constraints, List<FitnessCriterion> criteria,
                                  InventoryPolicy inventoryPolicy, PieceInventory availableInventory) {
        expander = new MacroBoardExpander(definitions);
        this.atomicTiles = atomicTiles;
        physicalEvaluator = new BoardEvaluator(atomicTiles, constraints, criteria);
        this.inventoryPolicy = Objects.requireNonNull(inventoryPolicy);
        this.availableInventory = Objects.requireNonNull(availableInventory);
    }

    public ExpandedBoardEvaluation evaluate(BoardGenome genome) {
        ExpandedBoard expanded = expander.expand(genome);
        Evaluation physical = physicalEvaluator.evaluate(new EvaluationContext(
                expanded.genome(), expanded.layout(), atomicTiles));
        InventoryAssessment inventory = inventoryPolicy == InventoryPolicy.IGNORE
                ? new InventoryAssessment(Map.of(), 0, 0)
                : InventoryAssessment.compare(expanded.inventory(), availableInventory);
        return new ExpandedBoardEvaluation(expanded, physical, inventory, inventoryPolicy);
    }

}
