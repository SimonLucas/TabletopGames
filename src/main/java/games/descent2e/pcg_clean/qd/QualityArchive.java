package games.descent2e.pcg_clean.qd;

import java.util.*;

/** Reusable, bounded one-elite-per-cell archive for board, macro or atomic candidates. */
public final class QualityArchive<C> {
    private final QualityDescriptor<C> descriptor;
    private final Comparator<C> qualityOrder;
    private final Map<BehaviorCell, C> elites = new TreeMap<>();
    private long evaluations;

    /** qualityOrder must place the preferred candidate first. */
    public QualityArchive(QualityDescriptor<C> descriptor, Comparator<C> qualityOrder) {
        this.descriptor = Objects.requireNonNull(descriptor);
        this.qualityOrder = Objects.requireNonNull(qualityOrder);
    }

    public synchronized boolean offer(C candidate) {
        evaluations++;
        BehaviorCell cell = descriptor.describe(candidate);
        descriptor.validate(cell);
        C incumbent = elites.get(cell);
        if (incumbent == null || qualityOrder.compare(candidate, incumbent) < 0) {
            elites.put(cell, candidate);
            return true;
        }
        return false;
    }

    public synchronized QualityArchiveSnapshot<C> snapshot() {
        return new QualityArchiveSnapshot<>(evaluations, elites, descriptor.axes());
    }
}
