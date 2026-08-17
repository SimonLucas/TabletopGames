package games.descent2e.pcg_clean.qd;

import java.util.List;

/** Metadata for one discrete quality-diversity behavior axis. */
public record DescriptorAxis(String id, String label, List<String> binLabels) {
    public DescriptorAxis {
        if (id == null || id.isBlank() || label == null || label.isBlank() || binLabels.isEmpty())
            throw new IllegalArgumentException("Invalid descriptor axis");
        binLabels = List.copyOf(binLabels);
    }

    public int bins() { return binLabels.size(); }
}
