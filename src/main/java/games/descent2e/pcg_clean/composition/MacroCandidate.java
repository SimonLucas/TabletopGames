package games.descent2e.pcg_clean.composition;

/** Evaluated macro library entry. Compiled macros normally have zero structural violations. */
public record MacroCandidate(long id, MacroPieceDefinition definition,
                             int violationCount, double quality) {}
