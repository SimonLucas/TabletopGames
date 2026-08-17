package games.descent2e.pcg_clean.composition;

/** A port connection internal to a macro composition. */
public record MacroConnection(int firstInstance, int firstPort, int secondInstance, int secondPort) {
    public MacroConnection {
        if (firstInstance == secondInstance) throw new IllegalArgumentException("A piece cannot connect to itself");
    }
}
