package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.dark.shaders.distortion.DistortionShader;

import java.awt.*;

public class AEG_RustHurricaneChargeEffect implements EveryFrameWeaponEffectPlugin {
    private boolean started = false;
    private float timer = 0f;
    private DistortionShader distortion;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getChargeLevel() <= 0f) return;

        timer += amount;

        // Start only once
        if (!started && weapon.getChargeLevel() > 0.1f) {
            started = true;

            // Begin shader distortion effect
            distortion = ShaderLib.createRippleDistortion(weapon.getLocation());
            distortion.setIntensity(0.5f);
            distortion.setSize(200f);
            distortion.fadeIn(0.5f);
            ShaderLib.addDistortion(distortion);
        }

        // Color pulse effect
        if (engine.getPlayerShip() == weapon.getShip()) {
            float alpha = weapon.getChargeLevel();
            Color pulseColor = new Color(120, 0, 160, (int)(alpha * 255)); // Violet core
            engine.addSmoothParticle(
                    weapon.getLocation(),
                    weapon.getShip().getVelocity(),
                    60f + 40f * alpha,
                    1.5f,
                    0.1f + 0.2f * alpha,
                    pulseColor
            );
        }

        // Ramp up the shader intensity over time
        if (distortion != null) {
            distortion.setIntensity(Math.min(1f, timer / 6f));
            distortion.setSize(250f + timer * 25f); // Expands while charging
        }

        // Final black void pulse near full charge
        if (weapon.getChargeLevel() > 0.85f) {
            engine.addNebulaParticle(
                    weapon.getLocation(),
                    weapon.getShip().getVelocity(),
                    80f,
                    2f,
                    0f,
                    0.1f,
                    1.2f,
                    new Color(0, 0, 0, 160), // Shadow void
                    true
            );
        }
    }
}
