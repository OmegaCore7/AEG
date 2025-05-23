package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class AEG_ReturnZeroOnHitEffect implements OnHitEffectPlugin {
    private static final float DURATION = 20f;
    private static final float BLACK_HOLE_RADIUS = 3000f;
    private static final float INNER_RING_RADIUS = 200f;
    private static final float PULL_STRENGTH = 300f;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        engine.addPlugin(new BlackHoleEffectPlugin(point));
    }

    public static class BlackHoleEffectPlugin implements EveryFrameCombatPlugin {

        private final Vector2f location;
        private float elapsed = 0f;
        private boolean explosionOccurred = false;

        public BlackHoleEffectPlugin(Vector2f loc) {
            this.location = new Vector2f(loc);
        }

        @Override public void init(CombatEngineAPI engine) {}
        @Override public void renderInWorldCoords(ViewportAPI viewport) {}
        @Override public void renderInUICoords(ViewportAPI viewport) {}
        @Override public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {}

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;

            elapsed += amount;

            // Apply pull to ships
            applyBlackHolePull(engine);

            // Standard rotating rings
            createEllipticalGradientRing(engine, location, 200f, 251, elapsed, (float)(Math.PI/5), 0f, 1.0f, false);
            createEllipticalGradientRing(engine, location, 180f, 251, elapsed, (float)(Math.PI/6), (float)Math.PI/4f, 0.8f, false);

            // Perpendicular elliptical rings (simulate rotation in different plane)
            createEllipticalGradientRing(engine, location, 160f, 251, elapsed, (float)(Math.PI/7), (float)Math.PI/2f, 0.6f, true);
            createEllipticalGradientRing(engine, location, 220f, 251, elapsed, (float)(Math.PI/8), (float)(3 * Math.PI / 4f), 0.5f, true);

            // Trigger explosion after full duration
            if (elapsed >= DURATION && !explosionOccurred) {
                triggerExplosion(engine, location);
                explosionOccurred = true;
            }

            // Remove plugin shortly after explosion
            if (elapsed >= DURATION + 1f) {
                engine.removePlugin(this);
            }
        }

        private void applyBlackHolePull(CombatEngineAPI engine) {
            for (CombatEntityAPI entity : engine.getShips()) {
                if (entity instanceof ShipAPI && entity != engine.getPlayerShip()) {
                    float distance = MathUtils.getDistance(entity, location);
                    if (distance <= BLACK_HOLE_RADIUS) {
                        Vector2f pull = VectorUtils.getDirectionalVector(entity.getLocation(), location);
                        float strength = (1f - distance / BLACK_HOLE_RADIUS) * PULL_STRENGTH;
                        pull.scale(strength);
                        Vector2f.add(entity.getVelocity(), pull, entity.getVelocity());
                    }
                }
            }
        }

        private void createGradientRing(CombatEngineAPI engine, Vector2f center, float radius, int count) {
            for (int i = 0; i < count; i++) {
                float angle = (float) (i * 2 * Math.PI / count);
                float x = center.x + radius * (float) Math.cos(angle);
                float y = center.y + radius * (float) Math.sin(angle);
                Color color = getOrangeGradientColor(i, count);
                engine.addHitParticle(new Vector2f(x, y), new Vector2f(), 10f, 1f, DURATION, color);
            }
        }

        private Color getOrangeGradientColor(int index, int total) {
            float ratio = (float) index / total;
            int red = (int) (255 * (1 - ratio) + 255 * ratio);
            int green = (int) (140 * (1 - ratio) + 69 * ratio);
            return new Color(red, green, 0);
        }

        private void triggerExplosion(CombatEngineAPI engine, Vector2f center) {
            // Mazinger Zero photon colors
            Color coreWhite = new Color(255, 255, 200);
            Color photonRed = new Color(255, 60, 60);
            Color photonGold = new Color(255, 180, 50);
            Color photonMagenta = new Color(255, 100, 0);

            // Multiple smaller explosions around center to build up the effect
            for (int i = 0; i < 12; i++) {
                Vector2f offset = MathUtils.getRandomPointInCircle(center, 600f);
                Vector2f velocity = MathUtils.getRandomPointInCircle(null, 100f);
                float size = 200f + (float) Math.random() * 150f;
                Color color = (i % 2 == 0) ? photonGold : photonRed;
                engine.spawnExplosion(offset, velocity, color, size, 0.6f);
                engine.addHitParticle(offset, velocity, size * 0.6f, 1f, 0.5f, photonRed);
            }

            // Pulsing nebula shockwaves for depth and energy flow
            for (int i = 0; i < 3; i++) {
                float delay = i * 0.2f;
                float size = 1000f + i * 1000f;
                float duration = 0.7f + i * 0.1f;
                engine.addNebulaParticle(
                        center,
                        new Vector2f(),
                        size,
                        1.5f,
                        delay,   // delay before particle appears
                        0.3f,    // ramp-up duration
                        duration,
                        photonMagenta
                );
            }

            // The final big bright core explosion
            engine.spawnExplosion(center, new Vector2f(), coreWhite, 2500f, 1.2f);
            engine.addHitParticle(center, new Vector2f(), 3000f, 1f, 1f, photonGold);

            // Play explosion sound
            Global.getSoundPlayer().playSound("terrain_hyperspace_lightning", 1f, 1f, center, new Vector2f());

            // Deal heavy damage to all ships except player inside the explosion radius
            ShipAPI player = engine.getPlayerShip();
            for (ShipAPI ship : engine.getShips()) {
                if (ship != null && ship != player && MathUtils.getDistance(ship, center) < 2500f) {
                    engine.applyDamage(ship, center, 10000f, DamageType.HIGH_EXPLOSIVE, 5000f, true, false, player);
                }
            }
        }
        private void createEllipticalGradientRing(CombatEngineAPI engine, Vector2f center, float baseRadius, int count,
                                                  float elapsedTime, float rotationSpeed, float angleOffset, float brightness,
                                                  boolean verticalEllipse) {
            float ellipseFactor = 0.6f + 0.4f * (float) Math.sin(elapsedTime * 2); // oscillates between 0.2 and 1.0
            float angle = elapsedTime * rotationSpeed + angleOffset;

            for (int i = 0; i < count; i++) {
                float theta = (float) (i * 2 * Math.PI / count);
                float sin = (float) Math.sin(theta);
                float cos = (float) Math.cos(theta);

                // Apply ellipse transformation
                float x = baseRadius * cos;
                float y = baseRadius * sin;

                if (verticalEllipse) {
                    y *= ellipseFactor;
                } else {
                    x *= ellipseFactor;
                }

                // Rotate the entire shape
                float rotX = x * (float) Math.cos(angle) - y * (float) Math.sin(angle);
                float rotY = x * (float) Math.sin(angle) + y * (float) Math.cos(angle);

                Vector2f pos = new Vector2f(center.x + rotX, center.y + rotY);
                Color color = getOrangeGradientColor(i, count);
                color = new Color(
                        Math.min(255, (int)(color.getRed() * brightness)),
                        Math.min(255, (int)(color.getGreen() * brightness)),
                        color.getBlue()
                );

                engine.addHitParticle(pos, new Vector2f(), 10f, 1f, 0.1f, color);
            }
        }

    }
}
