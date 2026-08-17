package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.qd.*;

import java.util.*;

/** Deduplicated generated-definition store backed by a configurable QD archive. */
public final class MacroPieceLibrary {
    private final QualityArchive<MacroCandidate> archive;

    public MacroPieceLibrary(QualityDescriptor<MacroCandidate> descriptor) {
        archive = new QualityArchive<>(descriptor, Comparator.comparingInt(MacroCandidate::violationCount)
                .thenComparing(Comparator.comparingDouble(MacroCandidate::quality).reversed())
                .thenComparingLong(MacroCandidate::id));
    }

    public synchronized boolean offer(MacroCandidate candidate) {
        String signature = candidate.definition().canonicalSignature();
        if (archive.snapshot().elites().values().stream()
                .anyMatch(elite -> elite.definition().canonicalSignature().equals(signature))) return false;
        return archive.offer(candidate);
    }

    public synchronized List<MacroPieceDefinition> definitions() {
        return archive.snapshot().elites().values().stream().map(MacroCandidate::definition).toList();
    }
    public QualityArchiveSnapshot<MacroCandidate> snapshot() { return archive.snapshot(); }
}
