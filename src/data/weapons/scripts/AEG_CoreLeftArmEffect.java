package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class AEG_CoreLeftArmEffect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private Vector2f initialOffset = new Vector2f(37f, 8f); // Initial offset relative to the ship's facing
    private float maxDistancePositive = 2f; // Maximum positive distance the arm weapon can move
    private float maxDistanceNegative = -2f; // Maximum negative distance the arm weapon can move
    private float movementSpeed = 50f; // Speed at which the arm weapon moves

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || engine.isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        WeaponAPI decoGun = null;

        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().getId().equals("WS0006")) { // Deco gun
                decoGun = w;
                break;
            }
        }

        if (decoGun != null) {
            // Get the current angle of the deco gun and ship
            float decoGunAngle = decoGun.getCurrAngle();
            float shipFacing = ship.getFacing();

            // Calculate the direction of movement based on the difference between the deco gun's angle and the ship's facing
            float angleDifference = decoGunAngle - shipFacing;

            // Get the current position of the arm (relative to the ship's position)
            Vector2f currentArmPosition = weapon.getSlot().getLocation();

            // Move the arm based on the angle difference
            if (angleDifference > 0 && currentArmPosition.y < initialOffset.y + maxDistancePositive) {
                // Move the arm upwards
                currentArmPosition.y += movementSpeed * amount;
            } else if (angleDifference < 0 && currentArmPosition.y > initialOffset.y + maxDistanceNegative) {
                // Move the arm downwards
                currentArmPosition.y -= movementSpeed * amount;
            }

            // Ensure the arm position stays within bounds
            currentArmPosition.y = Math.min(Math.max(currentArmPosition.y, initialOffset.y + maxDistanceNegative), initialOffset.y + maxDistancePositive);

            // Update the arm weapon's position
            weapon.getSlot().getLocation().set(currentArmPosition);
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        // No specific action on fire
    }
}