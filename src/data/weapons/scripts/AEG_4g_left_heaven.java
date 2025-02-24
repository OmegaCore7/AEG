package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;

public class AEG_4g_left_heaven implements EveryFrameWeaponEffectPlugin {

    private static final Color CHARGEUP_COLOR = new Color(255, 255, 0, 255); // Yellow color
    private static final Color ACTIVE_COLOR = new Color(0, 255, 0, 255); // Green color
    private boolean runOnce = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip() == null) {
            return;
        }

        if (!runOnce) {
            init(weapon);
            runOnce = true;
        }

        ShipAPI ship = weapon.getShip();
        float chargeLevel = weapon.getChargeLevel();

        if (ship.getSystem() != null) {
            ShipSystemAPI system = ship.getSystem();
            if (system.isActive()) {
                weapon.getAnimation().setFrame(1); // Use frame 4g_heaven_arml_01
                if (system.isChargeup()) {
                    weapon.getSprite().setColor(CHARGEUP_COLOR); // Change color to yellow during charge-up
                } else {
                    weapon.getSprite().setColor(ACTIVE_COLOR); // Change color to green when active
                }
            } else {
                weapon.getAnimation().setFrame(0); // Use frame 4g_heaven_arml_00
                weapon.getSprite().setColor(Color.WHITE); // Reset color to white when system is off
            }
        } else {
            weapon.getAnimation().setFrame(0); // Use frame 4g_heaven_arml_00
            weapon.getSprite().setColor(Color.WHITE); // Reset color to white when system is off
        }
    }

    private void init(WeaponAPI weapon) {
        // Initialization logic if needed
    }
}