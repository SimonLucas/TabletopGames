package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.data.*;
import games.descent2e.pcg_clean.domain.*;
import games.descent2e.pcg_clean.evaluation.*;
import games.descent2e.pcg_clean.qd.*;

import java.util.*;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/** Co-evolves a bounded macro catalogue and complete, atomically evaluated macro boards. */
public final class HierarchicalMacroEvolutionEngine {
    private static final int CATALOGUE_PAIR_PROPOSALS = 8;
    private static final int NATURAL_LOOP_PAIR_BUDGET = 1024;
    private final TileCatalog atomic;
    private final List<PieceDefinition> regularSeeds;
    private final PieceDefinition entrance;
    private final PieceDefinition exit;
    private final HierarchicalEvolutionConfig config;
    private final MacroPieceLibrary catalogue;
    private final MacroGraftOperator graft = new MacroGraftOperator();
    private final MacroQualityModel macroQuality = new MacroQualityModel();
    private final List<Constraint> constraints;
    private final List<FitnessCriterion> criteria;
    private final QualityArchive<ExpandedBoardCandidate> boards;
    private final RandomGenerator random;
    private final HierarchicalEvolutionListener listener;
    private long nextMacroId;
    private long nextBoardId;

    public HierarchicalMacroEvolutionEngine(TileCatalog atomic, HierarchicalEvolutionConfig config,
                                            MacroPieceLibrary catalogue,
                                            QualityDescriptor<ExpandedBoardCandidate> boardDescriptor,
                                            List<Constraint> constraints, List<FitnessCriterion> criteria) {
        this(atomic, config, catalogue, boardDescriptor, constraints, criteria,
                HierarchicalEvolutionListener.none());
    }

    public HierarchicalMacroEvolutionEngine(TileCatalog atomic, HierarchicalEvolutionConfig config,
                                            MacroPieceLibrary catalogue,
                                            QualityDescriptor<ExpandedBoardCandidate> boardDescriptor,
                                            List<Constraint> constraints, List<FitnessCriterion> criteria,
                                            HierarchicalEvolutionListener listener) {
        this.atomic = Objects.requireNonNull(atomic);
        this.config = Objects.requireNonNull(config);
        this.catalogue = Objects.requireNonNull(catalogue);
        this.constraints = List.copyOf(constraints);
        this.criteria = List.copyOf(criteria);
        this.listener = Objects.requireNonNull(listener);
        entrance = role("entrance");
        exit = role("exit");
        regularSeeds = atomic.all().stream().filter(tile -> !isRole(tile.id())).map(tile -> (PieceDefinition) tile).toList();
        if (regularSeeds.isEmpty()) throw new IllegalArgumentException("No regular atomic seeds");
        boards = new QualityArchive<>(boardDescriptor,
                Comparator.comparingInt((ExpandedBoardCandidate candidate) -> candidate.evaluation().violationCount())
                        .thenComparing(Comparator.comparingDouble(
                                (ExpandedBoardCandidate candidate) -> candidate.evaluation().fitness()).reversed())
                        .thenComparingLong(ExpandedBoardCandidate::id));
        random = RandomGeneratorFactory.of("L64X128MixRandom").create(config.seed());
    }

    public HierarchicalEvolutionResult run() {
        for (int i = 0; i < config.catalogueBootstrapAttempts(); i++) evolveCatalogue();
        bootstrapBoards();
        for (int i = 0; i < config.boardIterations(); i++) {
            if (random.nextDouble() < config.catalogueEvolutionProbability()) evolveCatalogue();
            emitBoard();
        }
        listener.boardArchiveChanged(boards.snapshot());
        listener.macroArchiveChanged(catalogue.snapshot());
        return new HierarchicalEvolutionResult(boards.snapshot(), catalogue.snapshot(), catalogue.entries());
    }

