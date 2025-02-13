package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;
import java.awt.Color;

public class AEG_4g_right_brokenmagnumEffect implements EveryFrameWeaponEffectPlugin {
    private static final float CHARGE_UP_TIME = 1f; // 1 second charge-up time
    private static final float EXTENDED_TIME = 0.33f; // 0.33 seconds extended time after firing
    private static final float RETRACT_DISTANCE = -10f;
    private static final float EXTEND_DISTANCE = 25f;

    private float chargeUpProgress = 0f;
    private float extendedTimeProgress = 0f;
    private boolean isCharging = false;
    private boolean isExtended = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        if (weapon.getChargeLevel() > 0) {
            isCharging = true;
            chargeUpProgress += amount;
            if (chargeUpProgress > CHARGE_UP_TIME) {
                chargeUpProgress = CHARGE_UP_TIME;
            }
        } else if (isCharging) {
            isCharging = false;
            isExtended = true;
            extendedTimeProgress = 0f;
            chargeUpProgress = 0f;
            spawnSmoke(engine, weapon);
        }

        if (isExtended) {
            extendedTimeProgress += amount;
            if (extendedTimeProgress > EXTENDED_TIME) {
                isExtended = false;
            }
        }

        float progress = isExtended ? 1f : chargeUpProgress / CHARGE_UP_TIME;
        float brokenMagnumOffset = interpolate(RETRACT_DISTANCE, EXTEND_DISTANCE, progress);

        // Apply transformation
        applyTransformation(weapon, brokenMagnumOffset, 0);
    }

    private float interpolate(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    private void applyTransformation(WeaponAPI weapon, float offsetX, float offsetY) {
        Vector2f location = weapon.getLocation();
        location.x += offsetX;
        location.y += offsetY;
    }

    private void spawnSmoke(CombatEngineAPI engine, WeaponAPI weapon) {
        Vector2f smokeLocation = weapon.getFirePoint(0);
        engine.addSmokeParticle(smokeLocation, new Vector2f(0, 0), 50f, 1f, 0.5f, new Color(100, 100, 100, 150));
    }
}