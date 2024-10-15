package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.util.Misc;
import data.shipsystems.helpers.JitterEffectManager;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;

public class AEG_SteelBarrage extends BaseShipSystemScript {

    private static final float RAM_RADIUS = 1000f;
    private static final float RAM_FORCE = 450f; // Adjusted value
    private static final float RAM_DAMAGE = 2000f; // Damage to apply
    private static final float COLLISION_THRESHOLD = 75f; // Threshold for collision detection
    private static final int RAM_COUNT = 5; // Number of rams
    private static final float PAUSE_DURATION = 0.4f; // Reduced pause duration in seconds
    private static final Color LIGHT_GREEN_COLOR = new Color(144, 238, 144, 255); // RGBA for light green
    private static final Color EXPLOSION_COLOR = new Color(175, 220, 120, 255); // Bright green explosion color

    private int ramCounter = 0;
    private float pauseTimer = 0f;
    private ShipAPI targetShip = null;
    private int maneuverStep = 0;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        if (state == State.ACTIVE) {
            if (ramCounter == 0) {
                // Find the nearest enemy ship within the ram radius
                targetShip = findClosestTarget(ship);
                if (targetShip != null) {
                    ramCounter = RAM_COUNT;
                }
            }

            if (targetShip != null && ramCounter > 0) {
                if (pauseTimer <= 0f) {
                    if (maneuverStep == 0) {
                        // Step 1: Create hitspark on the target ship
                        createHitspark(ship, targetShip);
                    }

                    // Perform maneuvers before ramming
                    performManeuvers(ship, targetShip);

                    if (maneuverStep >= 5) { // Adjusted for additional maneuvers
                        // Step 2: Ram the target
                        applyRammingForceAndDamage(ship, targetShip, id, effectLevel);

                        // Step 3: Create a big green explosion
                        createExplosion(ship, targetShip);

                        // Reset maneuver step and pause timer
                        maneuverStep = 0;
                        pauseTimer = PAUSE_DURATION;
                        ramCounter--;
                    } else {
                        // Increment maneuver step and reset pause timer
                        maneuverStep++;
                        pauseTimer = PAUSE_DURATION / 2f; // Shorter pause between maneuvers
                    }
                } else {
                    // Decrease pause timer
                    pauseTimer -= Global.getCombatEngine().getElapsedInLastFrame();
                }
            }
        }

