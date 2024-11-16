package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.shipsystems.helpers.AEG_MeteorSmash; // CHARGE AND RAM THINGS
import data.shipsystems.helpers.AEG_NaniteSwarm; // DISABLE THINGS
import data.shipsystems.helpers.AEG_VanishingManeuver; // Dodging and High Maneuverability
import data.shipsystems.helpers.AEG_SBRegen; // PASSIVE REGEN WHEN SYSTEM NOT BEING USED
import org.lwjgl.input.Keyboard; // HOW WE ATTACH EACH MANEUVER TO SHIFT+BUTTON

public class AEG_SteelBarrage extends BaseShipSystemScript {

    private static final float SHIELD_UNFOLD_RATE_MULT = 10.0f; // Near-instant unfold rate

    private AEG_SBRegen regenHelper;

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

        // Apply constant shield unfold rate increase
        ship.getMutableStats().getShieldUnfoldRateMult().modifyMult(id, SHIELD_UNFOLD_RATE_MULT);

        if (regenHelper == null) {
            regenHelper = new AEG_SBRegen(ship);
        }

        if (state == State.IN) {
            // Initialize positions for maneuvers
            AEG_MeteorSmash.initializePositions(ship);
        } else if (state == State.ACTIVE) {
            // Check for key combinations and execute corresponding maneuver
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
                    AEG_MeteorSmash.execute(ship, id);
                } else if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
                    AEG_VanishingManeuver.execute(ship, id);
                } else if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
                    AEG_NaniteSwarm.execute(ship, id);
                } else if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
                    // Do nothing for now
                }
            }
        } else if (state == State.OUT) {
            // Reset positions after maneuvers
            AEG_MeteorSmash.resetPositions(ship);
        }

        // Advance regeneration when the system isn't active
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

        // Reset speed and maneuverability
        ship.getMutableStats().getMaxSpeed().unmodify(id);
        ship.getMutableStats().getAcceleration().unmodify(id);
        ship.getMutableStats().getDeceleration().unmodify(id);

        // Reset engine flame size
        ship.getEngineController().extendFlame(id, 1.0f, 1.0f, 1.0f);

        // Reset arm and shoulder positions
        AEG_MeteorSmash.resetPositions(ship);

        // Remove shield unfold rate increase
        ship.getMutableStats().getShieldUnfoldRateMult().unmodify(id);
    }
}