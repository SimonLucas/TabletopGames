package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.domain.Cell;
import games.descent2e.pcg_clean.evaluation.Constraint;
import games.descent2e.pcg_clean.evaluation.FitnessCriterion;
import games.descent2e.pcg_clean.evaluation.constraints.*;
import games.descent2e.pcg_clean.evaluation.criteria.*;

import java.util.*;

/** Composition root for complete hierarchical macro-board MAP-Elites runs. */
public final class HierarchicalMacroBoardGenerator {
    private final TileCatalog tiles;

    public HierarchicalMacroBoardGenerator(TileCatalog tiles) { this.tiles = Objects.requireNonNull(tiles); }

    public HierarchicalEvolutionResult generate(HierarchicalEvolutionConfig config) {
        return generate(config, HierarchicalEvolutionListener.none());
    }

    public HierarchicalEvolutionResult generate(HierarchicalEvolutionConfig config,
                                                HierarchicalEvolutionListener listener) {
        List<Constraint> constraints = List.of(new SuccessfulLayoutConstraint(),
                new ConnectedPieceGraphConstraint(), new RequiredTileRoleConstraint("entrance", 1),
                new RequiredTileRoleConstraint("exit", 1), new BoardExtentConstraint(48, 48));
        List<FitnessCriterion> criteria = List.of(new TargetCellCountCriterion(166, 2.0),
                new CompactnessCriterion(1.0), new GraphBranchingCriterion(0.15, 1.0),
                new GraphCycleCriterion(1, 1.5), new EntranceExitDistanceCriterion(7, 1.5),
                new TerrainCompositionCriterion(Map.of(
                        Cell.WATER, 0.12, Cell.PIT, 0.04, Cell.BLOCK, 0.04), 1.5));
        MacroPieceLibrary catalogue = new MacroPieceLibrary(new MacroShapeDescriptor(6, 6));
        return new HierarchicalMacroEvolutionEngine(tiles, config, catalogue,
                new ExpandedGraphStructureDescriptor(), constraints, criteria, listener).run();
    }
}
