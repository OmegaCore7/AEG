package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.VectorUtils;

public class AEG_MeteorSmash {

    private static final float SPEED_BOOST = 2.0f;
    private static final float MANEUVERABILITY_BOOST = 2.0f;
    private static final float PUSH_FORCE = 500f;
    private static final float TIME_DILATION_DURATION = 0.25f; // Shortened duration
    private static final float TIME_MULT = 0.1f;
    private static final float ROTATION_SPEED = 180f; // Degrees per second

    public static final float TARGET_LEFT_ARM_ANGLE = -40f;
    public static final float TARGET_RIGHT_ARM_ANGLE = 40f;
    public static final float TARGET_LEFT_SHOULDER_ANGLE = -7f;
    public static final float TARGET_RIGHT_SHOULDER_ANGLE = 7f;

    private static Vector2f initialLeftArmPos;
    private static Vector2f initialRightArmPos;
    private static Vector2f initialLeftShoulderPos;
    private static Vector2f initialRightShoulderPos;

    private static float initialLeftArmAngle;
    private static float initialRightArmAngle;
    private static float initialLeftShoulderAngle;
    private static float initialRightShoulderAngle;

    public static void execute(ShipAPI ship, String id) {
        CombatEngineAPI engine = Global.getCombatEngine();

        // Apply time dilation for the transition
        AEG_TimeDilationHelper.applyTimeDilation(ship, TIME_DILATION_DURATION, TIME_MULT);

        // Lock weapons into place at specified angles
        setWeaponAngles(ship);

        // Apply visual effects for ramming maneuver
        AEG_SB_Effect.applyRammingEffects(ship, engine);

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

        // Rotate ship to face target smoothly
        if (ship.getMouseTarget() != null) {
            float targetAngle = VectorUtils.getAngle(ship.getLocation(), ship.getMouseTarget());
            smoothRotateToAngle(ship, targetAngle, engine.getElapsedInLastFrame());
        }
    }

    private static void setWeaponAngles(ShipAPI ship) {
        float shipFacing = ship.getFacing();
        for (WeaponAPI w : ship.getAllWeapons()) {
            switch (w.getSlot().getId()) {
                case "WS0003":
                    w.setCurrAngle(shipFacing + TARGET_LEFT_ARM_ANGLE);
                    break;
                case "WS0004":
                    w.setCurrAngle(shipFacing + TARGET_RIGHT_ARM_ANGLE);
                    break;
                case "WS0001":
                    w.setCurrAngle(shipFacing + TARGET_LEFT_SHOULDER_ANGLE);
                    break;
                case "WS0002":
                    w.setCurrAngle(shipFacing + TARGET_RIGHT_SHOULDER_ANGLE);
                    break;
            }
        }
    }

    public static void initializePositions(ShipAPI ship) {
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

    public static void resetPositions(ShipAPI ship) {
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

    private static void smoothRotateToAngle(ShipAPI ship, float targetAngle, float deltaTime) {
        float currentAngle = ship.getFacing();
        float shortestAngle = MathUtils.getShortestRotation(currentAngle, targetAngle);
        float rotationAmount = ROTATION_SPEED * deltaTime;

        if (Math.abs(shortestAngle) < rotationAmount) {
            ship.setFacing(targetAngle);
        } else {
            ship.setFacing(currentAngle + Math.signum(shortestAngle) * rotationAmount);
        }
    }
}