package games.descent2e.pcg_clean.composition;

/** Intrinsic utility of a reusable module, independent of any complete board. */
public final class MacroQualityModel {
    public double score(MacroPieceDefinition definition) {
        PieceMetrics metrics = definition.metrics();
        double attachmentUtility = Math.min(1.0, metrics.exposedPorts() / 4.0);
        double directionUtility = metrics.exposedPorts() == 0 ? 0
                : metrics.portsByDirection().size() / 4.0;
        double scaleUtility = Math.min(1.0, metrics.atomicPieces() / 6.0);
        double branchUtility = Math.min(1.0, metrics.internalBranches() / 3.0);
        double cycleUtility = Math.min(1.0, metrics.internalCycles());
        return 0.20 * metrics.compactness() + 0.15 * attachmentUtility
                + 0.10 * directionUtility + 0.10 * scaleUtility
                + 0.15 * branchUtility + 0.30 * cycleUtility;
    }
}
