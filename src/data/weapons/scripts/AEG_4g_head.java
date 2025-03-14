package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import java.awt.Color;

public class AEG_4g_head implements EveryFrameWeaponEffectPlugin {

    private static final int NUM_FRAMES = 9;  // Total number of frames
    private int currentFrame = 0;  // Current frame index
    private boolean shipSystemActive = false;  // Flag for ship system status

    // List of weapon slots to be updated
    private static final String[] weaponSlots = {
            "AEG_4g_torso", "AEG_4g_legs", "AEG_4g_left_shoulder", "AEG_4g_right_shoulder", "AEG_4g_head",
            "AEG_4g_left_punch", "AEG_4g_right_punch", "AEG_4g_right_willknife", "AEG_4g_left_boltingdriver",
            "AEG_4g_left_protectshade", "AEG_4g_right_brokenmagnum", "AEG_4g_rightkneedrill"
    };

    // List of hidden weapon slots
    private static final String[] hiddenWeaponSlots = {
            "AEG_4g_left_punch", "AEG_4g_right_punch", "AEG_4g_right_willknife", "AEG_4g_left_boltingdriver",
            "AEG_4g_left_protectshade", "AEG_4g_right_brokenmagnum", "AEG_4g_rightkneedrill"
    };

    // Method to set the visibility (alpha) of weapon slots
    private void setWeaponVisibility(ShipAPI ship, String weaponId, boolean visible) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSlot().getId().equals(weaponId)) {
                weapon.getSprite().setAlphaMult(visible ? 1.0f : 0.0f);  // Set alpha to 1 for visible, 0 for invisible
            }
        }
    }

    // Method to simulate changing the weapon color
    private void setWeaponColor(ShipAPI ship, String weaponId, Color color) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSlot().getId().equals(weaponId)) {
                weapon.getSprite().setColor(color);  // Set the weapon color
            }
        }
    }

    // Method to update all weapon colors when ship system is active
    private void updateWeaponColors(ShipAPI ship) {
        Color activeColor = new Color(0, 255, 0);  // Green color
        Color defaultColor = new Color(255, 255, 255);  // White color

        for (String weaponId : weaponSlots) {
            if (!isWeaponHidden(weaponId)) {
                setWeaponColor(ship, weaponId, shipSystemActive ? activeColor : defaultColor);
            }
        }
    }

    // Simulating the update method for weapon frame progress and system checks
    private void updateWeaponState(ShipAPI ship) {
        // Play through the 9 frames regardless of weapon state
        currentFrame = (currentFrame + 1) % NUM_FRAMES;

        // Check the ship system status and update weapon colors
        updateWeaponColors(ship);

        // Set visibility for hidden weapons (alpha to 0)
        for (String weaponId : hiddenWeaponSlots) {
            setWeaponVisibility(ship, weaponId, false);  // Set these weapons to be invisible
        }

        // Set visibility for visible weapons (alpha to 1)
        for (String weaponId : weaponSlots) {
            if (!isWeaponHidden(weaponId)) {
                setWeaponVisibility(ship, weaponId, true);  // Set these weapons to be visible
            }
        }
    }

    // Helper method to determine if a weapon should be hidden
    private boolean isWeaponHidden(String weaponId) {
        for (String hiddenWeapon : hiddenWeaponSlots) {
            if (hiddenWeapon.equals(weaponId)) {
                return true;  // If the weapon is in the hidden list
            }
        }
        return false;
    }

    // Placeholder method to check if the ship system is active (excluding charge-up)
    public void setShipSystemActive(boolean active) {
        this.shipSystemActive = active;
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null) return;

        // Update weapon state every frame
        updateWeaponState(ship);
    }
}