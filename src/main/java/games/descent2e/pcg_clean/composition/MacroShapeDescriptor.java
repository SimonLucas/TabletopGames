package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.qd.*;

import java.util.List;

/**
 * Shape-diversity grid. Axis balance separates straight from bent/square footprints;
 * compactness separates L/sparse shapes from filled rooms of similar extent.
 */
public final class MacroShapeDescriptor implements QualityDescriptor<MacroCandidate> {
    private final int balanceBins;
    private final int compactnessBins;
    private final List<DescriptorAxis> axes;

    public MacroShapeDescriptor(int balanceBins, int compactnessBins) {
        if (balanceBins < 2 || compactnessBins < 2) throw new IllegalArgumentException("Shape grids need two bins");
        this.balanceBins = balanceBins;
        this.compactnessBins = compactnessBins;
        axes = List.of(new DescriptorAxis("axial-balance", "Axial balance (minor / major extent)",
                        percentageLabels(balanceBins)),
                new DescriptorAxis("compactness", "Occupied footprint / bounding box",
                        percentageLabels(compactnessBins)));
    }

    @Override public List<DescriptorAxis> axes() { return axes; }

    @Override
    public BehaviorCell describe(MacroCandidate candidate) {
        PieceMetrics metrics = candidate.definition().metrics();
        double balance = Math.min(metrics.width(), metrics.height())
                / (double) Math.max(metrics.width(), metrics.height());
        return new BehaviorCell(List.of(bin(balance, balanceBins), bin(metrics.compactness(), compactnessBins)));
    }

    private int bin(double value, int bins) {
        return Math.min(bins - 1, Math.max(0, (int) Math.floor(value * bins)));
    }

    private List<String> percentageLabels(int bins) {
        return java.util.stream.IntStream.range(0, bins)
                .mapToObj(bin -> "%d–%d%%".formatted(100 * bin / bins, 100 * (bin + 1) / bins)).toList();
    }
}
