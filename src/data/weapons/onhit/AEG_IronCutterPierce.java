package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class AEG_IronCutterPierce implements OnHitEffectPlugin {
    private static final float LINE_LENGTH = 400f;
    private final Random rand = new Random();

    @Override
    public void onHit(final DamagingProjectileAPI projectile, final CombatEntityAPI target,
                      final Vector2f point, final boolean shieldHit, ApplyDamageResultAPI damageResult,
                      final CombatEngineAPI engine) {
        if (engine == null || !(target instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) target;

        if (shieldHit) {
            handleShieldHit(engine, projectile, ship, point);
            return;
        }

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
                    spawnHullStrikeVisuals(engine, point, finalExitPoint, finalDirection, (ShipAPI) target, projectile);
                }
                elapsed += amount;
                if (elapsed > 0.25f) {
                    engine.removePlugin(this);
                }
            }

            @Override public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {}
            @Override public void init(CombatEngineAPI engine) {}
            @Override public void renderInWorldCoords(ViewportAPI viewport) {}
            @Override public void renderInUICoords(ViewportAPI viewport) {}
        });

        engine.removeEntity(projectile);
    }

    private void applyLineDamage(CombatEngineAPI engine, CombatEntityAPI target, Vector2f entryPoint,
                                 Vector2f direction, DamagingProjectileAPI projectile) {
        float baseDamage = projectile.getDamageAmount();
        float ticks = 10f;
        float damagePerTick = baseDamage / ticks;
        float step = LINE_LENGTH / ticks;

        for (int i = 0; i <= ticks; i++) {
            Vector2f p = new Vector2f(entryPoint);
            p.x += direction.x * i * step;
            p.y += direction.y * i * step;
            engine.applyDamage(target, p, damagePerTick, DamageType.HIGH_EXPLOSIVE, 50, false, false, projectile.getSource());
        }
    }

    private void spawnHullStrikeVisuals(final CombatEngineAPI engine, final Vector2f entry, Vector2f exit, Vector2f direction,
                                        final ShipAPI target, final DamagingProjectileAPI projectile) {
        final Vector2f lineDir = Vector2f.sub(exit, entry, null);
        float distance = lineDir.length();
        lineDir.normalise();

        final int segments = 6;
        final float spacing = distance / segments;

        // First wave
        for (int i = 0; i <= segments; i++) {
            Vector2f point = getOffsetPointAlongLine(entry, lineDir, spacing * i);
            spawnExplosionWithRipple(engine, point, 60f + rand.nextFloat() * 80f, 0.3f + rand.nextFloat() * 0.3f);
        }

        // Delay and schedule second wave and final strike
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            float timer = 0f;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                timer += amount;
                if (timer > 0.15f && timer < 0.5f) {
                    // Second wave
                    for (int i = 0; i <= segments; i++) {
                        Vector2f point = getOffsetPointAlongLine(entry, lineDir, spacing * i);
                        spawnExplosionWithRipple(engine, point, 90f + rand.nextFloat() * 100f, 0.3f + rand.nextFloat() * 0.3f);
                    }
                }

                if (timer >= 0.5f) {
                    Vector2f finalStrikePoint = new Vector2f(target.getLocation());
                    finalStrikePoint.x += rand.nextFloat() * 80f - 40f;
                    finalStrikePoint.y += rand.nextFloat() * 80f - 40f;

                    engine.spawnExplosion(finalStrikePoint, new Vector2f(), new Color(255, 100, 20, 255), 250f, 1.2f);
                    RippleDistortion ripple = new RippleDistortion(finalStrikePoint, new Vector2f());
                    ripple.setSize(400f);
                    ripple.setIntensity(100f);
                    ripple.setLifetime(0.6f);
                    ripple.fadeInIntensity(0.1f);
                    ripple.fadeOutIntensity(0.4f);
                    DistortionShader.addDistortion(ripple);

                    engine.applyDamage(target, finalStrikePoint, 300f, DamageType.HIGH_EXPLOSIVE, 0f,
                            false, false, projectile.getSource());

                    engine.removePlugin(this);
                }
            }
        });
    }

    private Vector2f getOffsetPointAlongLine(Vector2f origin, Vector2f dir, float distance) {
        Vector2f point = new Vector2f(origin);
        point.x += dir.x * distance;
        point.y += dir.y * distance;

        float angle = rand.nextFloat() * 360f;
        float offsetMag = rand.nextFloat() * 30f;
        point.x += Math.cos(Math.toRadians(angle)) * offsetMag;
        point.y += Math.sin(Math.toRadians(angle)) * offsetMag;

        return point;
    }

    private void spawnExplosionWithRipple(CombatEngineAPI engine, Vector2f location, float size, float duration) {
        engine.spawnExplosion(location, new Vector2f(), new Color(255 - rand.nextInt(55), 150 - rand.nextInt(50),
                50 + rand.nextInt(50), 255), size, duration);

        RippleDistortion ripple = new RippleDistortion(location, new Vector2f());
        ripple.setSize(size + rand.nextFloat() * 50f);
        ripple.setIntensity(60f + rand.nextFloat() * 40f);
        ripple.setLifetime(duration);
        ripple.fadeInIntensity(0.1f);
        ripple.fadeOutIntensity(0.3f);
        DistortionShader.addDistortion(ripple);
    }

    private void handleShieldHit(CombatEngineAPI engine, DamagingProjectileAPI proj, ShipAPI target, Vector2f entry) {
        Vector2f center = target.getLocation();
        Vector2f dir = Vector2f.sub(center, entry, null);
        dir.normalise();

        float distance = target.getCollisionRadius();
        int segments = 5;
        float spacing = distance / segments;

        for (int i = 0; i <= segments; i++) {
            Vector2f point = getOffsetPointAlongLine(entry, dir, spacing * i);

            CombatEntityAPI arcTarget = target;
            if (rand.nextFloat() < 0.25f) {
                for (ShipAPI s : engine.getShips()) {
                    if (s == target || s.isHulk() || s.isShuttlePod()) continue;
                    if (Misc.getDistance(s.getLocation(), point) < 600f) {
                        arcTarget = s;
                        break;
                    }
                }
            }

            engine.spawnEmpArcVisual(point, proj.getSource(), point, arcTarget,
                    40f + rand.nextFloat() * 60f,
                    new Color(50, 255 - rand.nextInt(100), 255, 200 + rand.nextInt(55)),
                    new Color(200, 255, 255, 150 + rand.nextInt(105)));

            RippleDistortion ripple = new RippleDistortion(point, new Vector2f());
            ripple.setSize(150f + rand.nextFloat() * 100f);
            ripple.setIntensity(50f + rand.nextFloat() * 30f);
            ripple.setLifetime(0.4f + rand.nextFloat() * 0.3f);
            ripple.fadeInIntensity(0.1f);
            ripple.fadeOutIntensity(0.2f);
            DistortionShader.addDistortion(ripple);
        }
    }
}
