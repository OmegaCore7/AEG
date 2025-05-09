package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

public class AEG_RustHurricaneChargeEffect implements EveryFrameWeaponEffectPlugin {
    private float nextSoundInterval = 0.8f;
    private float soundTimer = 0f;
    private float ripplePulseTimer = 0f;
    private int soundStage = 0;
    private float timer = 0f;
    private boolean hasFullyCharged = false;
    private final Random rand = new Random();
    private final float MAX_CHARGE_DURATION = 6f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getChargeLevel() <= 0f) return;

        Vector2f muzzle = weapon.getFirePoint(0);
        if (muzzle == null) muzzle = weapon.getLocation();

        timer += amount;

        soundTimer += amount;

        if (soundTimer >= nextSoundInterval) {
            float pitch = 1f + (weapon.getChargeLevel() * 0.5f); // subtle pitch shift
            float volume = 0.5f + (weapon.getChargeLevel() * 0.5f); // build volume

            playChargeSound(weapon, volume, pitch);
            soundTimer = 0f;

            // As it charges, sounds get more frequent
            nextSoundInterval = Math.max(0.1f, 0.8f - weapon.getChargeLevel() * 0.7f);
        }

        // PARTICLES
        spawnChargingParticles(engine, weapon, muzzle);

        // FULL CHARGE EFFECT
        if (weapon.getChargeLevel() >= 1f && !hasFullyCharged) {
            spawnDispersalTransition(engine, muzzle);
            Global.getSoundPlayer().playSound("realitydisruptor_fire", 1f, 1f, muzzle, weapon.getShip().getVelocity());
            engine.addHitParticle(muzzle, new Vector2f(), 40f, 1f, 0.4f, new Color(200, 255, 255, 255));
            hasFullyCharged = true;
        }
        // RESET AFTER FIRING
        if (weapon.getChargeLevel() < 0.05f && hasFullyCharged) {
            hasFullyCharged = false;
            timer = 0f;
            soundTimer = 0f;
            soundStage = 0;
        }
    }

    private void spawnChargingParticles(CombatEngineAPI engine, WeaponAPI weapon, Vector2f muzzle) {
        // Dark pull particles
        for (int i = 0; i < 6; i++) {
            Vector2f offset = new Vector2f((rand.nextFloat() - 0.5f) * 60f, (rand.nextFloat() - 0.5f) * 60f);
            Vector2f spawn = Vector2f.add(muzzle, offset, null);
            Vector2f toMuzzle = Vector2f.sub(muzzle, spawn, null);
            toMuzzle.normalise();
            toMuzzle.scale(40f + rand.nextFloat() * 20f);

            engine.addSmoothParticle(
                    spawn, toMuzzle,
                    10f + rand.nextFloat() * 5f,
                    1f,
                    0.6f,
                    new Color(30 + rand.nextInt(30), 0, 60 + rand.nextInt(60), 150)
            );
        }

        // Subtle core glow with hue shifting
        float hueShift = (timer * 30f) % 360f;
        Color pulsingColor = Color.getHSBColor(hueShift / 360f, 1f, 1f);

        engine.addNebulaParticle(
                muzzle,
                new Vector2f((rand.nextFloat() - 0.5f) * 15f, (rand.nextFloat() - 0.5f) * 15f),
                15f + rand.nextFloat() * 20f,
                1.6f,
                0f, 0.1f, 0.6f,
                new Color(pulsingColor.getRed(), 0, pulsingColor.getBlue(), 120),
                true
        );

        // EMP arcs crackling
        if (rand.nextFloat() < 0.1f) {
            for (int i = 0; i < 2; i++) {
                Vector2f from = new Vector2f(
                        muzzle.x + (rand.nextFloat() - 0.5f) * 60f,
                        muzzle.y + (rand.nextFloat() - 0.5f) * 60f
                );
                Vector2f to = new Vector2f(
                        muzzle.x + (rand.nextFloat() - 0.5f) * 60f,
                        muzzle.y + (rand.nextFloat() - 0.5f) * 60f
                );
                engine.spawnEmpArcVisual(
                        from, weapon.getShip(),   // from location & entity
                        to, weapon.getShip(),     // to location & entity
                        2f,                       // thickness
                        new Color(200 - rand.nextInt(100), 0, 100 - rand.nextInt(100)),   // fringe
                        new Color(255 - rand.nextInt(75), 0, 150 - rand.nextInt(75))    // core
                );
            }
        }

        // Peripheral flickers
        for (int i = 0; i < 3; i++) {
            Vector2f flickerPos = new Vector2f(
                    muzzle.x + (float) Math.cos(rand.nextFloat() * 2 * Math.PI) * 40f,
                    muzzle.y + (float) Math.sin(rand.nextFloat() * 2 * Math.PI) * 40f
            );
            engine.addHitParticle(flickerPos, new Vector2f(), 3f, 0.8f, 0.15f,
                    new Color(180, 20 + rand.nextInt(40), 60 + rand.nextInt(80), 180));
        }
    }

    private void spawnDispersalTransition(CombatEngineAPI engine, Vector2f muzzle) {
        // Chaotic nebula burst
        for (int i = 0; i < 12; i++) {
            Vector2f vel = new Vector2f((rand.nextFloat() - 0.5f) * 200f, (rand.nextFloat() - 0.5f) * 200f);
            Color riftColor = new Color(80 - rand.nextInt(70), 0, 80 - rand.nextInt(60), 200 - rand.nextInt(50));
            engine.addNebulaParticle(
                    muzzle,
                    vel,
                    60f + rand.nextFloat() * 100f,
                    2f + rand.nextFloat(),
                    0f,
                    0.3f,
                    1.5f,
                    riftColor,
                    true
            );
        }

        // Vortex swirl outward
        for (int i = 0; i < 10; i++) {
            float angle = rand.nextFloat() * 360f;
            float speed = 30f + rand.nextFloat() * 50f;
            float radians = (float) Math.toRadians(angle);
            Vector2f vel = new Vector2f(
                    (float) Math.cos(radians) * speed,
                    (float) Math.sin(radians) * speed
            );

            engine.addSmoothParticle(
                    muzzle,
                    vel,
                    8f + rand.nextFloat() * 10f,
                    1f,
                    1.5f,
                    new Color(150 + rand.nextInt(60), 0, 30 + rand.nextInt(60), 160)
            );
        }

        // Ripple distortion burst
        RippleDistortion ripple = new RippleDistortion(muzzle, new Vector2f());
        ripple.setSize(300f);
        ripple.setIntensity(35f);
        ripple.setLifetime(0.7f);
        ripple.fadeOutIntensity(0.5f);
        DistortionShader.addDistortion(ripple);
    }

    private void playChargeSound(WeaponAPI weapon, float volume, float pitch) {
        Vector2f muzzle = weapon.getFirePoint(0);
        if (muzzle == null) muzzle = weapon.getLocation();

        Global.getSoundPlayer().playSound(
                "system_high_energy_focus_activate",
                pitch,
                volume,
                muzzle,
                weapon.getShip().getVelocity()
        );
    }
}