    private void bootstrapBoards() {
        List<PieceDefinition> modules = new ArrayList<>(regularSeeds.stream()
                .filter(seed -> seed.ports().size() >= 3).toList());
        modules.addAll(catalogue.definitions().stream()
                .filter(macro -> macro.ports().size() >= 3).toList());
        if (modules.isEmpty()) throw new IllegalStateException("No extensible module with at least three ports");
        int smallestModule = modules.stream().mapToInt(module -> module.inventory().totalPieces()).min().orElseThrow();
        List<PieceDefinition> bootstrapModules = modules.stream()
                .filter(module -> module.inventory().totalPieces() == smallestModule).toList();
        int attempts = Math.max(12, config.catalogueBootstrapAttempts());
        for (int i = 0; i < attempts; i++) {
            PieceDefinition module = pick(bootstrapModules);
            List<MacroPieceDefinition> entranceModules = graft.graft("board-entry-" + i, module, entrance);
            if (entranceModules.isEmpty()) continue;
            MacroPieceDefinition entranceModule = pick(entranceModules);
            List<MacroPieceDefinition> starts = graft.graft("board-bootstrap-" + i, entranceModule, exit).stream()
                    .filter(this::withinBoardSize).filter(start -> !start.ports().isEmpty()).toList();
            if (!starts.isEmpty()) offerBoard(pick(starts),
                    module instanceof MacroPieceDefinition macro ? macro : null);
        }
        if (boards.snapshot().elites().isEmpty())
            throw new IllegalStateException("No initial macro board entered the archive");
    }

    private void emitBoard() {
        PieceDefinition donor = chooseDonor();
        List<ExpandedBoardCandidate> parents = new ArrayList<>(boards.snapshot().elites().values().stream()
                .filter(parent -> parent.evaluation().expanded().inventory().totalPieces()
                        + donor.inventory().totalPieces() <= config.maximumBoardAtomicPieces()).toList());
        if (parents.isEmpty()) return;
        Collections.shuffle(parents, new Random(random.nextLong()));
        for (ExpandedBoardCandidate parent : parents) {
            List<MacroPieceDefinition> children = graft.graft("board-" + nextBoardId,
                            parent.rootDefinition(), donor).stream().filter(this::withinBoardSize).toList();
            if (children.isEmpty()) continue;
            if (donor instanceof MacroPieceDefinition macro) catalogue.recordGraft(macro, true);
            offerBoard(preferNaturalLoops(children), donor instanceof MacroPieceDefinition macro ? macro : null);
            return;
        }
        if (donor instanceof MacroPieceDefinition macro) catalogue.recordGraft(macro, false);
    }

    private PieceDefinition chooseDonor() {
        List<MacroPieceDefinition> macros = catalogue.definitions().stream()
                .filter(macro -> !macro.ports().isEmpty()).toList();
        List<MacroPieceDefinition> cyclic = macros.stream()
                .filter(macro -> macro.metrics().internalCycles() > 0).toList();
        boolean boardArchiveHasCycle = boards.snapshot().elites().values().stream()
                .anyMatch(board -> board.rootDefinition().metrics().internalCycles() > 0);
        if (!boardArchiveHasCycle && !cyclic.isEmpty()) return pick(cyclic);
        if (!cyclic.isEmpty() && random.nextDouble() < 0.35) return pick(cyclic);
        return !macros.isEmpty() && random.nextDouble() < config.catalogueDonorProbability()
                ? pick(macros) : pick(regularSeeds);
    }

    private void evolveCatalogue() {
        List<MacroPieceDefinition> macros = catalogue.definitions();
        Optional<MacroPieceDefinition> naturalLoop = discoverNaturalLoop(macros);
        if (naturalLoop.isPresent()) {
            offerMacro(naturalLoop.get());
            return;
        }
        Map<String, MacroPieceDefinition> distinct = new LinkedHashMap<>();
        for (int proposal = 0; proposal < CATALOGUE_PAIR_PROPOSALS; proposal++) {
            PieceDefinition first = macros.isEmpty() || random.nextBoolean() ? pick(regularSeeds) : pick(macros);
            PieceDefinition second = macros.isEmpty() || random.nextDouble() < 0.70 ? pick(regularSeeds) : pick(macros);
            graft.graft("catalogue-" + nextMacroId + '-' + proposal, first, second).stream()
                    .filter(child -> child.inventory().totalPieces() <= config.maximumCatalogueAtomicPieces())
                    .forEach(child -> distinct.putIfAbsent(child.canonicalSignature(), child));
        }
        List<MacroPieceDefinition> children = List.copyOf(distinct.values());
        if (children.isEmpty()) return;
        offerMacro(preferNaturalLoops(children));
    }

