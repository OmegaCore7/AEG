package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class AEG_4g_GoldionCrusherVisuals implements BeamEffectPlugin {
    private boolean isChargingUp = false;
    private float chargeUpTimer = 0f;
    private static final float CHARGE_UP_DURATION = 2f;
    private float passiveTimer = 0f;

    private static final float RING_INTERVAL = 1.5f; // seconds between rings
    private static final float RING_MAX_RADIUS = 1000f;
    private static final float RING_THICKNESS = 10f;

    private float ringTimer = 0f;
    private float currentRingRadius = 0f;

    private float shieldHitCooldown = 0f;
    private float hullHitDuration = 0f;
    private boolean finalBurstTriggered = false;


    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine == null || beam == null || beam.getWeapon() == null || beam.getWeapon().getShip() == null) return;
        ShipAPI ship = beam.getWeapon().getShip();

        Vector2f from = beam.getFrom();
        Vector2f to = beam.getTo();
        Vector2f velocity = ship.getVelocity();
        Vector2f center = new Vector2f((from.x + to.x) / 2f, (from.y + to.y) / 2f);

        // === PASSIVE EFFECT when selected but not firing ===
        ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();
        spawnPassiveVisuals(engine, center, velocity, passiveTimer);


        // === CHARGE-UP PHASE ===
        if (beam.getWeapon().getChargeLevel() > 0f) {
            if (!isChargingUp) {
                isChargingUp = true;
                chargeUpTimer = 0f;
            } else {
                chargeUpTimer += amount;
                spawnChargeUpVisuals(engine, center, velocity, chargeUpTimer / CHARGE_UP_DURATION);
            }

            if (chargeUpTimer < CHARGE_UP_DURATION) {
                return; // delay shockwave effects until charge-up completes
            }
        } else {
            // Reset charge state
            isChargingUp = false;
            chargeUpTimer = 0f;
        }

        // === RING PULSE EFFECTS ===
        ringTimer += amount;
        if (ringTimer >= RING_INTERVAL) {
            ringTimer -= RING_INTERVAL;
            currentRingRadius = 0f;
        }
        if (currentRingRadius < RING_MAX_RADIUS) {
            currentRingRadius += RING_MAX_RADIUS * (amount / RING_INTERVAL);
        }

        float intensity = 1f - (currentRingRadius / RING_MAX_RADIUS);
        if (intensity < 0f) intensity = 0f;

        spawnGoldenParticleRing(engine, center, velocity, currentRingRadius, intensity);

        // Heat distortion pulse on new wave
        if (currentRingRadius == 0f && !engine.isPaused()) {
            WaveDistortion wave = new WaveDistortion(center, ship.getVelocity());
            wave.setIntensity(30f);
            wave.setSize(300f);
            wave.setArc(0, 360f);
            wave.fadeInSize(0.2f);
            wave.fadeOutIntensity(1f);
            wave.setLifetime(0.8f);
            DistortionShader.addDistortion(wave);
        }

        // Random sparks along ring
        if (Math.random() < 0.2f * intensity) {
            Vector2f point = MathUtils.getPointOnCircumference(center, currentRingRadius, (float)(Math.random() * 360));
            engine.addHitParticle(point, velocity, 30f * intensity, 0.7f, 0.15f, new Color(255, 200, 100, (int)(255 * intensity)));
        }

        // EMP arcs
        if (Math.random() < 0.05f * intensity) {
            float angleFrom = (float) (Math.random() * 360f);
            Vector2f arcFrom = MathUtils.getPointOnCircumference(center, currentRingRadius, angleFrom);
            Vector2f arcTo = MathUtils.getPointOnCircumference(center, currentRingRadius, angleFrom + 10f + (float) Math.random() * 20f);
            engine.spawnEmpArcVisual(arcFrom, null, arcTo, null, 10f,
                    new Color(255, 180, 60, (int)(255 * intensity)),
                    new Color(255, 100, 20, (int)(150 * intensity)));
        }

        // Damage to nearby ships
        for (ShipAPI other : engine.getShips()) {
            if (other == ship || other.isHulk() || other.isStationModule()) continue;
            float dist = MathUtils.getDistance(center, other.getLocation());
            if (dist <= RING_MAX_RADIUS) {
                float minRadius = 100f;
                float normDist = Math.min(1f, Math.max(0f, (dist - minRadius) / (RING_MAX_RADIUS - minRadius)));
                float proxIntensity = (float) Math.pow(1f - normDist, 2f); // Exponential falloff
                applyProximityEffect(engine, other, proxIntensity, ship);
            }
        }

        // Beam hit visual effects
        beam.setWidth(75f + MathUtils.getRandom().nextInt(75));
        applyGoldionCrusherEffects(engine, beam, ship, amount);
    }


    private void applyGoldionCrusherEffects(CombatEngineAPI engine, BeamAPI beam, ShipAPI ship, float amount) {
        if (beam.getDamageTarget() == null || !(beam.getDamageTarget() instanceof ShipAPI target)) return;

        Vector2f hitPoint = beam.getTo();

        if (shieldHitCooldown > 0f) shieldHitCooldown -= amount;

        boolean isShieldHit = target.getShield() != null
                && target.getShield().isOn()
                && target.getShield().isWithinArc(hitPoint);

        if (beam.didDamageThisFrame()) {
            if (isShieldHit) {
                hullHitDuration = 0f;

                engine.spawnExplosion(hitPoint, target.getVelocity(), new Color(255, 230, 80), 100f, 0.2f);
                engine.addHitParticle(hitPoint, target.getVelocity(), 90f, 1.5f, 0.1f, Color.WHITE);

                if (shieldHitCooldown <= 0f) {
                    shieldHitCooldown = 0.5f;
                    engine.spawnEmpArc(
                            ship,
                            beam.getFrom(),
                            null,
                            target,
                            DamageType.ENERGY,
                            500f,
                            300f,
                            1000f,
                            "tachyon_lance_emp_impact",
                            30f,
                            Color.YELLOW,
                            Color.ORANGE
                    );
                }
            } else {
                hullHitDuration += amount;
                float intensity = Math.min(1f, hullHitDuration / 10f);

                float maxRange = RING_MAX_RADIUS;
                // Non-linear scaling for more dramatic jitter near epicenter
                float jitterStrength = 15f + 40f * (float)Math.pow(intensity, 2);
                int jitterCount = (int)(5 + 10 * intensity);

                target.setJitter(ship, new Color(255, 220, 100, 180 + MathUtils.getRandom().nextInt(75)), intensity, jitterCount, jitterStrength);
                // Omit or keep setJitterUnder as you prefer, since it's invisible
                //target.setJitterUnder(ship, new Color(255, 170, 50, 200 - MathUtils.getRandom().nextInt(50)), intensity * 0.6f, jitterCount, jitterStrength * 2f);
                engine.addHitParticle(hitPoint, target.getVelocity(), 120f + 50f * intensity, 2f, 0.2f, new Color(255, 220, 100));
                engine.spawnExplosion(hitPoint, target.getVelocity(), new Color(255, 200, 100), 80f + intensity * 100f, 0.3f);

                if (hullHitDuration > 4f) {
                    target.getFluxTracker().increaseFlux(100f * amount, false);
                }
            }
        }
        //Needs revision!! More Dynamic Finale that Fakes Atomization of the targets within 800f Radius
        //Between 800 and 1200 is just the push and flux damage
        float innerRadius = 800f;
        float outerRadius = 1200f;
        if (beam.getWeapon().getChargeLevel() <= 0f && !finalBurstTriggered) {
            finalBurstTriggered = true;
            engine.spawnExplosion(hitPoint, ship.getVelocity(), new Color(255, 180, 60), 300f, 0.6f);

            for (ShipAPI enemy : engine.getShips()) {
                if (enemy.getOwner() == ship.getOwner() || enemy.isHulk() || enemy.isStationModule()) continue;

                float dist = MathUtils.getDistance(enemy, hitPoint);
                if (dist <= 1200f) {
                    Vector2f push = Vector2f.sub(enemy.getLocation(), hitPoint, null);
                    push.normalise();
                    push.scale(100f);
                    enemy.getVelocity().x += push.x;
                    enemy.getVelocity().y += push.y;
                    enemy.getFluxTracker().increaseFlux(300f, true);

                    if (dist <= 800f) {
                        engine.applyDamage(enemy, enemy.getLocation(), 5000f, DamageType.HIGH_EXPLOSIVE, 0f, true, false, ship);

                        for (int i = 0; i < 80; i++) {
                            Vector2f point = MathUtils.getRandomPointInCircle(enemy.getLocation(), 100f);
                            engine.addHitParticle(point, MathUtils.getRandomPointInCircle(null, 150f), 25f, 2f, 1f, new Color(255, 200, 100));
                        }

                        WaveDistortion wave = new WaveDistortion(enemy.getLocation(), new Vector2f());
                        wave.setSize(400f);
                        wave.setIntensity(40f);
                        wave.setLifetime(1f);
                        DistortionShader.addDistortion(wave);
                    }
                }
            }
        }
        if (beam.getWeapon().getChargeLevel() > 0f) {
            finalBurstTriggered = false;
        }
    }
    private void spawnGoldenParticleRing(CombatEngineAPI engine, Vector2f center, Vector2f velocity, float radius, float intensity) {
        int particleCount = 360;
        float angleStep = 360f / particleCount;

        float ratio = radius / RING_MAX_RADIUS;
        ratio = Math.min(1f, Math.max(0f, ratio)); // clamp 0..1

        for (int i = 0; i < particleCount; i++) {
            float angle = i * angleStep;
            Vector2f point = MathUtils.getPointOnCircumference(center, radius, angle);

            float alpha = Math.max(0f, 1f - ratio); // fade out as before

            // Gold color: hue approx 0.12 (around 43 degrees)
            // Saturation: from 0.6 (center) to 1.0 (edges)
            // Brightness: from 1.0 (center) to 0.6 (edges)

            float hue = 0.12f;
            float saturation = 0.6f + 0.4f * ratio; // 0.6 to 1.0
            float brightness = 1.0f - 0.4f * ratio; // 1.0 to 0.6

            Color goldColor = Color.getHSBColor(hue, saturation, brightness);
            Color finalColor = new Color(
                    goldColor.getRed(),
                    goldColor.getGreen(),
                    goldColor.getBlue(),
                    (int)(255 * alpha * intensity)
            );

            engine.addSmoothParticle(
                    point,
                    velocity,
                    20f,
                    1.2f + 0.5f * intensity,
                    0.5f,
                    finalColor
            );
        }
    }

    private void applyProximityEffect(CombatEngineAPI engine, ShipAPI target, float intensity, ShipAPI sourceShip) {
        float jitterStrength = 10f + 30f * intensity; // nonlinear scaling
        int jitterCount = (int)(2 + 6 * intensity);

        Color jitterColor = new Color(255, 220, 100, (int)(100 + 155 * intensity));

        target.setJitter(sourceShip, jitterColor, intensity, jitterCount, jitterStrength);
        target.setJitterUnder(sourceShip, new Color(255, 170, 50, 150), intensity * 0.5f, jitterCount, jitterStrength * 1.5f);

        if (Math.random() < 0.05f * intensity) {
            Vector2f knock = Vector2f.sub(target.getLocation(), sourceShip.getLocation(), null);
            knock.normalise();
            knock.scale(100f * intensity);
            target.getVelocity().x += knock.x;
            target.getVelocity().y += knock.y;
        }

        float dmg = 200f * intensity;
        float emp = 150f * intensity;

        engine.applyDamage(target, target.getLocation(), dmg, DamageType.HIGH_EXPLOSIVE, emp, false, false, sourceShip);
        engine.addHitParticle(target.getLocation(), target.getVelocity(), 80f * intensity, 1f, 0.2f, Color.YELLOW);
    }

    private void spawnPassiveVisuals(CombatEngineAPI engine, Vector2f center, Vector2f velocity, float time) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 2f);
        Color glow = new Color(255, 200, 100, (int)(100 + 100 * pulse));
        engine.addSmoothParticle(center, velocity, 50f + 10f * pulse, 1.5f, 0.5f, glow);
    }
    private void spawnChargeUpVisuals(CombatEngineAPI engine, Vector2f center, Vector2f velocity, float progress) {
        progress = Math.min(progress, 1f);
        float radius = 80f + 100f * progress;

        for (int i = 0; i < 16; i++) {
            float angle = (float)(Math.random() * 360f);
            Vector2f point = MathUtils.getPointOnCircumference(center, radius, angle);
            Color c = new Color(255, 160 + (int)(95 * progress), 50, (int)(200 * progress));
            engine.addSmoothParticle(point, velocity, 20f, 1.2f + progress, 0.3f, c);
        }

        if (Math.random() < 0.15f) {
            WaveDistortion wave = new WaveDistortion(center, velocity);
            wave.setSize(150f + 150f * progress);
            wave.setIntensity(15f * progress);
            wave.setLifetime(0.5f);
            wave.fadeOutIntensity(0.3f);
            wave.fadeInSize(0.1f);
            DistortionShader.addDistortion(wave);
        }
    }
}
