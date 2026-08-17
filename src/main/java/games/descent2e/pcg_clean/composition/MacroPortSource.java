package games.descent2e.pcg_clean.composition;

/** Immediate child port from which one public macro port originates. */
public record MacroPortSource(int macroPort, int childInstance, int childPort) {}
