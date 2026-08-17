package games.descent2e.pcg_clean.ui;

/** Candidate-specific text, colour and click behavior for a generic QD heatmap. */
public interface QualityEliteRenderer<C> {
    double colourValue(C candidate);
    String firstLine(C candidate);
    String secondLine(C candidate);
    String tooltip(C candidate);
    void open(C candidate);
}
