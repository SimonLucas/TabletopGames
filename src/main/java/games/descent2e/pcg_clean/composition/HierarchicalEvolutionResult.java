package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.qd.QualityArchiveSnapshot;

import java.util.List;

public record HierarchicalEvolutionResult(QualityArchiveSnapshot<ExpandedBoardCandidate> boards,
                                          QualityArchiveSnapshot<MacroCandidate> macros,
                                          List<MacroCatalogueEntry> catalogue) {
    public HierarchicalEvolutionResult { catalogue = List.copyOf(catalogue); }
}
