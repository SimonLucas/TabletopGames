package games.descent2e.pcg_clean.ui;

import games.descent2e.pcg_clean.composition.*;
import games.descent2e.pcg_clean.data.*;
import games.descent2e.pcg_clean.domain.PieceInventory;

import javax.swing.SwingUtilities;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

/** Runnable complete-board demonstration using the co-evolving macro catalogue. */
public final class HierarchicalMacroBoardApplication {
    private HierarchicalMacroBoardApplication() {}

    public static void main(String[] args) throws IOException {
        Path tilePath = args.length == 0 ? Path.of("data/descent2e/tiles.json") : Path.of(args[0]);
        TileCatalog tiles = new TileCatalogLoader().load(tilePath);
        HierarchicalEvolutionConfig config = new HierarchicalEvolutionConfig(
                100, 2_000, 24, 8, 0.30, 0.65, 20260817L,
                InventoryPolicy.IGNORE, PieceInventory.empty());
        System.out.printf("Hierarchical macro run: seed=%d, iterations=%,d, board pieces<=%d, macro pieces<=%d%n",
                config.seed(), config.boardIterations(), config.maximumBoardAtomicPieces(),
                config.maximumCatalogueAtomicPieces());
        TileArtworkProvider artwork = new FileTileArtworkProvider(tilePath.resolveSibling("img").resolve("tiles"));
        ExpandedGraphStructureDescriptor boardDescriptor = new ExpandedGraphStructureDescriptor();
        MacroShapeDescriptor macroDescriptor = new MacroShapeDescriptor(6, 6);
        QualityArchiveHeatmapPanel<ExpandedBoardCandidate> boardHeatmap = new QualityArchiveHeatmapPanel<>(
                "Physical board MAP-Elites", boardDescriptor, boardRenderer(tiles, artwork));
        QualityArchiveHeatmapPanel<MacroCandidate> macroHeatmap = new QualityArchiveHeatmapPanel<>(
                "Macro catalogue MAP-Elites", macroDescriptor, macroRenderer(tiles, artwork));
        SwingUtilities.invokeLater(() -> {
            showHeatmapWindow("Hierarchical boards — physical branching × cycles", boardHeatmap, 20, 20);
            showHeatmapWindow("Macro catalogue — axial balance × compactness", macroHeatmap, 70, 70);
        });
        Thread evolution = new Thread(() -> runEvolution(tiles, config, boardHeatmap, macroHeatmap),
                "descent-hierarchical-map-elites");
        evolution.setDaemon(true);
        evolution.start();
    }

    private static void runEvolution(TileCatalog tiles, HierarchicalEvolutionConfig config,
                                     QualityArchiveHeatmapPanel<ExpandedBoardCandidate> boardHeatmap,
                                     QualityArchiveHeatmapPanel<MacroCandidate> macroHeatmap) {
        HierarchicalEvolutionListener listener = new HierarchicalEvolutionListener() {
            @Override public void boardArchiveChanged(games.descent2e.pcg_clean.qd.QualityArchiveSnapshot<ExpandedBoardCandidate> snapshot) {
                boardHeatmap.submit(snapshot);
            }
            @Override public void macroArchiveChanged(games.descent2e.pcg_clean.qd.QualityArchiveSnapshot<MacroCandidate> snapshot) {
                macroHeatmap.submit(snapshot);
            }
        };
        HierarchicalEvolutionResult result = new HierarchicalMacroBoardGenerator(tiles).generate(config, listener);
        long feasible = result.boards().elites().values().stream()
                .filter(candidate -> candidate.evaluation().feasible()).count();
        long uses = result.catalogue().stream().map(MacroCatalogueEntry::usage)
                .mapToLong(MacroUsageStatistics::successfulGrafts).sum();
        System.out.printf("Complete: %,d board evaluations, %d board niches (%d feasible), "
                        + "%d macro niches, %,d successful catalogue grafts%n",
                result.boards().evaluations(), result.boards().elites().size(), feasible,
                result.macros().elites().size(), uses);
        System.out.println("Active macro catalogue:");
        result.catalogue().forEach(entry -> {
            var definition = entry.candidate().definition();
            var usage = entry.usage();
            System.out.printf("  %-24s atoms=%d ports=%d quality=%.3f grafts=%d/%d boardAdmissions=%d feasible=%d%n",
                    definition.id(), definition.metrics().atomicPieces(), definition.metrics().exposedPorts(),
                    entry.candidate().quality(), usage.successfulGrafts(), usage.graftAttempts(),
                    usage.boardAdmissions(), usage.feasibleBoardAdmissions());
        });
    }

    private static QualityEliteRenderer<ExpandedBoardCandidate> boardRenderer(
            TileCatalog tiles, TileArtworkProvider artwork) {
        return new QualityEliteRenderer<>() {
            public double colourValue(ExpandedBoardCandidate candidate) {
                return candidate.evaluation().fitness() / (1.0 + candidate.evaluation().violationCount());
            }
            public String firstLine(ExpandedBoardCandidate candidate) {
                return "v=%d".formatted(candidate.evaluation().violationCount());
            }
            public String secondLine(ExpandedBoardCandidate candidate) {
                return "f=%.3f".formatted(candidate.evaluation().fitness());
            }
            public String tooltip(ExpandedBoardCandidate candidate) {
                return "%d physical pieces; %d violations; fitness %.6f; click to inspect".formatted(
                        candidate.evaluation().expanded().genome().tiles().size(),
                        candidate.evaluation().violationCount(), candidate.evaluation().fitness());
            }
            public void open(ExpandedBoardCandidate candidate) {
                BoardViewer.show("Physical board elite", java.util.List.of(BoardViewModel.from(candidate, tiles)), artwork);
            }
        };
    }

    private static QualityEliteRenderer<MacroCandidate> macroRenderer(
            TileCatalog tiles, TileArtworkProvider artwork) {
        return new QualityEliteRenderer<>() {
            public double colourValue(MacroCandidate candidate) {
                return candidate.quality() / (1.0 + candidate.violationCount());
            }
            public String firstLine(MacroCandidate candidate) {
                return "n=%d p=%d".formatted(candidate.definition().metrics().atomicPieces(),
                        candidate.definition().metrics().exposedPorts());
            }
            public String secondLine(MacroCandidate candidate) {
                return "q=%.3f".formatted(candidate.quality());
            }
            public String tooltip(MacroCandidate candidate) {
                var metrics = candidate.definition().metrics();
                return "%d atoms; %d exposed ports; balance %.3f; compactness %.3f; click to inspect".formatted(
                        metrics.atomicPieces(), metrics.exposedPorts(),
                        Math.min(metrics.width(), metrics.height()) / (double) Math.max(metrics.width(), metrics.height()),
                        metrics.compactness());
            }
            public void open(MacroCandidate candidate) {
                MacroCatalogueEntry entry = new MacroCatalogueEntry(candidate, MacroUsageStatistics.empty());
                BoardViewer.show("Macro catalogue elite", java.util.List.of(BoardViewModel.from(entry, tiles)), artwork);
            }
        };
    }

    private static void showHeatmapWindow(String title, JComponent panel, int offsetX, int offsetY) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setMinimumSize(new Dimension(700, 520));
        frame.setLocation(offsetX, offsetY);
        frame.setVisible(true);
    }
}
