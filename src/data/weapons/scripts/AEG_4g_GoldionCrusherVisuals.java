package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class AEG_4g_GoldionCrusherVisuals implements BeamEffectPlugin {
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

        // Ring pulse timer & radius logic
        ringTimer += amount;
        if (ringTimer >= RING_INTERVAL) {
            ringTimer -= RING_INTERVAL;
            currentRingRadius = 0f; // Start new ring pulse
        }
        if (currentRingRadius < RING_MAX_RADIUS) {
            // Increase radius proportionally to elapsed time within pulse interval
            currentRingRadius += RING_MAX_RADIUS * (amount / RING_INTERVAL);
        }

        // Visual intensity fades as radius increases (1 at 0 radius, 0 at max)
        float intensity = 1f - (currentRingRadius / RING_MAX_RADIUS);
        if (intensity < 0f) intensity = 0f;

        Vector2f from = beam.getFrom();
        Vector2f to = beam.getTo();
        Vector2f velocity = ship.getVelocity();
        Vector2f center = new Vector2f((from.x + to.x) / 2f, (from.y + to.y) / 2f);

        spawnGoldenParticleRing(engine, center, velocity, currentRingRadius, intensity);


        // --- Visual expanding ring ---
        spawnGoldenParticleRing(engine, center, velocity, currentRingRadius, intensity);

        // Optional: subtle hit particles along ring edge
        if (Math.random() < 0.2f * intensity) {
            Vector2f pointOnRing = MathUtils.getPointOnCircumference(center, currentRingRadius, (float) (Math.random() * 360));
            engine.addHitParticle(pointOnRing, velocity, 30f * intensity, 0.7f, 0.15f, new Color(255, 200, 100, (int)(255 * intensity)));
        }

        // --- EMP arcs along ring edge, spaced randomly ---
        if (Math.random() < 0.05f * intensity) {
            float angleFrom = (float) (Math.random() * 360f);
            Vector2f arcfrom = MathUtils.getPointOnCircumference(center, currentRingRadius, angleFrom);
            Vector2f arcto = MathUtils.getPointOnCircumference(center, currentRingRadius, angleFrom + 10f + (float) Math.random() * 20f);

            engine.spawnEmpArcVisual(
                    arcfrom, null,
                    arcto, null,
                    10f,
                    new Color(255, 180, 60, (int)(255 * intensity)),
                    new Color(255, 100, 20, (int)(150 * intensity))
            );
        }

        // --- Apply damage/jitter to ships based on distance from center (wave center) ---
        for (ShipAPI other : engine.getShips()) {
            if (other == ship || other.isHulk() || other.isStationModule()) continue;

            float dist = MathUtils.getDistance(center, other.getLocation());

            if (dist <= RING_MAX_RADIUS) {
                float minRadius = 100f; // radius for full damage near center

                float proxIntensity;
                if (dist < minRadius) {
                    proxIntensity = 1f;
                } else {
                    proxIntensity = 1f - ((dist - minRadius) / (RING_MAX_RADIUS - minRadius));
                    proxIntensity = Math.max(proxIntensity, 0f);
                }

                applyProximityEffect(engine, other, proxIntensity, ship);
            }
        }

        // --- Existing beam visuals and hit effects ---
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

                target.setJitter(ship, new Color(255, 210, 90), intensity, 10, 5f + 10f * intensity);
                engine.addHitParticle(hitPoint, target.getVelocity(), 120f + 50f * intensity, 2f, 0.2f, new Color(255, 220, 100));
                engine.spawnExplosion(hitPoint, target.getVelocity(), new Color(255, 200, 100), 80f + intensity * 100f, 0.3f);

                if (hullHitDuration > 4f) {
                    target.getFluxTracker().increaseFlux(100f * amount, false);
                }
            }
        }

        if (beam.getWeapon().getChargeLevel() <= 0f && !finalBurstTriggered) {
            finalBurstTriggered = true;

            engine.spawnExplosion(hitPoint, ship.getVelocity(), new Color(255, 180, 60), 300f, 0.6f);
            engine.addSmoothParticle(hitPoint, ship.getVelocity(), 250f, 2f, 0.5f, Color.YELLOW);

            for (ShipAPI enemy : engine.getShips()) {
                if (enemy.getOwner() == ship.getOwner()) continue;
                if (MathUtils.getDistance(enemy, hitPoint) <= 600f) {
                    Vector2f push = Vector2f.sub(enemy.getLocation(), hitPoint, null);
                    push.normalise();
                    push.scale(300f);
                    enemy.getVelocity().x += push.x;
                    enemy.getVelocity().y += push.y;
                    enemy.getFluxTracker().increaseFlux(300f, true);
                }
            }
        }
        if (beam.getWeapon().getChargeLevel() > 0f) {
            finalBurstTriggered = false;
        }
    }
    private void spawnGoldenParticleRing(CombatEngineAPI engine, Vector2f center, Vector2f velocity, float radius, float intensity) {
        int particleCount = 144; // Controls how many particles in the ring; increase for smoother loop
        float angleStep = 360f / particleCount;

        for (int i = 0; i < particleCount; i++) {
            float angle = i * angleStep;
            Vector2f point = MathUtils.getPointOnCircumference(center, radius, angle);

            float alpha = Math.max(0f, 1f - (radius / RING_MAX_RADIUS)); // fade out as it expands
            Color color = new Color(
                    255,
                    (int)(220 + 30 * intensity),
                    (int)(100 + 80 * intensity),
                    (int)(255 * alpha * intensity)
            );

            engine.addSmoothParticle(
                    point,
                    velocity,
                    20f,               // size of particle
                    1.2f + 0.5f * intensity, // brightness
                    0.5f,              // duration
                    color
            );
        }
    }

    private void applyProximityEffect(CombatEngineAPI engine, ShipAPI target, float intensity, ShipAPI sourceShip) {
        target.setJitter(sourceShip, new Color(255, 220, 100), intensity, 3, 5f * intensity);

        float dmg = 200f * intensity;
        float emp = 150f * intensity;
        engine.applyDamage(target, target.getLocation(), dmg, DamageType.HIGH_EXPLOSIVE, emp, false, false, sourceShip);

        engine.addHitParticle(target.getLocation(), target.getVelocity(), 80f * intensity, 1f * intensity, 0.2f, Color.YELLOW);
    }
}
