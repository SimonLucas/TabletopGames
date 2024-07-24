package games.toads;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.toads.actions.PlayFlankCard;
import games.toads.actions.UndoOpponentFlank;
import players.mcts.*;
import utilities.Pair;

import java.util.List;

import static players.mcts.MCTSEnums.OpponentTreePolicy.*;

public class ToadMCTSPlayer extends MCTSPlayer {

    int actingPlayer;
    boolean functionalityApplies;
    AbstractAction flankAction;

    public ToadMCTSPlayer(MCTSParams params) {
        super(params);
    }

    private boolean functionalityApplies(AbstractGameState gameState) {
        MCTSParams params = getParameters();
        if (params.opponentTreePolicy == SelfOnly || params.opponentTreePolicy == MCGSSelfOnly) {
            return false;
        }
        ToadGameState state = (ToadGameState) gameState;
        return state.getHiddenFlankCard(1 - actingPlayer) != null;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // We check if we are playing in defence (or are using a self-only tree)
        // if not, then we delegate to the super() method
        ToadGameState state = (ToadGameState) gameState;
        MCTSParams params = getParameters();

        int currentPlayer = state.getCurrentPlayer();
        actingPlayer = currentPlayer;
        if (flankAction != null) { // from the last action; we may have some clean up to do
            if (params.opponentTreePolicy == MultiTree) {
                // null out the root node for the opponent as it has dummy data in
                MultiTreeNode multiTreeNode = (MultiTreeNode) root;
                multiTreeNode.resetRoot(1 - actingPlayer);
            } else if (params.opponentTreePolicy == MCGS) {
                // nothing to do in this case
            } else {
                // find the node that the flankAction leads to, and set this as the root
                SingleTreeNode childNode = root.getChildren().get(flankAction)[currentPlayer];
                childNode.rootify(root);
                root = childNode;
            }
            flankAction = null;
        }

        functionalityApplies = functionalityApplies(state);
        if (!functionalityApplies) {
            return super._getAction(state, actions);
        }
        // otherwise, we add an UndoOpponentFlank
        new UndoOpponentFlank(state);

        List<AbstractAction> validActionsForOpponent = getForwardModel().computeAvailableActions(gameState);
        flankAction = super._getAction(state, validActionsForOpponent);

        // we then apply the bestAction (which should be a PlayFlankCard)
        if (!(flankAction instanceof PlayFlankCard)) {
            throw new AssertionError("Expected a PlayFlankCard action");
        }
        if (!validActionsForOpponent.contains(flankAction)) {
            flankAction = validActionsForOpponent.get(0);
            // this may be true due to redeterminisation of the game state
            // we store the actual action taken
        }

        // and then return the bestAction from the *next* state in the tree

        // in this case we look up the node for the new state after applying the flank action
        // and we don't actually need to apply the flank action as this cannot affect our information state
        // (except for the currentPlayer...)
        // So we can apply *any* valid flank action
        getForwardModel().next(state, flankAction);

        AbstractAction actualAction;
        if (params.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.MultiTree) {
            // in this case we use the root node for the decision player
            SingleTreeNode ourRoot = ((MultiTreeNode) root).getRoot(currentPlayer);
            actualAction = ourRoot.bestAction();
        } else if (params.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.MCGS) {
            Object stateKey = params.MCGSStateKey.getKey(state);
            MCGSNode node = ((MCGSNode) root).getTranspositionMap().get(stateKey);
            if (node == null) {
                throw new AssertionError("No node found for state key " + stateKey);
            }
            actualAction = node.bestAction();
        } else {
            // we have OMA or OneTree or something...we navigate manually down the tree and take the best action from the node we reach
            SingleTreeNode childNode = root.getChildren().get(flankAction)[currentPlayer];
            actualAction = childNode.bestAction();
        }
        this.lastAction = new Pair<>(actingPlayer, actualAction);
        return actualAction;
    }

    @Override
    protected void createRootNode(AbstractGameState gameState) {
        super.createRootNode(gameState);
        // we then just override the root so that redeterminisations occur from perspective of the correct player
        // otherwise we redeterminise from the root player (i.e. our opponent), which is very bad
        if (functionalityApplies) {
            root.setRedeterminisationPlayer(actingPlayer);
            // then we need to correct the transposition table
            if (root instanceof MCGSNode mcgsRoot) {
                mcgsRoot.getTranspositionMap().clear();
                mcgsRoot.getTranspositionMap().put(getParameters().MCGSStateKey.getKey(gameState, actingPlayer), mcgsRoot);
            }
        }
    }


    // for testing
    public SingleTreeNode getRoot() {
        return root;
    }
}