        // Add jitter copies continuously
        addJitterCopies(ship);
    }

    private ShipAPI findClosestTarget(ShipAPI ship) {
        ShipAPI closestTarget = null;
        float closestDistance = Float.MAX_VALUE;
        for (ShipAPI target : Global.getCombatEngine().getShips()) {
            if (target.getOwner() != ship.getOwner()) {
                float distance = Vector2f.sub(target.getLocation(), ship.getLocation(), null).length();
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestTarget = target;
                }
            }
        }
        return closestTarget;
    }

    private void createHitspark(ShipAPI ship, ShipAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f arcLocation = target.getLocation();
        engine.spawnEmpArc(
                ship, ship.getLocation(), ship, target,
                DamageType.ENERGY, // Damage type
                0f, // No damage
                0f, // No EMP damage
                100f, // Reduced range for small hitspark
                "tachyon_lance_emp_impact", // Impact sound
                5f, // Reduced thickness for small hitspark
                LIGHT_GREEN_COLOR, // Fringe color
                LIGHT_GREEN_COLOR // Core color
        );
    }

    private void faceTarget(ShipAPI ship, ShipAPI target) {
        Vector2f direction = Vector2f.sub(target.getLocation(), ship.getLocation(), null);
        float angle = Misc.getAngleInDegrees(direction);
        ship.setFacing(angle);
    }

    private void applyRammingForceAndDamage(ShipAPI ship, ShipAPI target, String id, float effectLevel) {
        Vector2f diff = Vector2f.sub(target.getLocation(), ship.getLocation(), null);
        diff.normalise();
        diff.scale(RAM_FORCE * effectLevel);
        ship.getVelocity().set(diff);

        // Check for collision
        float distance = Vector2f.sub(target.getLocation(), ship.getLocation(), null).length();
        if (distance <= COLLISION_THRESHOLD) {
            // Apply damage to the target's shield if it has one
            if (target.getShield() != null && target.getShield().isOn()) {
                target.getFluxTracker().increaseFlux(RAM_DAMAGE * 2f * effectLevel, true);
            } else {
                // Apply damage to armor if present
                float armorValue = target.getArmorGrid().getArmorRating() * target.getArmorGrid().getMaxArmorInCell();
                if (armorValue > 0) {
                    float effectiveDamage = RAM_DAMAGE * effectLevel * (1f - armorValue / (armorValue + RAM_DAMAGE));
                    // Convert Vector2f to integer coordinates
                    int x = (int) target.getLocation().x;
                    int y = (int) target.getLocation().y;
                    // Manually reduce armor
                    target.getArmorGrid().setArmorValue(x, y, Math.max(0f, armorValue - effectiveDamage));
                } else {
                    // Apply full damage to hull if no armor
                    target.getMutableStats().getHullDamageTakenMult().modifyMult(id, 1f + (RAM_DAMAGE * effectLevel / target.getMaxHitpoints()));
                }
            }
        }
    }

    private void createExplosion(ShipAPI ship, ShipAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();
        engine.spawnExplosion(target.getLocation(), target.getVelocity(), EXPLOSION_COLOR, 300f, 2f);
    }

    private void performManeuvers(ShipAPI ship, ShipAPI target) {
        Vector2f direction = Vector2f.sub(target.getLocation(), ship.getLocation(), null);
        direction.normalise();
        Vector2f perpendicular = new Vector2f(-direction.y, direction.x); // Perpendicular vector for strafing

        switch (maneuverStep) {
            case 0:
                // Example maneuver: Strafe to the right
                perpendicular.scale(500f); // Adjust the strafing speed as needed
                Vector2f.add(ship.getVelocity(), perpendicular, ship.getVelocity());
                break;
            case 1:
                // Example maneuver: Drift
                Vector2f drift = new Vector2f(direction);
                drift.scale(200f); // Adjust the drifting speed as needed
                Vector2f.add(ship.getVelocity(), drift, ship.getVelocity());
                break;
            case 2:
                // Example maneuver: Curve
                Vector2f curve = new Vector2f(direction);
                curve.scale(300f); // Adjust the curving speed as needed
                Vector2f.add(ship.getVelocity(), curve, ship.getVelocity());
                break;
            case 3:
                // Additional maneuver: Strafe to the left
                perpendicular.scale(-500f); // Adjust the strafing speed as needed
                Vector2f.add(ship.getVelocity(), perpendicular, ship.getVelocity());
                break;
            case 4:
                // Additional maneuver: Reverse drift
                Vector2f reverseDrift = new Vector2f(direction);
                reverseDrift.scale(-200f); // Adjust the drifting speed as needed
                Vector2f.add(ship.getVelocity(), reverseDrift, ship.getVelocity());
                break;
        }

        // Add random half rotations to simulate swinging fists
        float randomRotation = (float) (Math.random() * 180 - 90); // Random angle between -90 and 90 degrees
        ship.setFacing(ship.getFacing() + randomRotation);

        // Course correction logic to ensure the ship hits the target
        Vector2f currentVelocity = ship.getVelocity();
        Vector2f targetDirection = Vector2f.sub(target.getLocation(), ship.getLocation(), null);
        targetDirection.normalise();
        targetDirection.scale(currentVelocity.length());
        ship.getVelocity().set(targetDirection);
    }

    private void addJitterCopies(ShipAPI ship) {
        Color jitterColor = new Color(144, 238, 144, 255); // Light green color
        float jitterDuration = 5.0f; // Duration of the jitter copy
        float jitterRange = 5.0f; // Range of the jitter effect

        for (int i = 0; i < 10; i++) { // Create 10 jitter copies
            JitterEffectManager.addJitterCopy(ship, jitterColor, jitterDuration, jitterRange);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        // No time dilation to unapply, so this method can remain empty or handle other cleanup if needed
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("True Destruction Steel Fist Barrage!", false);
        }
        return null;
    }
}
