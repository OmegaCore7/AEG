package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.shipsystems.helpers.AEG_MeteorSmash;
import data.shipsystems.helpers.AEG_SBRegen;
import org.lazywizard.lazylib.MathUtils;

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
            AEG_MeteorSmash.initializePositions(ship);
        } else if (state == State.ACTIVE) {
            // Execute the Meteor Smash maneuver
            AEG_MeteorSmash.execute(ship, id);
        } else if (state == State.OUT) {
            // Reset arm and shoulder positions
            AEG_MeteorSmash.resetPositions(ship);
        }

        // Advance regeneration when the system isn't active
        regenHelper.advance(Global.getCombatEngine().getElapsedInLastFrame());

        // Check for collision and handle it
        for (CombatEntityAPI entity : Global.getCombatEngine().getShips()) {
            if (entity instanceof ShipAPI && entity != ship && ship.getCollisionClass() != CollisionClass.NONE) {
                if (ship.getCollisionRadius() + entity.getCollisionRadius() > MathUtils.getDistance(ship, entity)) {
                    AEG_MeteorSmash.handleCollision(ship, entity);
                    // Trigger the next maneuver (Gigantic Catastrophe)
                    // Placeholder for Gigantic Catastrophe maneuver
                    break;
                }
            }
        }
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