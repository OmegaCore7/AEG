package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.shipsystems.helpers.AEG_SB_Effect;
import data.shipsystems.helpers.AEG_SBRegen;
import data.shipsystems.helpers.AEG_TimeDilationHelper;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class AEG_SteelBarrage extends BaseShipSystemScript {

    private static final float SPEED_BOOST = 2.0f;
    private static final float MANEUVERABILITY_BOOST = 2.0f;
    private static final float PUSH_FORCE = 500f;
    private static final float TIME_DILATION_DURATION = 0.5f; // Shortened duration
    private static final float TIME_MULT = 0.1f;

    private Vector2f initialLeftArmPos;
    private Vector2f initialRightArmPos;
    private Vector2f initialLeftShoulderPos;
    private Vector2f initialRightShoulderPos;

    private float initialLeftArmAngle;
    private float initialRightArmAngle;
    private float initialLeftShoulderAngle;
    private float initialRightShoulderAngle;

    private static final float TARGET_LEFT_ARM_ANGLE = -40f;
    private static final float TARGET_RIGHT_ARM_ANGLE = 40f;
    private static final float TARGET_LEFT_SHOULDER_ANGLE = -7f;
    private static final float TARGET_RIGHT_SHOULDER_ANGLE = 7f;

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

        if (regenHelper == null) {
            regenHelper = new AEG_SBRegen(ship);
        }

        if (state == State.IN) {
            if (initialLeftArmPos == null || initialRightArmPos == null || initialLeftShoulderPos == null || initialRightShoulderPos == null) {
                for (WeaponAPI w : ship.getAllWeapons()) {
                    switch (w.getSlot().getId()) {
                        case "WS0003":
                            initialLeftArmPos = new Vector2f(w.getSlot().getLocation());
                            initialLeftArmAngle = w.getCurrAngle();
                            break;
                        case "WS0004":
                            initialRightArmPos = new Vector2f(w.getSlot().getLocation());
                            initialRightArmAngle = w.getCurrAngle();
                            break;
                        case "WS0001":
                            initialLeftShoulderPos = new Vector2f(w.getSlot().getLocation());
                            initialLeftShoulderAngle = w.getCurrAngle();
                            break;
                        case "WS0002":
                            initialRightShoulderPos = new Vector2f(w.getSlot().getLocation());
                            initialRightShoulderAngle = w.getCurrAngle();
                            break;
                    }
                }
            }
        } else if (state == State.ACTIVE) {
            // Apply time dilation for the transition
            AEG_TimeDilationHelper.applyTimeDilation(ship, TIME_DILATION_DURATION, TIME_MULT);

            // Lock weapons into place at specified angles
            setWeaponAngles(ship, TARGET_LEFT_ARM_ANGLE, TARGET_RIGHT_ARM_ANGLE, TARGET_LEFT_SHOULDER_ANGLE, TARGET_RIGHT_SHOULDER_ANGLE);

            // Apply visual effects for ramming maneuver
            AEG_SB_Effect.applyRammingEffects(ship, Global.getCombatEngine());

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
            // Reset arm and shoulder positions
            resetArmsAndShoulders(ship);
        }

        // Advance regeneration when the system isn't active
        regenHelper.advance(Global.getCombatEngine().getElapsedInLastFrame());
    }

    private void setWeaponAngles(ShipAPI ship, float leftArmAngle, float rightArmAngle, float leftShoulderAngle, float rightShoulderAngle) {
        float shipFacing = ship.getFacing();
        for (WeaponAPI w : ship.getAllWeapons()) {
            switch (w.getSlot().getId()) {
                case "WS0003":
                    w.setCurrAngle(shipFacing + leftArmAngle);
                    break;
                case "WS0004":
                    w.setCurrAngle(shipFacing + rightArmAngle);
                    break;
                case "WS0001":
                    w.setCurrAngle(shipFacing + leftShoulderAngle);
                    break;
                case "WS0002":
                    w.setCurrAngle(shipFacing + rightShoulderAngle);
                    break;
            }
        }
    }

    private void resetArmsAndShoulders(ShipAPI ship) {
        for (WeaponAPI w : ship.getAllWeapons()) {
            switch (w.getSlot().getId()) {
                case "WS0003":
                    if (initialLeftArmPos != null) {
                        w.getSlot().getLocation().set(initialLeftArmPos);
                        w.setCurrAngle(initialLeftArmAngle);
                    }
                    break;
                case "WS0004":
                    if (initialRightArmPos != null) {
                        w.getSlot().getLocation().set(initialRightArmPos);
                        w.setCurrAngle(initialRightArmAngle);
                    }
                    break;
                case "WS0001":
                    if (initialLeftShoulderPos != null) {
                        w.getSlot().getLocation().set(initialLeftShoulderPos);
                        w.setCurrAngle(initialLeftShoulderAngle);
                    }
                    break;
                case "WS0002":
                    if (initialRightShoulderPos != null) {
                        w.getSlot().getLocation().set(initialRightShoulderPos);
                        w.setCurrAngle(initialRightShoulderAngle);
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

        // Reset arm and shoulder positions
        resetArmsAndShoulders(ship);
    }
}
