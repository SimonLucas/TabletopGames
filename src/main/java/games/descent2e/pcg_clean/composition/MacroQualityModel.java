package games.descent2e.pcg_clean.composition;

/** Intrinsic utility of a reusable module, independent of any complete board. */
public final class MacroQualityModel {
    public double score(MacroPieceDefinition definition) {
        PieceMetrics metrics = definition.metrics();
        double attachmentUtility = Math.min(1.0, metrics.exposedPorts() / 4.0);
        double scaleUtility = Math.min(1.0, metrics.atomicPieces() / 6.0);
        double topologyUtility = Math.min(1.0,
                (metrics.internalBranches() + metrics.internalCycles()) / 3.0);
        return 0.40 * metrics.compactness() + 0.25 * attachmentUtility
                + 0.20 * scaleUtility + 0.15 * topologyUtility;
    }
}
