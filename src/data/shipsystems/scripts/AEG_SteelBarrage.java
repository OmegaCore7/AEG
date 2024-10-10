package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;

public class AEG_SteelBarrage extends BaseShipSystemScript {

    private static final float RAM_RADIUS = 3000f;
    private static final float RAM_FORCE = 500f; // Adjusted value
    private static final float RAM_DAMAGE = 1000f; // Damage to apply
    private static final float COLLISION_THRESHOLD = 50f; // Threshold for collision detection
    private static final int RAM_COUNT = 5; // Number of rams
    private static final float PAUSE_DURATION = 1f; // Pause duration in seconds
    private static final Color LIGHT_GREEN_COLOR = new Color(144, 238, 144, 255); // RGBA for light green

    private int ramCounter = 0;
    private float pauseTimer = 0f;
    private ShipAPI targetShip = null;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        if (state == State.IN) {
            // Initialize the system
            if (ramCounter == 0) {
                // Find the nearest enemy ship within the ram radius
                targetShip = findClosestTarget(ship);
                if (targetShip != null) {
                    ramCounter = RAM_COUNT;
                }
            }
        } else if (state == State.OUT) {
            // Handle the phasing and ramming logic during the chargedown phase
            if (targetShip != null && ramCounter > 0) {
                if (pauseTimer <= 0f) {
                    // Teleport to a predefined location around the target ship
                    Vector2f teleportLocation = getPredefinedLocationAroundTarget(targetShip, ramCounter);
                    phaseTeleport(ship, teleportLocation);

                    // Ensure the ship is not inside the enemy ship
                    if (isInsideTarget(ship, targetShip)) {
                        adjustPosition(ship, targetShip);
                    }

                    // Ensure the ship phases out before ramming
                    ship.setPhased(false);

                    // Apply ramming force and damage
                    applyRammingForceAndDamage(ship, targetShip, id, effectLevel);

                    // Reset pause timer and decrement ram counter
                    pauseTimer = PAUSE_DURATION;
                    ramCounter--;
                } else {
                    // Decrease pause timer
                    pauseTimer -= Global.getCombatEngine().getElapsedInLastFrame();
                }
            }
        }
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

    private Vector2f getPredefinedLocationAroundTarget(ShipAPI target, int ramCounter) {
        float angle = (float) (Math.PI / 2 * ramCounter); // 90 degrees apart
        float distance = 1500f; // Increase teleport distance to 1500f
        float x = target.getLocation().x + (float) Math.cos(angle) * distance;
        float y = target.getLocation().y + (float) Math.sin(angle) * distance;
        return new Vector2f(x, y);
    }

    private boolean isInsideTarget(ShipAPI ship, ShipAPI target) {
        float distance = Vector2f.sub(target.getLocation(), ship.getLocation(), null).length();
        return distance < target.getCollisionRadius();
    }

    private void adjustPosition(ShipAPI ship, ShipAPI target) {
        Vector2f direction = Vector2f.sub(ship.getLocation(), target.getLocation(), null);
        direction.normalise();
        direction.scale(target.getCollisionRadius() + ship.getCollisionRadius());
        Vector2f newPosition = Vector2f.add(target.getLocation(), direction, null);
        ship.setFixedLocation(newPosition);
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
                target.getFluxTracker().increaseFlux(RAM_DAMAGE * 2 * effectLevel, true);
            } else {
                // Apply damage to armor if present
                float armorValue = target.getArmorGrid().getArmorRating() * target.getArmorGrid().getMaxArmorInCell();
                if (armorValue > 0) {
                    float effectiveDamage = RAM_DAMAGE * effectLevel * (1 - armorValue / (armorValue + RAM_DAMAGE));
                    // Convert Vector2f to integer coordinates
                    int x = (int) target.getLocation().x;
                    int y = (int) target.getLocation().y;
                    // Manually reduce armor
                    target.getArmorGrid().setArmorValue(x, y, Math.max(0, armorValue - effectiveDamage));
                } else {
                    // Apply full damage to hull if no armor
                    target.getMutableStats().getHullDamageTakenMult().modifyMult(id, 1 + (RAM_DAMAGE * effectLevel / target.getMaxHitpoints()));
                }
            }
        }
    }

    private void phaseTeleport(ShipAPI ship, Vector2f location) {
        CombatEngineAPI engine = Global.getCombatEngine();
        engine.spawnExplosion(ship.getLocation(), ship.getVelocity(), LIGHT_GREEN_COLOR, 100f, 1f);
        ship.setFixedLocation(location);
        engine.spawnExplosion(location, ship.getVelocity(), LIGHT_GREEN_COLOR, 100f, 1f);
        ship.setPhased(true); // Phase in for teleport
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        // No need to unapply anything since we removed the damage immunity
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("Ramming enemy ships!", false);
        }
        return null;
    }
}
