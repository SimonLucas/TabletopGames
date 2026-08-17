package games.descent2e.pcg_clean.composition;

/** Bounded catalogue feedback retained only while a macro remains an archive elite. */
public record MacroUsageStatistics(long graftAttempts, long successfulGrafts,
                                   long boardAdmissions, long feasibleBoardAdmissions,
                                   double cumulativeAdmittedFitness) {
    public static MacroUsageStatistics empty() { return new MacroUsageStatistics(0, 0, 0, 0, 0); }

    public MacroUsageStatistics graft(boolean successful) {
        return new MacroUsageStatistics(graftAttempts + 1, successfulGrafts + (successful ? 1 : 0),
                boardAdmissions, feasibleBoardAdmissions, cumulativeAdmittedFitness);
    }

    public MacroUsageStatistics admitted(boolean feasible, double fitness) {
        return new MacroUsageStatistics(graftAttempts, successfulGrafts, boardAdmissions + 1,
                feasibleBoardAdmissions + (feasible ? 1 : 0), cumulativeAdmittedFitness + fitness);
    }

    public double graftSuccessRate() { return graftAttempts == 0 ? 0 : successfulGrafts / (double) graftAttempts; }
    public double meanAdmittedFitness() { return boardAdmissions == 0 ? 0 : cumulativeAdmittedFitness / boardAdmissions; }
}
