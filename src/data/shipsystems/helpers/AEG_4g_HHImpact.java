package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class AEG_4g_HHImpact extends BaseShipSystemScript {

    boolean goldionTriggered = false;
    private static final float SPEED_THRESHOLD = 230f;
    public static final float HIGH_SPEED_THRESHOLD = 539f;
    private static final float IMPACT_INTERVAL = 2f;
    public static final float BUILDUP_DURATION = 2f;
    private float lastImpactTime = 0f;
    private boolean explosionTriggered = false;
    private final Random random = new Random();

    @Override
    public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
        final ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null || state != State.ACTIVE) return;

        // GOLDION MODE CHECK - ADD HERE
        final boolean goldionActive = Boolean.TRUE.equals(ship.getCustomData().get("goldion_active"));
        // Prevent multiple plugins from being added
        String pluginKey = "AEG_HHImpactPlugin_" + ship.getId();
        if (Global.getCombatEngine().getCustomData().get(pluginKey) != null) return;
        Global.getCombatEngine().getCustomData().put(pluginKey, true);

        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {

            private boolean goldionTriggered = false;
            private boolean explosionTriggered = false;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);

                if (ship.getVelocity().length() < SPEED_THRESHOLD || currentTime - lastImpactTime < IMPACT_INTERVAL) {
                    return;
                }

                Vector2f fistPoint = transformRelativeToShip(ship, new Vector2f(70, 0));

                for (final ShipAPI target : Global.getCombatEngine().getShips()) {
                    if (target.getOwner() == ship.getOwner() || !target.isAlive()) continue;

                    if (isPointInsideBounds(fistPoint, target)) {

                        if (goldionActive) {
                            // Goldion mode active: only trigger Goldion once per impact
                            if (!goldionTriggered) {
                                goldionTriggered = true;
                                explosionTriggered = false;  // reset explosion just in case

                                Global.getCombatEngine().addFloatingText(fistPoint, "GOLDION IMPACT!", 30f, new Color(255, 215, 0), ship, 1f, 0.6f);
                                spawnGoldionImpactParticles(fistPoint, ship);
                                applyGoldionInfusion(ship, target);
                            }
                        } else {
                            // Normal mode: only trigger explosion and impact once per impact
                            if (!explosionTriggered) {
                                explosionTriggered = true;
                                goldionTriggered = false; // reset goldion just in case

                                Global.getCombatEngine().addFloatingText(fistPoint, "IMPACT!", 24f, Color.ORANGE, ship, 0.8f, 0.5f);
                                spawnImpactParticles(fistPoint);
                                spawnArmorSmoke(fistPoint);

                                if (ship.getVelocity().length() > HIGH_SPEED_THRESHOLD) {
                                    // Explosion logic here as you have it...
                                    // no need to check explosionTriggered here since it's set true above
                                    triggerExplosion(target, ship);
                                }
                            }
                        }

                        lastImpactTime = currentTime;
                        break; // Only trigger one impact per frame
                    }
                }
            }

            private void triggerExplosion(ShipAPI target, ShipAPI ship) {
                // Your existing explosion code here, wrapped in a method for clarity
                Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                    float timer = 0f;
                    final Vector2f center = target.getLocation();

                    @Override
                    public void advance(float amount, List<InputEventAPI> events) {
                        if (!target.isAlive()) {
                            Global.getCombatEngine().removePlugin(this);
                            return;
                        }
                        timer += amount;
                        if (timer >= BUILDUP_DURATION) {
                            spawnExplosionChunks(center, 5);
                            if (target.isAlive()) {
                                Global.getCombatEngine().applyDamage(target, center, 8000f, DamageType.HIGH_EXPLOSIVE, 1000f, true, true, ship);
                            }
                            spawnExplosionChunks(center, 20);
                            WaveDistortion ripple = new WaveDistortion();
                            ripple.setLocation(center);
                            ripple.setSize(350f);
                            ripple.setIntensity(25f);
                            ripple.setArc(0, 360);
                            ripple.fadeInIntensity(0.1f);
                            ripple.fadeOutIntensity(0.5f);
                            ripple.setLifetime(0.7f);
                            ripple.setAutoFadeIntensityTime(0.1f);
                            DistortionShader.addDistortion(ripple);
                            Global.getCombatEngine().removePlugin(this);
                        }
                    }
                });
            }

        boolean isPointInsideBounds(Vector2f point, ShipAPI target) {
                float collisionRadius = target.getCollisionRadius();
                float bufferPercent = 0.15f;  // 15% buffer to exclude shields
                float adjustedRadius = collisionRadius * (1f - bufferPercent);
                if (adjustedRadius < 0f) adjustedRadius = 0f;  // Safety check
                return MathUtils.getDistance(point, target.getLocation()) <= adjustedRadius;
            }
        });
    }
    private void spawnExplosionChunks(Vector2f center, int count) {
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * 360f;
            float speed = 375f + random.nextFloat() * 50f; // Fast and erratic
            Vector2f velocity = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(speed);

            Vector2f spawnPoint = new Vector2f(
                    center.x + (float)Math.cos(Math.toRadians(angle)) * 10f,
                    center.y + (float)Math.sin(Math.toRadians(angle)) * 10f
            );

            float size = 10f + random.nextFloat() * 25f;
            float duration = 2f + random.nextFloat() * 2f;

            // Spewing metal chunk — addHitParticle with low fade
            Global.getCombatEngine().addHitParticle(
                    spawnPoint,
                    velocity,
                    size,
                    1.5f,
                    duration,
                    new Color(50, 255, 100, 200) // metallic grey
            );

            // Optional glow on some chunks
            if (random.nextFloat() < 0.3f) {
                Global.getCombatEngine().addHitParticle(
                        spawnPoint,
                        velocity,
                        size * 1.5f,
                        1f,
                        duration * 0.5f,
                        new Color(255, 100, 50, 200) // glowing orange
                );
            }
        }
    }
    private void spawnImpactParticles(Vector2f location) {
        int numParticles = 5 + random.nextInt(6); // 5 to 10 inclusive
        for (int i = 0; i < numParticles; i++) {
            float angle = random.nextFloat() * 360f;
            float speed = 50f + random.nextFloat() * 100f;
            Vector2f velocity = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(speed);
            Vector2f point = new Vector2f(
                    location.x + 10f * (float) Math.cos(angle),
                    location.y + 10f * (float) Math.sin(angle)
            );
            Global.getCombatEngine().addHitParticle(point, velocity, 4f + random.nextFloat() * 6f, 2f, 1.5f + random.nextFloat() * 1.8f, new Color(255, 240, 180, 255));
            Global.getCombatEngine().addHitParticle(point, velocity, 5f + random.nextFloat() * 12f, 1.5f, 1.6f + random.nextFloat() * 1.9f, new Color(255, 160, 80, 225));
            Global.getCombatEngine().addHitParticle(point, velocity, 11f + random.nextFloat() * 18f, 1f, 1.7f + random.nextFloat() * 2f, new Color(100, 100, 100, 100));
        }
    }
    private void spawnGoldionImpactParticles(Vector2f location, ShipAPI ship) {
        int numParticles = 8 + random.nextInt(6); // 8 to 13 particles
        for (int i = 0; i < numParticles; i++) {
            float angle = random.nextFloat() * 360f;
            float speed = 75f + random.nextFloat() * 150f;
            Vector2f velocity = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(speed);
            Vector2f point = new Vector2f(
                    location.x + 10f * (float) Math.cos(Math.toRadians(angle)),
                    location.y + 10f * (float) Math.sin(Math.toRadians(angle))
            );

            Global.getCombatEngine().addHitParticle(point, velocity,
                    10f + random.nextFloat() * 10f,
                    2f,
                    1.2f + random.nextFloat() * 1.5f,
                    new Color(255, 255, 150 + random.nextInt(100), 255));

            if (random.nextFloat() < 0.4f) {
                Global.getCombatEngine().addHitParticle(point, velocity, 5f + random.nextFloat() * 12f, 1.5f, 1.6f + random.nextFloat() * 1.9f, new Color(225, 250, 200, 225));
                Global.getCombatEngine().addHitParticle(point, velocity,
                        15f + random.nextFloat() * 20f,
                        1.5f + random.nextFloat() * 2f,
                        0.8f + random.nextFloat() * 1.0f,
                        new Color(255, 215, 0, 200));
                Global.getCombatEngine().addHitParticle(point, velocity, 11f + random.nextFloat() * 18f, 1f, 1.7f + random.nextFloat() * 2f, new Color(175, 175, 100, 125));
            }
        }
    }

    private void spawnArmorSmoke(Vector2f center) {
        for (int i = 0; i < 5; i++) {
            float angle = random.nextFloat() * 360f;
            float speed = 40f + random.nextFloat() * 30f;
            Vector2f velocity = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(speed);
            Vector2f smokePoint = new Vector2f(
                    center.x + 10f * (float) Math.cos(angle),
                    center.y + 10f * (float) Math.sin(angle)
            );
            Global.getCombatEngine().addSmokeParticle(smokePoint, velocity, 6f + random.nextFloat() * 8f, 1f, 2f, new Color(100, 200, 150, 100));
        }
    }

    private Vector2f transformRelativeToShip(ShipAPI ship, Vector2f relative) {
        float facing = ship.getFacing() * (float) Math.PI / 180f;
        float cos = (float) Math.cos(facing);
        float sin = (float) Math.sin(facing);
        return new Vector2f(
                ship.getLocation().x + relative.x * cos - relative.y * sin,
                ship.getLocation().y + relative.x * sin + relative.y * cos
        );
    }
    // --- NEW: Goldion Infusion helper ---
    private void applyGoldionInfusion(ShipAPI source, ShipAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();

        // Avoid stacking multiple effects on the same target
        if (engine.getCustomData().containsKey("goldion_infusion_" + target.getId())) return;

        engine.getCustomData().put("goldion_infusion_" + target.getId(), true);
        engine.addPlugin(new GoldionInfusionEffect(source, target, random));
    }

    // --- Inner static class for infusion effect ---
    private static class GoldionInfusionEffect implements EveryFrameCombatPlugin {
        private final ShipAPI target;
        private final ShipAPI source;
        private final CombatEngineAPI engine;

        private float timeElapsed = 0f;
        private float orbTimer = 0f;

        private final float DURATION = 20f;
        private final float DAMAGE_OVER_TIME = 50f;
        private boolean exploded = false;

        private final Random random;

        public GoldionInfusionEffect(ShipAPI source, ShipAPI target, Random random) {
            this.source = source;
            this.target = target;
            this.engine = Global.getCombatEngine();
            this.random = random;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (engine.isPaused() || target == null || !target.isAlive() || exploded) return;

            timeElapsed += amount;
            orbTimer += amount;

            if (!target.getFluxTracker().isOverloaded()) {
                target.getFluxTracker().forceOverload(1f);
            }

            engine.applyDamage(target, target.getLocation(), DAMAGE_OVER_TIME * amount, DamageType.FRAGMENTATION, 0f, false, false, source);

            float intensity = timeElapsed / DURATION;
            Color jitterColor = new Color(255, (int) (200 + 55 * intensity), (int) (50 + 205 * intensity), 255);
            target.setJitter(this, jitterColor, 1f * intensity, 4, 10f * intensity);
            // Add the enhanced buildup plugin to handle visuals and explosion
            Global.getCombatEngine().addPlugin(new BuildupEffectPlugin(target, source, BUILDUP_DURATION));

            if (orbTimer >= 1f) {
                orbTimer = 0f;
                spawnHelixOrb();
            }

            if (timeElapsed >= DURATION) {
                triggerFinalExplosion();
                engine.removePlugin(this);
                engine.getCustomData().remove("goldion_infusion_" + target.getId());
            }
        }

        @Override
        public void renderInWorldCoords(ViewportAPI viewport) {

        }

        @Override
        public void renderInUICoords(ViewportAPI viewport) {

        }

        @Override
        public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
            // No input handling needed here
        }
        private void spawnHelixOrb() {
            CombatEntityAPI nearestEnemy = getNearestEnemy(target, target.getLocation(), 2000f);
            if (nearestEnemy != null) {
                Vector2f spawnLoc = new Vector2f(target.getLocation());
                float angle = VectorUtils.getAngle(spawnLoc, nearestEnemy.getLocation());
                engine.addPlugin(new data.weapons.scripts.AEG_4g_right_helixBall(source, spawnLoc, angle, engine));
            }
        }

        private void triggerFinalExplosion() {
            exploded = true;

            Vector2f location = target.getLocation();
            Vector2f velocity = target.getVelocity();

            // 1. Massive blinding flash
            engine.spawnExplosion(location, velocity, new Color(255, 255, 200), 500f, 2.5f);

            // 2. Gold-colored energy burst
            for (int i = 0; i < 12; i++) {
                float angle = random.nextFloat() * 360f;
                float speed = 150f + random.nextFloat() * 150f;
                Vector2f dir = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(speed);
                Color gold = new Color(255, 215 + random.nextInt(40), 50 + random.nextInt(80), 255);

                engine.addHitParticle(location, dir, 20f + random.nextFloat() * 40f, 2f, 2f + random.nextFloat() * 1f, gold);
            }

            // 3. Massive screen ripple
            WaveDistortion ripple = new WaveDistortion();
            ripple.setLocation(location);
            ripple.setSize(600f); // larger size
            ripple.setIntensity(40f);
            ripple.setArc(0, 360);
            ripple.fadeInIntensity(0.05f);
            ripple.fadeOutIntensity(0.8f);
            ripple.setLifetime(1f);
            ripple.setAutoFadeIntensityTime(0.1f);
            DistortionShader.addDistortion(ripple);

            // 6. Super loud crushing sound (replace with your own if needed)
            Global.getSoundPlayer().playSound("devastator_explosion", 1.5f, 1.2f, location, velocity);
            Global.getSoundPlayer().playSound("realitydisruptor_emp_impact", 1f, 1.5f, location, velocity);
            Global.getSoundPlayer().playSound("terrain_hyperspace_storm", 0.8f, 1f, location, velocity);

            // 7. Overkill damage
            engine.applyDamage(target, location, 9999999f, DamageType.HIGH_EXPLOSIVE, 9999f, true, false, source);

            // 8. Add a floating text for flair
            engine.addFloatingText(location, "Hikari Ni Nare!!!!!!!!!", 48f, new Color(255, 255, 200), target, 0.5f, 1f);

            // Clean up plugin
            engine.removePlugin(this);
        }

        private CombatEntityAPI getNearestEnemy(ShipAPI source, Vector2f loc, float range) {
            CombatEntityAPI nearest = null;
            float nearestDist = Float.MAX_VALUE;
            for (ShipAPI ship : engine.getShips()) {
                if (ship == source || ship.getOwner() == source.getOwner() || !ship.isAlive()) continue;
                float dist = MathUtils.getDistance(loc, ship.getLocation());
                if (dist < nearestDist && dist <= range) {
                    nearest = ship;
                    nearestDist = dist;
                }
            }
            return nearest;
        }

        @Override
        public void init(CombatEngineAPI engine) {

        }
    }
    public static class BuildupEffectPlugin extends BaseEveryFrameCombatPlugin {
        private ShipAPI target;
        private ShipAPI source;
        private float timer = 0f;
        private final Vector2f center;
        private boolean soundStarted = false;
        private boolean flavorTextShown = false;
        private final Random random = new Random();
        private final float buildupDuration;

        public BuildupEffectPlugin(ShipAPI target, ShipAPI source, float buildupDuration) {
            this.target = target;
            this.source = source;
            this.buildupDuration = buildupDuration;
            this.center = target.getLocation();
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (!target.isAlive()) {
                Global.getCombatEngine().removePlugin(this);
                return;
            }
            timer += amount;

            // Lightning arcs with increasing frequency
            if (random.nextFloat() < 0.05f * (timer / buildupDuration)) {
                Vector2f from = MathUtils.getRandomPointInCircle(center, 100f);
                Vector2f to = MathUtils.getRandomPointInCircle(center, 100f);
                Global.getCombatEngine().spawnEmpArcVisual(
                        from, null, to, null,
                        10f,
                        new Color(255, 255, 150, 150),
                        new Color(255, 200, 50, 255)
                );
            }

            // Golden nebula/mist leaking particles
            if (random.nextFloat() < 0.1f) {
                Vector2f leakPoint = MathUtils.getRandomPointInCircle(center, 120f);
                Vector2f leakVel = MathUtils.getRandomPointInCircle(null, 15f);
                Global.getCombatEngine().addNebulaParticle(
                        leakPoint,
                        leakVel,
                        20f + random.nextFloat() * 20f,
                        1.8f,
                        0.5f,
                        0.5f,
                        2f,
                        new Color(255, 220, 100, 120),
                        true
                );
            }

            // Small beams of light leaking outward
            if (random.nextFloat() < 0.07f) {
                float radius = 150f;
                float angle = random.nextFloat() * 360f;
                Vector2f outsidePoint = MathUtils.getPointOnCircumference(null, radius, angle);
                Global.getCombatEngine().spawnEmpArcVisual(
                        center, null,
                        outsidePoint, null,
                        15f,
                        new Color(255, 255, 180, 180),
                        new Color(255, 230, 130, 220)
                );
            }

            // Show flavor text at 10 seconds
            if (timer > 5f && !flavorTextShown) {
                Global.getCombatEngine().addFloatingText(center, "Self-repair protocols unresponsive!.", 24f, Color.RED, null, 0.5f, 1f);
                flavorTextShown = true;
            }
            // Show flavor text at 10 seconds
            if (timer > 10f && !flavorTextShown) {
                Global.getCombatEngine().addFloatingText(center, "Structural Integrity... Compromised!", 28f, Color.ORANGE, null, 0.5f, 1f);
                flavorTextShown = true;
            }
            // Show flavor text at 15 seconds
            if (timer > 15f && !flavorTextShown) {
                Global.getCombatEngine().addFloatingText(center, "Matter Breakdown at the Atomic Level", 38f, Color.YELLOW, null, 0.5f, 1f);
                flavorTextShown = true;
            }

                Global.getCombatEngine().removePlugin(this);
            }


        private void spawnExplosionChunks(Vector2f loc, int count) {
            for (int i = 0; i < count; i++) {
                Vector2f vel = MathUtils.getPointOnCircumference(null, 100f, random.nextFloat() * 360f);
                Global.getCombatEngine().addHitParticle(loc, vel, 15f + random.nextFloat() * 15f, 1f, 0.5f, new Color(255, 200, 100, 150));
            }
        }
    }

}
