package games.descent2e.pcg_clean.composition;

import games.descent2e.pcg_clean.domain.PieceDefinition;

import java.util.Objects;

/** One child definition positioned in macro-local coordinates. */
public record MacroPlacement(int instanceId, PieceDefinition definition,
                             int x, int y, int quarterTurns) {
    public MacroPlacement {
        definition = Objects.requireNonNull(definition);
        quarterTurns = Math.floorMod(quarterTurns, 4);
    }
}
