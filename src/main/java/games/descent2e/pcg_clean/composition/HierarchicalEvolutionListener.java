package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.qd.QualityArchiveSnapshot;

public interface HierarchicalEvolutionListener {
    void boardArchiveChanged(QualityArchiveSnapshot<ExpandedBoardCandidate> snapshot);
    void macroArchiveChanged(QualityArchiveSnapshot<MacroCandidate> snapshot);

    static HierarchicalEvolutionListener none() {
        return new HierarchicalEvolutionListener() {
            @Override public void boardArchiveChanged(QualityArchiveSnapshot<ExpandedBoardCandidate> snapshot) {}
            @Override public void macroArchiveChanged(QualityArchiveSnapshot<MacroCandidate> snapshot) {}
        };
    }
}
