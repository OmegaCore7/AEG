package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.CollisionUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class AEG_IronCutterPierce implements OnHitEffectPlugin {
    private static final float LINE_LENGTH = 400f;
    private static final float LINE_WIDTH = 80f;
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
                    spawnVisuals(engine, point, finalExitPoint, finalDirection, target, projectile);
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
        float ticks = 20f;
        float damagePerTick = baseDamage / ticks;
        float step = LINE_LENGTH / ticks;

        for (int i = 0; i <= ticks; i++) {
            Vector2f p = new Vector2f(entryPoint);
            p.x += direction.x * i * step;
            p.y += direction.y * i * step;
            engine.applyDamage(target, p, damagePerTick, DamageType.HIGH_EXPLOSIVE, 0, false, false, projectile.getSource());
        }
    }

    private void spawnVisuals(CombatEngineAPI engine, Vector2f entry, Vector2f exit, Vector2f direction,
                              CombatEntityAPI target, DamagingProjectileAPI projectile) {

        // Big initial explosion
        engine.spawnExplosion(entry, new Vector2f(), Color.WHITE, 40f, 0.6f);
        RippleDistortion impactRipple = new RippleDistortion(entry, new Vector2f());
        impactRipple.setSize(400f);
        impactRipple.setIntensity(90f);
        impactRipple.setLifetime(0.6f);
        impactRipple.fadeInIntensity(0.1f);
        impactRipple.fadeOutIntensity(0.4f);
        DistortionShader.addDistortion(impactRipple);

        Vector2f lineDir = new Vector2f(exit);
        Vector2f.sub(lineDir, entry, lineDir);
        float distance = lineDir.length();
        lineDir.normalise();

        float segments = 15f;
        float spacing = distance / segments;

        for (int i = 0; i <= segments; i++) {
            Vector2f point = new Vector2f(entry);
            point.x += lineDir.x * i * spacing;
            point.y += lineDir.y * i * spacing;

            // Glowing scorch line
            engine.addHitParticle(point, new Vector2f(), 30f, 1f, 0.3f,
                    new Color(255, 140 + rand.nextInt(115), 30 + rand.nextInt(50), 255));

            // Subtle burn nebulae
            engine.addNebulaParticle(point, new Vector2f(), 20f + rand.nextFloat() * 15f, 1.5f, 0.3f, 0.8f,
                    1.2f, new Color(50, 20, 10, 150), true);

            // Welding sparks on sides
            for (int s = 0; s < 2; s++) {
                float angleOffset = (s == 0 ? 90f : -90f);
                float angle = (float) Math.toDegrees(Math.atan2(lineDir.y, lineDir.x)) + angleOffset;
                Vector2f sparkOffset = new Vector2f(
                        (float) Math.cos(Math.toRadians(angle)) * (LINE_WIDTH * 0.5f),
                        (float) Math.sin(Math.toRadians(angle)) * (LINE_WIDTH * 0.5f)
                );
                Vector2f sparkPoint = Vector2f.add(point, sparkOffset, null);
                engine.addSmoothParticle(sparkPoint, new Vector2f(), 6f + rand.nextFloat() * 4f, 1f, 0.5f,
                        new Color(255, 150 + rand.nextInt(100), 50));
            }

            // Random black scorch accents
            if (rand.nextFloat() < 0.3f) {
                float offsetAngle = rand.nextFloat() * 360f;
                float length = 20f + rand.nextFloat() * 40f;
                Vector2f scorchEnd = new Vector2f(
                        (float) Math.cos(Math.toRadians(offsetAngle)) * length,
                        (float) Math.sin(Math.toRadians(offsetAngle)) * length
                );
                Vector2f accentPoint = Vector2f.add(point, scorchEnd, null);
                engine.addHitParticle(accentPoint, new Vector2f(), 15f, 1f, 0.4f, new Color(30, 30, 30, 200));
            }
        }

        if (target instanceof ShipAPI && ((ShipAPI) target).getShield() != null &&
                ((ShipAPI) target).getShield().isOn()) {

            for (float i = 0.1f; i <= 0.9f; i += 0.15f) {
                Vector2f arcPoint = new Vector2f(entry);
                arcPoint.x += lineDir.x * i * distance;
                arcPoint.y += lineDir.y * i * distance;

                engine.spawnEmpArcVisual(arcPoint, projectile.getSource(), arcPoint, target,
                        20f + rand.nextInt(30),
                        new Color(50, 255 - rand.nextInt(100), 255, 200),
                        new Color(200, 255, 255, 150));
            }

            RippleDistortion shieldRipple = new RippleDistortion(entry, new Vector2f());
            shieldRipple.setSize(250f);
            shieldRipple.setIntensity(65f);
            shieldRipple.setLifetime(0.6f);
            shieldRipple.fadeInIntensity(0.1f);
            shieldRipple.fadeOutIntensity(0.3f);
            DistortionShader.addDistortion(shieldRipple);
        }
    }
}
