package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.domain.PieceDefinition;
import games.descent2e.pcg_clean.qd.QualityArchiveSnapshot;

import java.util.*;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/** Bounded deterministic QD search that grows reusable definitions through valid port grafts. */
public final class MacroEvolutionEngine {
    private final List<PieceDefinition> seeds;
    private final MacroEvolutionConfig config;
    private final MacroPieceLibrary library;
    private final MacroGraftOperator graft = new MacroGraftOperator();
    private final MacroQualityModel quality = new MacroQualityModel();
    private final RandomGenerator random;
    private long nextId;

    public MacroEvolutionEngine(Collection<? extends PieceDefinition> seeds, MacroEvolutionConfig config,
                                MacroPieceLibrary library) {
        this.seeds = seeds.stream().sorted(Comparator.comparing(PieceDefinition::id)).map(value -> (PieceDefinition) value)
                .toList();
        if (this.seeds.size() < 2) throw new IllegalArgumentException("Macro evolution needs at least two seeds");
        this.config = Objects.requireNonNull(config);
        this.library = Objects.requireNonNull(library);
        random = RandomGeneratorFactory.of("L64X128MixRandom").create(config.seed());
    }

    public QualityArchiveSnapshot<MacroCandidate> run() {
        for (int i = 0; i < config.initializationAttempts(); i++)
            emit(pick(seeds), pick(seeds));
        for (int i = 0; i < config.iterations(); i++) {
            List<MacroPieceDefinition> parents = library.definitions();
            PieceDefinition first = parents.isEmpty() ? pick(seeds) : pick(parents);
            PieceDefinition second = random.nextDouble() < 0.70 ? pick(seeds)
                    : parents.isEmpty() ? pick(seeds) : pick(parents);
            emit(first, second);
        }
        return library.snapshot();
    }

    private void emit(PieceDefinition first, PieceDefinition second) {
        List<MacroPieceDefinition> children = graft.graft("macro-" + nextId, first, second).stream()
                .filter(child -> child.inventory().totalPieces() <= config.maximumAtomicPieces())
                .filter(this::allowedByInventory).toList();
        if (children.isEmpty()) return;
        MacroPieceDefinition child = children.get(random.nextInt(children.size()));
        InventoryAssessment assessment = InventoryAssessment.compare(child.inventory(), config.availableInventory());
        int violations = config.inventoryPolicy() == InventoryPolicy.HARD_LIMIT ? assessment.totalExcess() : 0;
        double score = quality.score(child);
        if (config.inventoryPolicy() == InventoryPolicy.SOFT_PENALTY) score *= assessment.score();
        library.offer(new MacroCandidate(nextId++, child, violations, score));
    }

    private boolean allowedByInventory(MacroPieceDefinition child) {
        return config.inventoryPolicy() != InventoryPolicy.HARD_LIMIT
                || InventoryAssessment.compare(child.inventory(), config.availableInventory()).withinLimits();
    }

    private <T> T pick(List<T> values) { return values.get(random.nextInt(values.size())); }
}
