package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.shipsystems.helpers.AEG_TimeDilationHelper;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class AEG_SteelBarrage extends BaseShipSystemScript {

    private static final float SPEED_BOOST = 2.0f;
    private static final float MANEUVERABILITY_BOOST = 2.0f;
    private static final float PUSH_FORCE = 500f;
    private static final float TIME_DILATION_DURATION = 1.0f;
    private static final float TIME_MULT = 0.1f;

    private Vector2f initialLeftArmPos;
    private Vector2f initialRightArmPos;

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

        if (state == State.IN) {
            if (initialLeftArmPos == null || initialRightArmPos == null) {
                for (WeaponAPI w : ship.getAllWeapons()) {
                    switch (w.getSlot().getId()) {
                        case "WS0003":
                            initialLeftArmPos = new Vector2f(w.getSlot().getLocation());
                            break;
                        case "WS0004":
                            initialRightArmPos = new Vector2f(w.getSlot().getLocation());
                            break;
                    }
                }
            }

            // Apply time dilation for 1 second
            AEG_TimeDilationHelper.applyTimeDilation(ship, TIME_DILATION_DURATION, TIME_MULT);

            // Move arms back diagonally
            moveArms(ship, -5f, -2f, -3f);
        } else if (state == State.ACTIVE) {
            // Move arms forward diagonally
            moveArms(ship, 15f, 2f, 2f);

            // Apply speed and maneuverability boost
            ship.getMutableStats().getMaxSpeed().modifyMult(id, SPEED_BOOST);
            ship.getMutableStats().getAcceleration().modifyMult(id, MANEUVERABILITY_BOOST);
            ship.getMutableStats().getDeceleration().modifyMult(id, MANEUVERABILITY_BOOST);

            // Increase engine flame size
            ship.getEngineController().extendFlame(id, 1.5f, 1.5f, 1.5f);

            // Apply push force
            Vector2f pushVector = VectorUtils.getDirectionalVector(ship.getLocation(), ship.getMouseTarget());
            pushVector.scale(PUSH_FORCE);
            Vector2f.add(ship.getVelocity(), pushVector, ship.getVelocity());

            // Rotate ship to face target
            if (ship.getMouseTarget() != null) {
                float targetAngle = VectorUtils.getAngle(ship.getLocation(), ship.getMouseTarget());
                ship.setFacing(targetAngle);
            }
        } else if (state == State.OUT) {
            // Reset arm positions
            resetArms(ship);
        }
    }

    private void moveArms(ShipAPI ship, float distance, float leftOffset, float rightOffset) {
        for (WeaponAPI w : ship.getAllWeapons()) {
            switch (w.getSlot().getId()) {
                case "WS0003":
                    if (initialLeftArmPos != null) {
                        Vector2f newPos = new Vector2f(initialLeftArmPos);
                        newPos.translate(leftOffset, distance);
                        w.getSlot().getLocation().set(newPos);
                    }
                    break;
                case "WS0004":
                    if (initialRightArmPos != null) {
                        Vector2f newPos = new Vector2f(initialRightArmPos);
                        newPos.translate(rightOffset, distance);
                        w.getSlot().getLocation().set(newPos);
                    }
                    break;
            }
        }
    }

    private void resetArms(ShipAPI ship) {
        for (WeaponAPI w : ship.getAllWeapons()) {
            switch (w.getSlot().getId()) {
                case "WS0003":
                    if (initialLeftArmPos != null) {
                        w.getSlot().getLocation().set(initialLeftArmPos);
                    }
                    break;
                case "WS0004":
                    if (initialRightArmPos != null) {
                        w.getSlot().getLocation().set(initialRightArmPos);
                    }
                    break;
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

        // Reset arm positions
        resetArms(ship);
    }
}
