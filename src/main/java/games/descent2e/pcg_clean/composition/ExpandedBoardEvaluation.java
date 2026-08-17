package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.evaluation.Evaluation;

/** Atomic board evaluation plus independently configurable inventory accounting. */
public record ExpandedBoardEvaluation(ExpandedBoard expanded, Evaluation physicalEvaluation,
                                      InventoryAssessment inventoryAssessment,
                                      InventoryPolicy inventoryPolicy) {
    public boolean feasible() {
        return physicalEvaluation.feasible()
                && (inventoryPolicy != InventoryPolicy.HARD_LIMIT || inventoryAssessment.withinLimits());
    }

    public int violationCount() {
        int physical = physicalEvaluation.constraints().stream()
                .mapToInt(result -> result.satisfied() ? 0 : Math.max(1, result.violations().size())).sum();
        return physical + (inventoryPolicy == InventoryPolicy.HARD_LIMIT ? inventoryAssessment.totalExcess() : 0);
    }

    public double fitness() {
        return inventoryPolicy == InventoryPolicy.SOFT_PENALTY
                ? physicalEvaluation.fitness() * inventoryAssessment.score()
                : physicalEvaluation.fitness();
    }
}
