package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.qd.*;

import java.util.List;
import java.util.stream.IntStream;

/** Default macro grid: construction scale versus number of exposed attachment ports. */
public final class MacroStructureDescriptor implements QualityDescriptor<MacroCandidate> {
    private final int pieceBins;
    private final int portBins;
    private final List<DescriptorAxis> axes;

    public MacroStructureDescriptor(int pieceBins, int portBins) {
        if (pieceBins < 2 || portBins < 2) throw new IllegalArgumentException("Macro grids need at least two bins");
        this.pieceBins = pieceBins;
        this.portBins = portBins;
        axes = List.of(new DescriptorAxis("atomic-pieces", "Atomic pieces", labels(pieceBins)),
                new DescriptorAxis("exposed-ports", "Exposed ports", labels(portBins)));
    }

    @Override public List<DescriptorAxis> axes() { return axes; }

    @Override
    public BehaviorCell describe(MacroCandidate candidate) {
        PieceMetrics metrics = candidate.definition().metrics();
        return new BehaviorCell(List.of(Math.min(metrics.atomicPieces(), pieceBins - 1),
                Math.min(metrics.exposedPorts(), portBins - 1)));
    }

    private List<String> labels(int bins) {
        return IntStream.range(0, bins).mapToObj(bin -> bin == bins - 1 ? bin + "+" : Integer.toString(bin)).toList();
    }
}
