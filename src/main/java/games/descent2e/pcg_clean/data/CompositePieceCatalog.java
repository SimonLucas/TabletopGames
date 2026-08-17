package games.descent2e.pcg_clean.data;

import games.descent2e.pcg_clean.domain.PieceDefinition;

import java.util.*;

/** Immutable overlay of generated definitions on a base atomic catalogue. */
public final class CompositePieceCatalog implements PieceCatalog {
    private final PieceCatalog base;
    private final Map<String, PieceDefinition> generated;

    public CompositePieceCatalog(PieceCatalog base, Collection<? extends PieceDefinition> generated) {
        this.base = Objects.requireNonNull(base);
        Map<String, PieceDefinition> index = new LinkedHashMap<>();
        generated.stream().sorted(Comparator.comparing(PieceDefinition::id)).forEach(definition -> {
            if (index.putIfAbsent(definition.id(), definition) != null)
                throw new IllegalArgumentException("Duplicate generated definition " + definition.id());
        });
        this.generated = Collections.unmodifiableMap(index);
    }

    @Override
    public PieceDefinition require(String id) {
        PieceDefinition definition = generated.get(id);
        return definition != null ? definition : base.require(id);
    }

    public List<PieceDefinition> generated() { return List.copyOf(generated.values()); }
}
