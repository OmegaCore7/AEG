package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;

public class AEG_4g_protectEffect implements EveryFrameWeaponEffectPlugin {
    private int frameIndex = 0;
    private float frameDuration = 0.1f; // Duration for each frame
    private float timeSinceLastFrame = 0f;
    private boolean chargingUp = true;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        float chargeLevel = weapon.getChargeLevel();
        timeSinceLastFrame += amount;

        if (timeSinceLastFrame >= frameDuration) {
            timeSinceLastFrame = 0f;

            if (chargeLevel > 0 && chargeLevel < 1) {
                // Charge up phase
                chargingUp = true;
                frameIndex = Math.min((int) (chargeLevel * 5), 4);
            } else if (chargeLevel == 1) {
                // Firing phase
                frameIndex = 4;
            } else if (chargeLevel < 0) {
                // Charge down phase
                chargingUp = false;
                frameIndex = Math.max((int) ((1 + chargeLevel) * 5), 0);
            }

            weapon.getAnimation().setFrame(frameIndex);
        }
    }
}