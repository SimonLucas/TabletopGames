package games.descent2e.pcg_clean.qd;

import java.util.*;

public record QualityArchiveSnapshot<C>(long evaluations, Map<BehaviorCell, C> elites,
                                        List<DescriptorAxis> axes) {
    public QualityArchiveSnapshot {
        elites = Collections.unmodifiableMap(new TreeMap<>(elites));
        axes = List.copyOf(axes);
    }
}
