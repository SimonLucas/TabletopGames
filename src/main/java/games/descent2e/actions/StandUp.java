package games.descent2e.actions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import static games.descent2e.actions.Triggers.ACTION_POINT_SPEND;

public class StandUp extends DescentAction{

    public StandUp() {
        super(ACTION_POINT_SPEND);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Stand up";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        Hero hero = (Hero) gs.getActingFigure();
        hero.setDefeated(false);
        hero.getNActionsExecuted().setToMax();   // Only thing they can do this turn
        // Health recovery: roll 2 red dice
        DicePool.revive.roll(gs.getRandom());
        hero.setAttribute(Figure.Attribute.Health, DicePool.revive.getDamage());
        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return dgs.getActingFigure() instanceof Hero && ((Hero)dgs.getActingFigure()).isDefeated() && dgs.getActingFigure().getNActionsExecuted().isMinimum();
    }
}