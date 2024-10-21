package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;

public class AEG_ZeroArmEffect implements EveryFrameWeaponEffectPlugin {
    private boolean fired = false;
    private float timer = 0f;
    private static final float INVISIBILITY_DURATION = 10f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        // Check if the weapon is the specific one we want to alter
        if (!"AEG_zero_arm_r".equals(weapon.getId())) {
            return;
        }

        // Check if the weapon has fired
        if (weapon.getCooldownRemaining() > 0 && !fired) {
            fired = true;
            timer = INVISIBILITY_DURATION;
            weapon.getSprite().setAlphaMult(0f); // Make the weapon invisible
        }

        // If the weapon has fired, count down the timer
        if (fired) {
            timer -= amount;
            if (timer <= 0) {
                fired = false;
                weapon.getSprite().setAlphaMult(1f); // Make the weapon visible again
            }
        }
    }
}
