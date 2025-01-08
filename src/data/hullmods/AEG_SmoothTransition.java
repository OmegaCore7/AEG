package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.ArrayList;
import java.util.List;

public class AEG_SmoothTransition implements EveryFrameCombatPlugin {
    private List<WeaponAPI> leftArmWeapons = new ArrayList<>();
    private List<WeaponAPI> rightArmWeapons = new ArrayList<>();
    private WeaponAPI currentLeftWeapon;
    private WeaponAPI currentRightWeapon;
    private boolean initialized = false;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!initialized) {
            initialize(engine);
            initialized = true;
        }

        // Implement gradual alpha transition logic here
        updateWeaponStates();
    }

    private void initialize(CombatEngineAPI engine) {
        for (CombatEntityAPI entity : engine.getShips()) {
            if (entity instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) entity;
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    if (isLeftArmWeapon(weapon)) {
                        leftArmWeapons.add(weapon);
                    } else if (isRightArmWeapon(weapon)) {
                        rightArmWeapons.add(weapon);
                    }
                }

                // Ensure one weapon from each set is visible
                if (!leftArmWeapons.isEmpty()) {
                    currentLeftWeapon = leftArmWeapons.get(0);
                    currentLeftWeapon.getSprite().setAlphaMult(1.0f);
                }
                if (!rightArmWeapons.isEmpty()) {
                    currentRightWeapon = rightArmWeapons.get(0);
                    currentRightWeapon.getSprite().setAlphaMult(1.0f);
                }
            }
        }
    }

    private boolean isLeftArmWeapon(WeaponAPI weapon) {
        // Check if the weapon ID matches the left arm weapon IDs
        String slotId = weapon.getSlot().getId();
        return slotId.equals("AEG_4g_armleft1") || slotId.equals("AEG_4g_armleft2") || slotId.equals("AEG_4g_armleft3");
    }

    private boolean isRightArmWeapon(WeaponAPI weapon) {
        // Check if the weapon ID matches the right arm weapon IDs
        String slotId = weapon.getSlot().getId();
        return slotId.equals("AEG_4g_armright1") || slotId.equals("AEG_4g_armright2") || slotId.equals("AEG_4g_armright3");
    }

    private void switchWeapon(WeaponAPI newWeapon, WeaponAPI currentWeapon) {
        // Implement gradual alpha transition from currentWeapon to newWeapon
        float transitionDuration = 1.0f; // Duration in seconds
        float elapsedTime = 0.0f;

        while (elapsedTime < transitionDuration) {
            float alpha = elapsedTime / transitionDuration;
            currentWeapon.getSprite().setAlphaMult(1.0f - alpha);
            newWeapon.getSprite().setAlphaMult(alpha);
            elapsedTime += 0.1f; // Adjust the increment as needed
        }

        currentWeapon.getSprite().setAlphaMult(0.0f);
        newWeapon.getSprite().setAlphaMult(1.0f);
    }

    private void updateWeaponStates() {
        for (WeaponAPI weapon : leftArmWeapons) {
            if (weapon.getSprite().getAlphaMult() == 0.0f) {
                weapon.disable(false);
            }
        }
        for (WeaponAPI weapon : rightArmWeapons) {
            if (weapon.getSprite().getAlphaMult() == 0.0f) {
                weapon.disable(false);
            }
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        // Initialization logic if needed
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
        // Implement input processing logic here if needed
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
        // Implement rendering logic here if needed
    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {
        // Implement UI rendering logic here if needed
    }
}