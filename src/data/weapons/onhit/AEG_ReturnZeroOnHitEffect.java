package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AEG_ReturnZeroOnHitEffect implements OnHitEffectPlugin {
    private static final float DURATION = 20f;
    private static final float BLACK_HOLE_RADIUS = 3000f;
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

        // 🔄 New: List to track orbiting orbs
        private final List<OrbitalParticle> orbitals = new ArrayList<>();
        private boolean initialized = false;

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

            if (!initialized) {
                spawnOrbitingParticles();
                initialized = true;
            }

            applyBlackHolePull(engine);

            // Core glow effect
            spawnNucleusGlow(engine);

            // Rotating elliptical rings
            createEllipticalGradientRing(engine, location, 200f, 251, elapsed, (float)(Math.PI/5), 0f, 1.0f, false);
            createEllipticalGradientRing(engine, location, 180f, 251, elapsed, (float)(Math.PI/6), (float)Math.PI/4f, 0.8f, false);
            createEllipticalGradientRing(engine, location, 160f, 251, elapsed, (float)(Math.PI/7), (float)Math.PI/2f, 0.6f, true);
            createEllipticalGradientRing(engine, location, 220f, 251, elapsed, (float)(Math.PI/8), (float)(3 * Math.PI / 4f), 0.5f, true);

            // 🔄 Update orbiting orbs
            for (OrbitalParticle orb : orbitals) {
                orb.advance(elapsed, engine, location);
            }

            if (elapsed >= DURATION && !explosionOccurred) {
                triggerExplosion(engine, location);
                explosionOccurred = true;
            }

            if (elapsed >= DURATION + 1f) {
                engine.removePlugin(this);
            }
        }

        private void spawnOrbitingParticles() {
            for (int i = 0; i < 6; i++) {
                float angleOffset = (float) (Math.random() * Math.PI * 2f);
                Color[] palette = {
                        new Color(255, 255, 200),
                        new Color(255, 60, 60),
                        new Color(255, 180, 50),
                        new Color(255, 100, 0)
                };
                Color color = palette[i % palette.length];
                orbitals.add(new OrbitalParticle(angleOffset, color));
            }
        }

        private void spawnNucleusGlow(CombatEngineAPI engine) {
            // Core white center
            engine.addNebulaParticle(location, new Vector2f(), 50f, 2f, 0f, 0.3f, 1f, new Color(255 - MathUtils.getRandom().nextInt(50), 150 - MathUtils.getRandom().nextInt(50),50 - MathUtils.getRandom().nextInt(50), 255 - MathUtils.getRandom().nextInt(50)));
            // Orange glow halo
            engine.addNebulaParticle(location, new Vector2f(), 75f, 2f, 0f, 0.3f, 1f, new Color(150 - MathUtils.getRandom().nextInt(50), 100 - MathUtils.getRandom().nextInt(50),0, 255 - MathUtils.getRandom().nextInt(50)));
            // Blue glow halo
            engine.addNebulaParticle(location, new Vector2f(), 100f, 2f, 0f, 0.3f, 1f, new Color(100 - MathUtils.getRandom().nextInt(50), 50 - MathUtils.getRandom().nextInt(50), 0, 200 - MathUtils.getRandom().nextInt(50)));
        }

        // 🔄 Orbital particle class for orbiting orbs
        public class OrbitalParticle {
            float baseAngle;
            Color baseColor;
            float speedMultiplier;

            public OrbitalParticle(float angle, Color color) {
                this.baseAngle = angle;
                this.baseColor = color;
                this.speedMultiplier = 1f + (float) Math.random() * 0.5f; // Between 1.0 and 1.5
            }

            void advance(float elapsedTime, CombatEngineAPI engine, Vector2f center) {
                float radius = 180f + 20f * (float)Math.sin(elapsedTime * 1.5f * speedMultiplier + baseAngle);
                float speed = 1f;
                float angle = baseAngle + elapsedTime * speed;
                float pulse = 0.5f + 0.5f * (float)Math.sin(elapsedTime * 3f + baseAngle);

                float x = center.x + radius * (float)Math.cos(angle);
                float y = center.y + radius * (float)Math.sin(angle);

                float size = 40f + 50f * pulse;
                float brightness = 0.8f + 0.7f * pulse;

                Color pulseColor = new Color(
                        Math.min(255, (int)(baseColor.getRed() * brightness)),
                        Math.min(255, (int)(baseColor.getGreen() * brightness)),
                        Math.min(255, (int)(baseColor.getBlue() * brightness)),
                        255
                );

                Vector2f pos = new Vector2f(x, y);

                engine.addHitParticle(pos, new Vector2f(), size, 1f, 0.1f, pulseColor);

                if (Math.random() < 0.3f) {
                    engine.addNebulaParticle(
                            pos,
                            MathUtils.getRandomPointInCircle(null, 20f),
                            size * 0.8f, // was 0.6f
                            1.5f,
                            0f,
                            0.1f,
                            0.4f,
                            new Color(255 - MathUtils.getRandom().nextInt(75), 100 - MathUtils.getRandom().nextInt(75), 80 - MathUtils.getRandom().nextInt(75),150 - MathUtils.getRandom().nextInt(75))
                    );
                }
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
