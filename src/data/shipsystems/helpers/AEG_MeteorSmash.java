package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class AEG_MeteorSmash {

    // Constants for target weapon angles
    public static final float TARGET_LEFT_ARM_ANGLE = -40f;
    public static final float TARGET_RIGHT_ARM_ANGLE = 40f;
    public static final float TARGET_LEFT_SHOULDER_ANGLE = -7f;
    public static final float TARGET_RIGHT_SHOULDER_ANGLE = 7f;

    // Constants for speed and maneuverability boosts
    private static final float SPEED_BOOST = 4.0f; // Speed boost multiplier
    private static final float MANEUVERABILITY_BOOST = 2.0f; // Maneuverability boost multiplier

    // Variables to store initial positions and angles of weapons
    private static Vector2f initialLeftArmPos;
    private static Vector2f initialRightArmPos;
    private static Vector2f initialLeftShoulderPos;
    private static Vector2f initialRightShoulderPos;

    private static float initialLeftArmAngle;
    private static float initialRightArmAngle;
    private static float initialLeftShoulderAngle;
    private static float initialRightShoulderAngle;

    // Method to execute the Meteor Smash ability
    public static void execute(final ShipAPI ship, String id) {
        final CombatEngineAPI engine = Global.getCombatEngine();

        // Lock weapons into place at specified angles
        setWeaponAngles(ship);

        // Increase engine flame size
        ship.getEngineController().extendFlame(id, 1.5f, 1.5f, 1.5f);

        // Apply speed and maneuverability boost
        ship.getMutableStats().getMaxSpeed().modifyMult(id, SPEED_BOOST);
        ship.getMutableStats().getAcceleration().modifyMult(id, MANEUVERABILITY_BOOST);
        ship.getMutableStats().getDeceleration().modifyMult(id, MANEUVERABILITY_BOOST);
        ship.getMutableStats().getTurnAcceleration().modifyMult(id, MANEUVERABILITY_BOOST);
        ship.getMutableStats().getMaxTurnRate().modifyMult(id, MANEUVERABILITY_BOOST);

        // Check for collision in the advance method
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (engine.isPaused()) return;

                // Check for collisions with other ships
                for (ShipAPI target : engine.getShips()) {
                    if (target == ship || target.isHulk() || !target.isAlive()) continue;

                    if (Misc.getDistance(ship.getLocation(), target.getLocation()) < ship.getCollisionRadius() + target.getCollisionRadius()) {
                        if (target.getHullSize() == ShipAPI.HullSize.FIGHTER || target.getHullSize() == ShipAPI.HullSize.FRIGATE) {
                            // Destroy fighter or frigate immediately
                            engine.applyDamage(target, target.getLocation(), target.getHitpoints(), DamageType.FRAGMENTATION, 0f, false, false, ship);
                            createEMPFeedback(target.getLocation());
                        }
                        engine.removePlugin(this); // Remove the plugin after collision is detected
                        break;
                    }
                }

                // Check for collisions with projectiles
                for (DamagingProjectileAPI projectile : engine.getProjectiles()) {
                    if (Misc.getDistance(ship.getLocation(), projectile.getLocation()) < ship.getCollisionRadius()) {
                        bounceProjectile(projectile);
                    }
                }

                // Check for collisions with missiles
                for (MissileAPI missile : engine.getMissiles()) {
                    if (Misc.getDistance(ship.getLocation(), missile.getLocation()) < ship.getCollisionRadius()) {
                        bounceMissile(missile);
                    }
                }
            }
        });
    }

    // Method to create EMP feedback effect
    private static void createEMPFeedback(Vector2f location) {
        final CombatEngineAPI engine = Global.getCombatEngine();
        engine.spawnEmpArcVisual(location, null, location, null, 10f, new Color(105,255,105,255), Color.CYAN);
    }

    // Method to bounce projectiles upon collision
    private static void bounceProjectile(DamagingProjectileAPI projectile) {
        // Calculate a random bounce direction
        float angle = (float) (Math.random() * 360);
        Vector2f bounceDirection = Misc.getUnitVectorAtDegreeAngle(angle);
        bounceDirection.scale(projectile.getVelocity().length());

        // Apply the bounce force
        projectile.getVelocity().set(bounceDirection);
    }

    // Method to bounce missiles upon collision
    private static void bounceMissile(MissileAPI missile) {
        // Calculate a random bounce direction
        float angle = (float) (Math.random() * 360);
        Vector2f bounceDirection = Misc.getUnitVectorAtDegreeAngle(angle);
        bounceDirection.scale(missile.getVelocity().length());

        // Apply the bounce force
        missile.getVelocity().set(bounceDirection);
    }

    // Method to set weapon angles to specified targets
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

    // Method to initialize positions and angles of weapons
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

    // Method to reset positions and angles of weapons to their initial states
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
}