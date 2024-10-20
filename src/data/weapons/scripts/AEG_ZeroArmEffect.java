package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;

public class AEG_ZeroArmEffect implements EveryFrameWeaponEffectPlugin {
    private boolean isTransparent = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        // Get the linked weapon
        WeaponAPI ironcutter = findWeaponById(weapon.getShip(), "AEG_ironcutter_l");

        if (ironcutter == null) {
            return;
        }

        // Sync aiming
        ironcutter.setCurrAngle(weapon.getCurrAngle());

        // Check if AEG_zero_arm_l has finished firing
        if (weapon.getChargeLevel() == 0 && weapon.isFiring()) {
            // Force fire AEG_ironcutter_l
            forceFireWeapon(weapon.getShip(), ironcutter);

            // Make AEG_zero_arm_l transparent
            isTransparent = true;
            weapon.getSprite().setColor(new Color(1, 1, 1, 0.5f));
        }

        // Check if AEG_ironcutter_l has finished firing
        if (ironcutter.getCooldownRemaining() <= 0) {
            // Make AEG_zero_arm_l opaque again
            isTransparent = false;
            weapon.getSprite().setColor(new Color(1, 1, 1, 1));
        }

        // Prevent AEG_zero_arm_l from firing while transparent
        if (isTransparent) {
            weapon.setRemainingCooldownTo(weapon.getCooldown());
        }
    }

    private WeaponAPI findWeaponById(ShipAPI ship, String weaponId) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getId().equals(weaponId)) {
                return weapon;
            }
        }
        return null;
    }

    private void forceFireWeapon(ShipAPI ship, WeaponAPI weapon) {
        // Logic to force fire the weapon
        if (weapon.getCooldownRemaining() <= 0) {
            ship.giveCommand(ShipCommand.FIRE, weapon.getLocation(), 0);
        }
    }
}
