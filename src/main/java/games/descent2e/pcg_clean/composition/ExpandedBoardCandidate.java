package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.domain.BoardGenome;

public record ExpandedBoardCandidate(long id, BoardGenome geneticGenome,
                                     ExpandedBoardEvaluation evaluation) {}
