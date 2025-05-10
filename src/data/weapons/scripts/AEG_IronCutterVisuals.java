package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_IronCutterVisuals {

    private static final float RIPPLE_DURATION = 3f;
    private static final float LIGHTNING_INTERVAL = 0.5f;

    private RippleDistortion ripple;
    private float rippleElapsed = 0f;
    private float lightningTimer = 0f;

    public void update(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;

        float chargeLevel = weapon.getChargeLevel();

        // Update ripple effect
        updateRippleEffect(amount, engine, weapon);

        // Handle lightning arcs
        handleLightning(engine, weapon, chargeLevel, amount);

        // Generate energy mist / nebula buildup
        generateNebula(engine, weapon, chargeLevel);
    }



    public void updateRippleEffect(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;

        rippleElapsed += amount;

        if (ripple == null || rippleElapsed > RIPPLE_DURATION) {
            ripple = new RippleDistortion(weapon.getLocation(), new Vector2f());
            ripple.setSize(50f + 100f * weapon.getChargeLevel());
            ripple.setIntensity(0.2f + 0.5f * weapon.getChargeLevel());
            ripple.setLifetime(RIPPLE_DURATION);
            ripple.fadeInSize(0.3f);
            ripple.fadeOutIntensity(0.5f);
            ripple.setFrameRate(60f);
            ripple.setArc(0f, 360f);

            DistortionShader.addDistortion(ripple);
            rippleElapsed = 0f;
        }
    }


    private void handleLightning(CombatEngineAPI engine, WeaponAPI weapon, float chargeLevel, float amount) {
        lightningTimer += amount;

        if (lightningTimer >= LIGHTNING_INTERVAL) {
            lightningTimer = 0f;

            Color lightningColor = new Color(
                    (int)(100 + 155 * chargeLevel), 0, 0,
                    (int)(100 + 155 * chargeLevel)
            );

            Vector2f loc = weapon.getLocation();
            Vector2f target = Misc.getPointAtRadius(loc, (float)(Math.random() * 50f + 30f));

            engine.spawnEmpArcVisual(
                    loc,
                    weapon.getShip(),
                    target,
                    null,
                    10f ,
                    lightningColor,
                    lightningColor
            );
        }
    }

    private void generateNebula(CombatEngineAPI engine, WeaponAPI weapon, float chargeLevel) {
        Vector2f loc = weapon.getLocation();

        float size = 20f + 80f * chargeLevel - MathUtils.getRandom().nextInt(15);
        float endSizeMult = 1.5f + 0.5f * chargeLevel;
        float duration = 1.5f + chargeLevel;

        engine.addNebulaSmoothParticle(
                loc,
                new Vector2f(0f, 0f),
                size,
                endSizeMult,
                0.3f,
                0.6f,
                duration,
                getNebulaColor()
        );
    }

    private Color getNebulaColor() {
        return new Color(255, 50 + MathUtils.getRandom().nextInt(180), 0, 150 + MathUtils.getRandom().nextInt(100)); // Red energy mist
    }

}
