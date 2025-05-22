package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class AEG_ReturnZeroOnHitEffect implements OnHitEffectPlugin {
    private static final float DURATION = 20f;
    private static final float DAMAGE_REDUCTION = 0.01f; // 99% damage reduction
    private static final float BLACK_HOLE_RADIUS = 2500f;
    private static final float RING_RADIUS = 205f;
    private static final float INNER_RING_RADIUS = 200f;
    private static final float PULL_STRENGTH = 300f;
    private static final Color RING_COLOR = new Color(185, 0, 255); // Bright magenta
    private static final Color INNER_RING_COLOR = new Color(255, 140, 0); // Gradient orange
    private static final Color EXPLOSION_COLOR = new Color(105, 255, 105); // White with green fringe
    private static final Color EXPLOSION_FRINGE_COLOR = new Color(25, 153, 25);
    private static final Random RANDOM = new Random();

    private static Vector2f blackHolePosition;
    private static boolean isActive = false;
    private static boolean explosionOccurred = false;
    private static float elapsedTime = 0f;
    private static final float BLACKHOLE_DURATION = 3.0f;
    private static final float PULL_RADIUS = 5000f;


    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        engine.addPlugin(new BlackHoleEffectPlugin(point));
    }

    public static class BlackHoleEffectPlugin implements EveryFrameCombatPlugin {

        private final Vector2f location;
        private float elapsed = 0f;
        private final List<Ring> rings = new ArrayList<>();

        public BlackHoleEffectPlugin(Vector2f loc) {
            this.location = new Vector2f(loc);
            blackHolePosition = new Vector2f(loc);
            isActive = true;
            explosionOccurred = false;
        }
        @Override public void init(CombatEngineAPI engine) {}
        @Override public void renderInWorldCoords(ViewportAPI viewport) {}
        @Override public void renderInUICoords(ViewportAPI viewport) {}
        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (Global.getCombatEngine().isPaused()) return;

            elapsed += amount;
            elapsedTime += amount;

            // Pull ships in
            applyBlackHolePull();

            // Draw ring particles and effects
            applyVisualEffects(Global.getCombatEngine().getPlayerShip());

            // Add sucked-in particles spiraling inward
            createBlackHoleParticles();

            // Optional: trigger final explosion near end of duration
            if (elapsed >= BLACKHOLE_DURATION && !explosionOccurred) {
                endEffect(Global.getCombatEngine().getPlayerShip(), "AEG_BlackHole_Explosion");
                explosionOccurred = true;
            }

            // Fade out and remove plugin
            if (elapsed >= BLACKHOLE_DURATION + 1f) {
                Global.getCombatEngine().removePlugin(this);
            }

            // Create rings
            rings.add(new Ring(elapsed, location));
            renderRings();
        }


        private static class Ring {
            final float creationTime;
            final Vector2f location;

            Ring(float time, Vector2f loc) {
                creationTime = time;
                location = new Vector2f(loc);
            }
        }
        private static void applyVisualEffects(ShipAPI ship) {
            if (blackHolePosition == null) {
                return; // Ensure blackHolePosition is not null
            }

            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine != null) {
                // Add central ring effect
                engine.addHitParticle(blackHolePosition, new Vector2f(), RING_RADIUS, 1f, DURATION, RING_COLOR);
                // Add inner ring effect
                engine.addHitParticle(blackHolePosition, new Vector2f(), INNER_RING_RADIUS, 1f, DURATION, INNER_RING_COLOR);

                // Create the wavy magenta ring
                createWavyParticleRing(engine, blackHolePosition, RING_RADIUS, 258, RING_COLOR);

                // Create the thicker gradient orange ring
                createGradientParticleRing(engine, blackHolePosition, INNER_RING_RADIUS, 251);
            }
        }

        private static void createWavyParticleRing(CombatEngineAPI engine, Vector2f center, float radius, int particleCount, Color color) {
            for (int i = 0; i < particleCount; i++) {
                float angle = (float) (i * 2 * Math.PI / particleCount);
                float noise = (RANDOM.nextFloat() - 0.5f) * 10f; // Add some noise for wavy effect
                float x = center.x + (radius + noise) * (float) Math.cos(angle);
                float y = center.y + (radius + noise) * (float) Math.sin(angle);
                engine.addHitParticle(new Vector2f(x, y), new Vector2f(0, 0), 5f, 1f, DURATION, color);
            }
        }

        private static void createGradientParticleRing(CombatEngineAPI engine, Vector2f center, float radius, int particleCount) {
            for (int i = 0; i < particleCount; i++) {
                float angle = (float) (i * 2 * Math.PI / particleCount);
                float x = center.x + radius * (float) Math.cos(angle);
                float y = center.y + radius * (float) Math.sin(angle);
                Color color = getGradientColor(i, particleCount);
                engine.addHitParticle(new Vector2f(x, y), new Vector2f(0, 0), 10f, 1f, DURATION, color); // Increased size to 10f
            }
        }

        private static Color getGradientColor(int index, int total) {
            float ratio = (float) index / total;
            int red = (int) (255 * ratio + 255 * (1 - ratio));
            int green = (int) (165 * ratio + 69 * (1 - ratio));
            int blue = (int) (0 * ratio + 0 * (1 - ratio));
            return new Color(red, green, blue);
        }

        private static void applyBlackHolePull() {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || blackHolePosition == null) {
                return; // Ensure engine and blackHolePosition are not null
            }

            for (CombatEntityAPI entity : engine.getShips()) {
                if (entity instanceof ShipAPI && entity != engine.getPlayerShip()) {
                    float distance = MathUtils.getDistance(entity, blackHolePosition);
                    if (distance <= BLACK_HOLE_RADIUS) {
                        Vector2f pullVector = VectorUtils.getDirectionalVector(entity.getLocation(), blackHolePosition);
                        float strength = (1f - distance / BLACK_HOLE_RADIUS) * PULL_STRENGTH;
                        pullVector.scale(strength);
                        Vector2f.add(entity.getVelocity(), pullVector, entity.getVelocity());
                    }
                }
            }
        }
        private static void createBlackHoleParticles() {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || blackHolePosition == null) {
                return; // Ensure engine and blackHolePosition are not null
            }

            for (int i = 0; i < 10; i++) { // Increase particle frequency
                Vector2f particlePos = MathUtils.getRandomPointInCircle(blackHolePosition, BLACK_HOLE_RADIUS);
                Vector2f particleVel = Vector2f.sub(blackHolePosition, particlePos, null);
                particleVel.scale(0.05f); // Slow down the particles
                float size = 20f + (float) Math.random() * 10f; // Increase particle size
                float transparency = 0.5f + (float) Math.random() * 0.5f; // Random transparency
                engine.addHitParticle(particlePos, particleVel, size, transparency, 1f, RING_COLOR);

                // Add smoke effect with variance
                if (Math.random() < 0.5) {
                    Vector2f smokePos = MathUtils.getRandomPointInCircle(particlePos, 10f); // Add variance to smoke position
                    Color smokeColor = new Color(50, 50, 50, (int) (transparency * 255));
                    engine.addSmokeParticle(smokePos, particleVel, size * 1.5f, transparency, 1f, smokeColor);
                }

                // Ensure particles stop at the ring
                if (MathUtils.getDistance(particlePos, blackHolePosition) <= RING_RADIUS) {
                    particleVel.set(0, 0);
                }
            }
        }

        private static void endEffect(ShipAPI ship, String id) {
            isActive = false;
              // Create final explosion
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine != null && blackHolePosition != null) {
                engine.spawnExplosion(blackHolePosition, new Vector2f(), EXPLOSION_COLOR, 2500f, 1f);
                engine.addHitParticle(blackHolePosition, new Vector2f(), 3000, 1f, 1f, EXPLOSION_FRINGE_COLOR);
                //Explosion Sound
                Global.getSoundPlayer().playSound("explosion_large", 1f, 1f, blackHolePosition, new Vector2f());

                // Add shockwave effect
                engine.addNebulaParticle(blackHolePosition, new Vector2f(), 3500f, 1.5f, 0.1f, 0.3f, 1f, EXPLOSION_COLOR);

                // Deal 10,000 high explosive damage to all entities except the player ship
                for (ShipAPI s : engine.getShips()) {
                    if (s != null && s != ship) {
                        engine.applyDamage(s, blackHolePosition, 10000f, DamageType.HIGH_EXPLOSIVE, 5000f, true, false, ship);
                    }
                }
                // Reset the 11-second timer
                elapsedTime = 0f;
            }
        }
        private void renderRings() {
            Iterator<Ring> iter = rings.iterator();
            while (iter.hasNext()) {
                Ring ring = iter.next();
                float progress = (elapsed - ring.creationTime) / BLACKHOLE_DURATION;
                if (progress > 1f) {
                    iter.remove();
                    continue;
                }

                float size = 100f + progress * 400f;
                float alpha = 1f - progress;
                Color color = new Color(
                        RING_COLOR.getRed(), RING_COLOR.getGreen(), RING_COLOR.getBlue(),
                        (int)(alpha * RING_COLOR.getAlpha())
                );
                Global.getCombatEngine().addSmoothParticle(ring.location, new Vector2f(), size, 1f, 0.05f, color);
            }
        }
        @Override
        public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
            // No input processing needed for this effect
        }
    }
}
