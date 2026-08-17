package games.descent2e.pcg_clean.qd;

import java.util.List;

public interface QualityDescriptor<C> {
    List<DescriptorAxis> axes();
    BehaviorCell describe(C candidate);

    default void validate(BehaviorCell cell) {
        if (cell.bins().size() != axes().size())
            throw new IllegalArgumentException("Descriptor returned the wrong number of axes");
        for (int i = 0; i < axes().size(); i++) {
            int bin = cell.bins().get(i);
            if (bin < 0 || bin >= axes().get(i).bins())
                throw new IllegalArgumentException("Descriptor bin is out of range for " + axes().get(i).id());
        }
    }
}
