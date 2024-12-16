package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;

public class AEG_SparkEffect implements EveryFrameWeaponEffectPlugin {
    private float previousAngle = 0f;
    private int currentFrame = 0;
    private float frameTime = 0f;
    private final float frameDuration = 0.1f; // Duration of each frame in seconds

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        float currentAngle = weapon.getCurrAngle();
        if (currentAngle != previousAngle) {
            // Weapon is moving
            frameTime += amount;
            if (frameTime >= frameDuration) {
                frameTime = 0f;
                currentFrame++;
                if (currentFrame > 12) {
                    currentFrame = 1; // Skip frame 00
                }
            }
        } else {
            // Weapon is idle
            currentFrame = 0;
        }

        weapon.getAnimation().setFrame(currentFrame);
        previousAngle = currentAngle;
    }
}