    private void offerMacro(MacroPieceDefinition child) {
        InventoryAssessment assessment = InventoryAssessment.compare(child.inventory(), config.availableInventory());
        int violations = config.inventoryPolicy() == InventoryPolicy.HARD_LIMIT ? assessment.totalExcess() : 0;
        double quality = macroQuality.score(child);
        if (config.inventoryPolicy() == InventoryPolicy.SOFT_PENALTY) quality *= assessment.score();
        if (catalogue.offer(new MacroCandidate(nextMacroId++, child, violations, quality)))
            listener.macroArchiveChanged(catalogue.snapshot());
    }

    /** Bounded pairing search using ordinary graft placement, active only until a usable loop exists. */
    private Optional<MacroPieceDefinition> discoverNaturalLoop(List<MacroPieceDefinition> macros) {
        if (macros.stream().anyMatch(macro -> macro.metrics().internalCycles() > 0 && !macro.ports().isEmpty()))
            return Optional.empty();
        int pairs = 0;
        List<MacroPieceDefinition> orderedMacros = macros.stream()
                .filter(macro -> macro.ports().size() >= 2).sorted(Comparator.comparing(MacroPieceDefinition::id)).toList();
        List<PieceDefinition> partners = new ArrayList<>(regularSeeds.stream()
                .filter(seed -> seed.ports().size() >= 2)
                .sorted(Comparator.comparing(PieceDefinition::id)).toList());
        partners.addAll(orderedMacros);
        for (MacroPieceDefinition macro : orderedMacros) for (PieceDefinition partner : partners) {
            if (pairs++ >= NATURAL_LOOP_PAIR_BUDGET) return Optional.empty();
            int nestedCycles = macro.metrics().internalCycles()
                    + (partner instanceof MacroPieceDefinition nested ? nested.metrics().internalCycles() : 0);
            Optional<MacroPieceDefinition> loop = graft.graft("natural-loop-" + nextMacroId, macro, partner).stream()
                    .filter(child -> child.inventory().totalPieces() <= config.maximumCatalogueAtomicPieces())
                    .filter(child -> child.metrics().internalCycles() > nestedCycles)
                    .filter(child -> !child.ports().isEmpty())
                    .max(Comparator.comparingInt(child -> child.metrics().exposedPorts()));
            if (loop.isPresent()) return loop;
        }
        return Optional.empty();
    }

    private void offerBoard(MacroPieceDefinition root, MacroPieceDefinition catalogueDonor) {
        CompositePieceCatalog definitions = new CompositePieceCatalog(atomic, List.of(root));
        BoardGenome genome = new BoardGenome(List.of(new PlacedTile(0, root.id(), 0)), List.of());
        ExpandedBoardEvaluation evaluation;
        try {
            evaluation = new ExpandedBoardEvaluator(definitions, atomic, constraints, criteria,
                    config.inventoryPolicy(), config.availableInventory()).evaluate(genome);
        } catch (IllegalArgumentException | IllegalStateException invalid) {
            return;
        }
        ExpandedBoardCandidate candidate = new ExpandedBoardCandidate(nextBoardId++, genome, evaluation, root);
        boolean admitted = boards.offer(candidate);
        if (admitted) listener.boardArchiveChanged(boards.snapshot());
        if (admitted && catalogueDonor != null)
            catalogue.recordBoardAdmission(catalogueDonor, evaluation.feasible(), evaluation.fitness());
    }

    private boolean withinBoardSize(MacroPieceDefinition definition) {
        return definition.inventory().totalPieces() <= config.maximumBoardAtomicPieces();
    }

    /** Do not discard a rare incidental loop after the ordinary anchored graft has discovered it. */
    private MacroPieceDefinition preferNaturalLoops(List<MacroPieceDefinition> children) {
        int mostCycles = children.stream().mapToInt(child -> child.metrics().internalCycles()).max().orElse(0);
        List<MacroPieceDefinition> bestTopology = children.stream()
                .filter(child -> child.metrics().internalCycles() == mostCycles).toList();
        return pick(bestTopology);
    }

    private PieceDefinition role(String role) {
        return atomic.all().stream().filter(tile -> tile.id().toLowerCase(Locale.ROOT).startsWith(role))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Missing " + role + " definition"));
    }

    private boolean isRole(String id) {
        String lower = id.toLowerCase(Locale.ROOT);
        return lower.startsWith("entrance") || lower.startsWith("exit");
    }

    private <T> T pick(List<T> values) { return values.get(random.nextInt(values.size())); }
}
