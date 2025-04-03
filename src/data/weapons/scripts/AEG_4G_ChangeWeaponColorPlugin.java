package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import java.awt.Color;

public class AEG_4G_ChangeWeaponColorPlugin implements EveryFrameWeaponEffectPlugin {

    private static final Color ACTIVE_COLOR = new Color(0, 255, 0, 255); // Green color for active
    private boolean systemActive = false;
    private float timeElapsed = 0f; // Track elapsed time for system activation

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip() == null) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        ShipSystemAPI system = ship.getSystem();

        if (system == null || (!system.isChargeup() && !system.isActive())) {
            systemActive = false; // Reset system active flag
            timeElapsed = 0f; // Reset elapsed time
            weapon.getSprite().setColor(Color.WHITE); // Default white when system is off
            return; // Return if the system is not activated
        }

        if (system.isActive()) {
            if (!systemActive) {
                systemActive = true;
                timeElapsed = 0f; // Reset the timer when the system becomes active
            }
            timeElapsed += amount;
            if (timeElapsed >= 4f) {
                weapon.getSprite().setColor(ACTIVE_COLOR); // Green when active
            }
        } else {
            weapon.getSprite().setColor(Color.WHITE); // Default white when system is off
            systemActive = false; // Reset the state when the system is off
            timeElapsed = 0f; // Reset the timer when the system is off
        }
    }
}