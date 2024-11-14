package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class AEG_OmegaBlasterFinisher {

    private static final float SCAN_RADIUS = 2000f;
    private static final float OMEGA_BLASTER_RANGE = 500f;
    private static final float ROTATION_SPEED = 180f; // Degrees per second

    public static void execute(ShipAPI ship, String id) {
        CombatEngineAPI engine = Global.getCombatEngine();

        // Scan for targets within the radius
        List<CombatEntityAPI> targets = getTargetsInRange(engine, ship.getLocation(), SCAN_RADIUS);

        // Select the best weapon for each target and execute the maneuver
        for (CombatEntityAPI target : targets) {
            if (target instanceof ShipAPI) {
                ShipAPI targetShip = (ShipAPI) target;
                selectAndFireWeapon(ship, targetShip, engine);
            }
        }

        // Apply dynamic movements
        applyDynamicMovements(ship, engine);
    }

    private static List<CombatEntityAPI> getTargetsInRange(CombatEngineAPI engine, Vector2f point, float range) {
        List<CombatEntityAPI> result = new ArrayList<>();
        for (CombatEntityAPI entity : engine.getShips()) {
            if (MathUtils.getDistance(point, entity.getLocation()) <= range) {
                result.add(entity);
            }
        }
        return result;
    }

    private static void selectAndFireWeapon(ShipAPI ship, ShipAPI target, CombatEngineAPI engine) {
        float distance = MathUtils.getDistance(ship, target);

        if (distance <= OMEGA_BLASTER_RANGE) {
            fireWeapon(ship, "WS0013", target); // Omega Blaster
        } else if (distance <= 1000f) {
            fireWeapon(ship, "WS0009", target); // Devastating beam
        } else if (distance <= 1500f) {
            fireWeapon(ship, "WS0007", target); // Fast firing pulse laser
            fireWeapon(ship, "WS0008", target); // Faster firing pulse laser
        } else {
            fireWeapon(ship, "WS0005", target); // PD volley missiles
            fireWeapon(ship, "WS0006", target); // Get off me / Get away
        }
    }

    private static void fireWeapon(ShipAPI ship, String weaponSlotId, ShipAPI target) {
        WeaponAPI weapon = getWeaponBySlot(ship, weaponSlotId);
        if (weapon != null && weapon.getCooldownRemaining() <= 0) {
            // Check if the target is within the weapon's firing arc
            Vector2f targetPoint = target.getLocation();
            float angleToTarget = VectorUtils.getAngle(ship.getLocation(), targetPoint);
            float weaponArc = weapon.getArc();

            if (Math.abs(MathUtils.getShortestRotation(weapon.getCurrAngle(), angleToTarget)) <= weaponArc / 2) {
                // Fire the weapon if the target is within the firing arc
                weapon.setForceFireOneFrame(true);
            } else if (weaponArc < 10) { // If the weapon has a narrow arc, rotate the ship
                smoothRotateToAngle(ship, angleToTarget, Global.getCombatEngine().getElapsedInLastFrame());
            }
        }
    }

    private static WeaponAPI getWeaponBySlot(ShipAPI ship, String slotId) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSlot().getId().equals(slotId)) {
                return weapon;
            }
        }
        return null;
    }

    private static void applyDynamicMovements(ShipAPI ship, CombatEngineAPI engine) {
        // Example dynamic movement: Rotate to face the nearest target
        List<CombatEntityAPI> targets = getTargetsInRange(engine, ship.getLocation(), SCAN_RADIUS);
        if (!targets.isEmpty()) {
            CombatEntityAPI nearestTarget = targets.get(0);
            float nearestDistance = MathUtils.getDistance(ship, nearestTarget);

            for (CombatEntityAPI target : targets) {
                float distance = MathUtils.getDistance(ship, target);
                if (distance < nearestDistance) {
                    nearestTarget = target;
                    nearestDistance = distance;
                }
            }

            float targetAngle = VectorUtils.getAngle(ship.getLocation(), nearestTarget.getLocation());
            smoothRotateToAngle(ship, targetAngle, engine.getElapsedInLastFrame());
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