package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class AEG_IronCutterPierce implements OnHitEffectPlugin {
    private static final float LINE_LENGTH = 400f;
    private static final int EXPLOSION_SEGMENTS = 6;
    private final Random rand = new Random();

    @Override
    public void onHit(final DamagingProjectileAPI projectile, final CombatEntityAPI target,
                      final Vector2f point, final boolean shieldHit, ApplyDamageResultAPI damageResult,
                      final CombatEngineAPI engine) {
        if (engine == null) return;

        Vector2f direction = Vector2f.sub(target.getLocation(), point, null);
        if (direction.lengthSquared() == 0) {
            direction = new Vector2f(
                    (float) Math.cos(Math.toRadians(projectile.getFacing())),
                    (float) Math.sin(Math.toRadians(projectile.getFacing()))
            );
        } else {
            direction.normalise();
        }

        Vector2f exitPoint = new Vector2f(point);
        Vector2f scaledDir = new Vector2f(direction);
        scaledDir.scale(LINE_LENGTH);
        Vector2f.add(exitPoint, scaledDir, exitPoint);

        final Vector2f finalDirection = direction;
        final Vector2f finalExitPoint = exitPoint;

        engine.addPlugin(new EveryFrameCombatPlugin() {
            private boolean triggered = false;
            private float elapsed = 0f;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (!triggered) {
                    triggered = true;
                    applyLineDamage(engine, target, point, finalDirection, projectile);
                    if (shieldHit) {
                        spawnShieldEMP(engine, point, finalExitPoint, target, projectile);
                    } else {
                        spawnHullExplosions(engine, point, finalExitPoint);
                    }
                }
                elapsed += amount;
                if (elapsed > 0.25f) {
                    engine.removePlugin(this);
                }
            }

            @Override
            public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
            }

            @Override
            public void init(CombatEngineAPI engine) {
            }

            @Override
            public void renderInWorldCoords(ViewportAPI viewport) {
            }

            @Override
            public void renderInUICoords(ViewportAPI viewport) {
            }
        });

        engine.removeEntity(projectile);
    }

    private void applyLineDamage(CombatEngineAPI engine, CombatEntityAPI target, Vector2f entryPoint,
                                 Vector2f direction, DamagingProjectileAPI projectile) {
        float baseDamage = projectile.getDamageAmount();
        float damagePerTick = baseDamage / EXPLOSION_SEGMENTS;
        float step = LINE_LENGTH / EXPLOSION_SEGMENTS;

        for (int i = 0; i <= EXPLOSION_SEGMENTS; i++) {
            Vector2f p = new Vector2f(entryPoint);
            p.x += direction.x * i * step;
            p.y += direction.y * i * step;
            engine.applyDamage(target, p, damagePerTick, DamageType.HIGH_EXPLOSIVE, 50, false, false, projectile.getSource());
        }
    }

    private void spawnHullExplosions(CombatEngineAPI engine, Vector2f entry, Vector2f exit) {
        Vector2f dir = Vector2f.sub(exit, entry, null);
        float distance = dir.length();
        dir.normalise();
        float step = distance / EXPLOSION_SEGMENTS;

        for (int i = 0; i <= EXPLOSION_SEGMENTS; i++) {
            Vector2f p = new Vector2f(entry);
            p.x += dir.x * i * step;
            p.y += dir.y * i * step;

            // Random offset for non-perfect line
            float angle = rand.nextFloat() * 360f;
            float offsetDist = rand.nextFloat() * 25f;
            Vector2f offset = new Vector2f(
                    (float) Math.cos(Math.toRadians(angle)) * offsetDist,
                    (float) Math.sin(Math.toRadians(angle)) * offsetDist
            );
            Vector2f.add(p, offset, p);

            // Random explosion attributes
            float size = 75f + rand.nextFloat() * 60f + i * 10f;
            float duration = 0.4f + rand.nextFloat() * 0.3f;
            float alpha = 0.6f + rand.nextFloat() * 0.3f;
            float brightness = 1.0f + rand.nextFloat() * 0.5f;

            Color color = new Color(
                    (int) (180 + rand.nextFloat() * 75),
                    (int) (80 + rand.nextFloat() * 100),
                    (int) (40 + rand.nextFloat() * 40),
                    (int) (255 * alpha)
            );

            engine.spawnExplosion(p, new Vector2f(), color, size, duration);

            RippleDistortion ripple = new RippleDistortion(p, new Vector2f());
            ripple.setSize(size * (1.5f + rand.nextFloat() * 0.5f));
            ripple.setIntensity(50f + rand.nextFloat() * 50f);
            ripple.setLifetime(0.5f + rand.nextFloat() * 0.4f);
            ripple.fadeInIntensity(0.1f + rand.nextFloat() * 0.1f);
            ripple.fadeOutIntensity(0.3f + rand.nextFloat() * 0.2f);
            DistortionShader.addDistortion(ripple);
        }
    }

    private void spawnShieldEMP(CombatEngineAPI engine, Vector2f entry, Vector2f exit,
                                CombatEntityAPI target, DamagingProjectileAPI projectile) {
        Vector2f dir = Vector2f.sub(exit, entry, null);
        float distance = dir.length();
        dir.normalise();
        float step = distance / EXPLOSION_SEGMENTS;

        for (int i = 0; i < EXPLOSION_SEGMENTS; i++) {
            Vector2f p = new Vector2f(entry);
            p.x += dir.x * i * step;
            p.y += dir.y * i * step;

            float angle = rand.nextFloat() * 360f;
            float offsetDist = rand.nextFloat() * 35f;
            Vector2f offset = new Vector2f(
                    (float) Math.cos(Math.toRadians(angle)) * offsetDist,
                    (float) Math.sin(Math.toRadians(angle)) * offsetDist
            );
            Vector2f.add(p, offset, p);

            float coreSize = 30f + rand.nextFloat() * 60f;
            Color fringe = new Color(100 + rand.nextInt(155), 255, 255, 180 + rand.nextInt(75));
            Color core = new Color(50 + rand.nextInt(100), 255, 255, 150 + rand.nextInt(105));

            engine.spawnEmpArcVisual(p, projectile.getSource(), p, target, coreSize, fringe, core);
        }

        // Add central ripple on shield
        RippleDistortion ripple = new RippleDistortion(entry, new Vector2f());
        ripple.setSize(200f + rand.nextFloat() * 100f);
        ripple.setIntensity(50f + rand.nextFloat() * 40f);
        ripple.setLifetime(0.4f + rand.nextFloat() * 0.3f);
        ripple.fadeInIntensity(0.1f + rand.nextFloat() * 0.1f);
        ripple.fadeOutIntensity(0.3f + rand.nextFloat() * 0.2f);
        DistortionShader.addDistortion(ripple);
    }
}