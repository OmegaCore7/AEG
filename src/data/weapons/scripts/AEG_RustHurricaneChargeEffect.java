package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.distortion.WaveDistortion;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

import static java.util.logging.Logger.global;

public class AEG_RustHurricaneChargeEffect implements EveryFrameWeaponEffectPlugin {
    private float soundTimer = 0f;
    private float ripplePulseTimer = 0f;
    private int soundStage = 0;
    private WaveDistortion waveDistortion = null;
    private boolean started = false;
    private float timer = 0f;
    private RippleDistortion primaryDistortion = null;
    private RippleDistortion secondaryDistortion = null;
    private final float MAX_DURATION = 6f;
    private Random rand = new Random();
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getChargeLevel() <= 0f) return;
        Vector2f muzzle = weapon.getFirePoint(0); // muzzle position
        timer += amount;
        ripplePulseTimer += amount;
        if (ripplePulseTimer >= 1.0f) {
            ripplePulseTimer = 0f;

            WaveDistortion ripple = new WaveDistortion(muzzle, new Vector2f(0, 0));
            ripple.setSize(200f);
            ripple.setIntensity(20f);      // sharp, visible effect
            ripple.setLifetime(0.4f);      // short burst
            ripple.setArc(0f, 360f);
            ripple.fadeOutIntensity(0.6f);
            // No setInvert(), simulate inward with timing/layering
            DistortionShader.addDistortion(ripple);
        }
        soundTimer += amount;

        if (soundStage == 0 && soundTimer >= 0f) {
            playChargeSound(weapon, 0.6f); // first, quiet
            soundStage++;
        }
        if (soundStage == 1 && soundTimer >= 2f) {
            playChargeSound(weapon, 0.8f); // medium
            soundStage++;
        }
        if (soundStage == 2 && soundTimer >= 4f) {
            playChargeSound(weapon, 1.0f); // full volume
            soundStage++;
        }

        if (muzzle == null) {
            muzzle = weapon.getLocation(); // fallback
        }

        // Cosmic vortex particles — simulating space "suck-in"
        Vector2f pullVelocity = Vector2f.sub(muzzle, weapon.getLocation(), null);
        pullVelocity.scale(0.3f);
        for (int i = 0; i < 6; i++) {
            Vector2f offset = new Vector2f((rand.nextFloat() - 0.5f) * 60f, (rand.nextFloat() - 0.5f) * 60f);
            Vector2f spawn = Vector2f.add(muzzle, offset, null);

            Vector2f toMuzzle = Vector2f.sub(muzzle, spawn, null);
            toMuzzle.normalise();
            toMuzzle.scale(40f + rand.nextFloat() * 20f);

            Color particleColor = new Color(50 - rand.nextInt(50), 0, 150 - rand.nextInt(100), 100 + rand.nextInt(100));

            engine.addSmoothParticle(
                    spawn,
                    toMuzzle,
                    10f + rand.nextFloat() * 5f,
                    1f,
                    0.5f + rand.nextFloat() * 0.3f,
                    particleColor
            );
        }


        // Subtle glow pulse as charge builds
        float alpha = weapon.getChargeLevel();
        engine.addNebulaParticle(
                muzzle,
                new Vector2f((rand.nextFloat() - 0.5f) * 20f, (rand.nextFloat() - 0.5f) * 20f),
                10f + rand.nextFloat() * 20f,
                1.5f,
                0f,
                0.1f,
                0.6f,
                new Color(60, 0, 20, 100),
                true
        );
        for (int i = 0; i < 3; i++) {
            Vector2f flickerPos = new Vector2f(
                    muzzle.x + (float) Math.cos(rand.nextFloat() * 2 * Math.PI) * 35f,
                    muzzle.y + (float) Math.sin(rand.nextFloat() * 2 * Math.PI) * 35f
            );
            engine.addHitParticle(flickerPos, new Vector2f(), 3f, 0.8f, 0.1f, new Color(180 - rand.nextInt(100), 80 + rand.nextInt(80), 255, 180 + rand.nextInt(75)));
        }
        if (weapon.getChargeLevel() > 0.1f) {
            for (int i = 0; i < 2; i++) {
                engine.addNebulaParticle(
                        muzzle,
                        new Vector2f((rand.nextFloat() - 0.5f) * 60f, (rand.nextFloat() - 0.5f) * 60f),
                        50f + rand.nextFloat() * 60f,
                        2f + rand.nextFloat(),
                        0f,
                        0.2f,
                        0.8f,
                        new Color(
                                20 + rand.nextInt(120),   // red
                                0,                       // green
                                60 + rand.nextInt(60),  // blue
                                100 + rand.nextInt(20)   // alpha
                        )
                );
            }

            engine.addNebulaParticle(
                    muzzle,
                    new Vector2f(),
                    140f,
                    2.4f,
                    0f,
                    0.2f + rand.nextInt(2),
                    1.2f + rand.nextInt(2),
                    new Color(10, 10, 10, 180),
                    true
            );
        }
        for (int i = 0; i < 3; i++) {
            Vector2f ringPos = new Vector2f(
                    muzzle.x + (float) Math.cos(rand.nextFloat() * 2 * Math.PI) * (50f + rand.nextFloat() * 20f),
                    muzzle.y + (float) Math.sin(rand.nextFloat() * 2 * Math.PI) * (50f + rand.nextFloat() * 20f)
            );
            engine.addHitParticle(
                    ringPos,
                    new Vector2f(),
                    6f + rand.nextFloat() * 4f,
                    1.2f,
                    0.2f + rand.nextFloat() * 0.2f,
                    new Color(200, 0, 255, 120 + rand.nextInt(100))
            );
        }
    }
    private void playChargeSound(WeaponAPI weapon, float volume) {
        Vector2f muzzle = weapon.getFirePoint(0);
        if (muzzle == null) muzzle = weapon.getLocation();

        Global.getSoundPlayer().playSound(
                "gigacannon_charge", // same sound
                1f,                  // pitch
                volume,             // dynamic volume
                muzzle,
                weapon.getShip().getVelocity()
        );
    }
}
