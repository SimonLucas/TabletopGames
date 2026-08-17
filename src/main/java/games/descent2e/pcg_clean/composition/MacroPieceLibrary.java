package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.qd.*;

import java.util.*;

/** Deduplicated generated-definition store backed by a configurable QD archive. */
public final class MacroPieceLibrary {
    private final QualityArchive<MacroCandidate> archive;
    private final Map<String, MacroUsageStatistics> usage = new LinkedHashMap<>();

    public MacroPieceLibrary(QualityDescriptor<MacroCandidate> descriptor) {
        archive = new QualityArchive<>(descriptor, Comparator.comparingInt(MacroCandidate::violationCount)
                .thenComparing(Comparator.comparingInt((MacroCandidate candidate) ->
                        candidate.definition().metrics().internalCycles()).reversed())
                .thenComparing(Comparator.comparingDouble(MacroCandidate::quality).reversed())
                .thenComparingLong(MacroCandidate::id));
    }

    public synchronized boolean offer(MacroCandidate candidate) {
        String signature = candidate.definition().canonicalSignature();
        if (archive.snapshot().elites().values().stream()
                .anyMatch(elite -> elite.definition().canonicalSignature().equals(signature))) return false;
        boolean admitted = archive.offer(candidate);
        if (admitted) synchronizeUsage();
        return admitted;
    }

    public synchronized List<MacroPieceDefinition> definitions() {
        return archive.snapshot().elites().values().stream().map(MacroCandidate::definition).toList();
    }

    public synchronized void recordGraft(MacroPieceDefinition definition, boolean successful) {
        updateUsage(definition, statistics -> statistics.graft(successful));
    }

    public synchronized void recordBoardAdmission(MacroPieceDefinition definition,
                                                   boolean feasible, double fitness) {
        updateUsage(definition, statistics -> statistics.admitted(feasible, fitness));
    }

    public synchronized List<MacroCatalogueEntry> entries() {
        return archive.snapshot().elites().values().stream().map(candidate -> new MacroCatalogueEntry(candidate,
                usage.getOrDefault(candidate.definition().canonicalSignature(), MacroUsageStatistics.empty()))).toList();
    }

    private void updateUsage(MacroPieceDefinition definition,
                             java.util.function.UnaryOperator<MacroUsageStatistics> update) {
        String signature = definition.canonicalSignature();
        if (usage.containsKey(signature)) usage.put(signature, update.apply(usage.get(signature)));
    }

    private void synchronizeUsage() {
        Set<String> active = archive.snapshot().elites().values().stream()
                .map(candidate -> candidate.definition().canonicalSignature()).collect(java.util.stream.Collectors.toSet());
        usage.keySet().retainAll(active);
        active.forEach(signature -> usage.putIfAbsent(signature, MacroUsageStatistics.empty()));
    }

    public QualityArchiveSnapshot<MacroCandidate> snapshot() { return archive.snapshot(); }
}
