package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.shipsystems.helpers.AEG_MeteorSmash;
import data.shipsystems.helpers.AEG_NaniteSwarm;
import data.shipsystems.helpers.AEG_VanishingManeuver;
import data.shipsystems.helpers.AEG_SBRegen;
import data.shipsystems.helpers.AEG_UltimateManeuver;
import org.lwjgl.input.Keyboard;

public class AEG_SteelBarrage extends BaseShipSystemScript {

    private static final float SHIELD_UNFOLD_RATE_MULT = 10.0f;

    private AEG_SBRegen regenHelper;

    private enum ManeuverState { IDLE, METEOR_SMASH, VANISHING_MANEUVER, NANITE_SWARM, ULTIMATE }
    private ManeuverState currentState = ManeuverState.IDLE;

    private void transitionToState(ManeuverState newState, ShipAPI ship, String id) {
        // Deactivate current state
        switch (currentState) {
            case METEOR_SMASH:
                AEG_MeteorSmash.resetPositions(ship);
                break;
            case VANISHING_MANEUVER:
                // Reset vanishing maneuver effects
                break;
            case NANITE_SWARM:
                // Reset nanite swarm effects
                break;
            case ULTIMATE:
                // Ultimate maneuver ends automatically
                break;
            default:
                break;
        }

        // Activate new state
        switch (newState) {
            case METEOR_SMASH:
                AEG_MeteorSmash.execute(ship, id);
                break;
            case VANISHING_MANEUVER:
                AEG_VanishingManeuver.execute(ship, id);
                break;
            case NANITE_SWARM:
                AEG_NaniteSwarm.execute(ship, id);
                break;
            case ULTIMATE:
                AEG_UltimateManeuver.execute(ship, id);
                break;
            default:
                break;
        }

        currentState = newState;
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        ship.getMutableStats().getShieldUnfoldRateMult().modifyMult(id, SHIELD_UNFOLD_RATE_MULT);

        if (regenHelper == null) {
            regenHelper = new AEG_SBRegen(ship);
        }

        if (state == State.IN) {
            AEG_MeteorSmash.initializePositions(ship);
        } else if (state == State.ACTIVE) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
                    transitionToState(ManeuverState.METEOR_SMASH, ship, id);
                } else if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
                    transitionToState(ManeuverState.VANISHING_MANEUVER, ship, id);
                } else if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
                    transitionToState(ManeuverState.NANITE_SWARM, ship, id);
                } else if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
                    transitionToState(ManeuverState.ULTIMATE, ship, id);
                }
            }
        } else if (state == State.OUT) {
            AEG_MeteorSmash.resetPositions(ship);
        }

        regenHelper.advance(Global.getCombatEngine().getElapsedInLastFrame());
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        ship.getMutableStats().getMaxSpeed().unmodify(id);
        ship.getMutableStats().getAcceleration().unmodify(id);
        ship.getMutableStats().getDeceleration().unmodify(id);

        ship.getEngineController().extendFlame(id, 1.0f, 1.0f, 1.0f);

        AEG_MeteorSmash.resetPositions(ship);

        ship.getMutableStats().getShieldUnfoldRateMult().unmodify(id);
    }
}