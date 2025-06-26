package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import data.weapons.scripts.AEG_4g_HelixOrbManager;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class AEG_4g_HHGoldionBreaker {

    private static final Random random = new Random();

    public static void execute(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();

        ShipAPI target = findOptimalTarget(ship);
        if (target == null) {
            engine.addFloatingText(ship.getLocation(), "No viable Goldion Breaker target", 20f, Color.RED, ship, 0.5f, 1f);
            return;
        }

        Vector2f launchPoint = MathUtils.getPointOnCircumference(ship.getLocation(), 75f, ship.getFacing()); // Approx. "fist" position
        float angle = VectorUtils.getAngle(launchPoint, target.getLocation());

        engine.addFloatingText(ship.getLocation(), "IGNITION OVERLOAD: GOLDION BREAKER!", 40f, new Color(255, 230, 80), ship, 1f, 1f);
        engine.addPlugin(new BreakerProjectile(ship, target, launchPoint, angle));
// Expanding nebula burst
        for (int i = 0; i < 30; i++) {
            Vector2f randomVel = MathUtils.getRandomPointInCircle(null, MathUtils.getRandomNumberInRange(20f, 200f));
            float size = MathUtils.getRandomNumberInRange(60f, 150f);
            float duration = MathUtils.getRandomNumberInRange(1.5f, 3f);
            float rampUp = MathUtils.getRandomNumberInRange(0.1f, 0.5f);
            float alphaMult = MathUtils.getRandomNumberInRange(0.5f, 1f);

            Color glowColor = new Color(
                    255,
                    200 + (int)(Math.random() * 55),
                    100 + (int)(Math.random() * 100),
                    (int)(180 * alphaMult)
            );
            float alpha = 0.5f + random.nextFloat() * 0.5f;
            glowColor = new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), (int)(255 * alpha));


            engine.addNebulaParticle(
                    launchPoint,
                    randomVel,
                    size,
                    MathUtils.getRandomNumberInRange(1.5f, 2.5f),
                    rampUp,
                    0.4f,
                    duration,
                    glowColor
            );
        }

        // End goldion mode
        ship.setCustomData("goldion_active", false);
        ship.setCustomData("goldion_timer", 0f);
        ship.setCustomData("goldion_boost_applied", false);
    }

    private static ShipAPI findOptimalTarget(ShipAPI ship) {
        List<ShipAPI> enemies = CombatUtils.getShipsWithinRange(ship.getLocation(), 2000f);
        return enemies.stream()
                .filter(e -> e.getOwner() != ship.getOwner() && !e.isFighter() && !e.isHulk())
                .max(Comparator.comparingDouble(e -> e.getHullSize().ordinal() + getNearbyEnemyWeight(e)))
                .orElse(null);
    }

    private static float getNearbyEnemyWeight(ShipAPI ship) {
        float weight = 0f;
        for (ShipAPI s : CombatUtils.getShipsWithinRange(ship.getLocation(), 400f)) {
            if (s.getOwner() == ship.getOwner() || s == ship) continue;
            weight += 0.5f;
        }
        return weight;
    }

    private static void spawnBreakerParticles(Vector2f loc, CombatEngineAPI engine) {
        for (int i = 0; i < 40; i++) {
            Vector2f vel = MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(100f, 250f), MathUtils.getRandom().nextFloat() * 360f);
            engine.addHitParticle(loc, vel, 20f + MathUtils.getRandom().nextFloat() * 30f, 1f, 1f, new Color(255, 220, 100, 180));
        }
    }

    // ---- Goldion Infusion: Public method ----
    public static void applyGoldionInfusion(ShipAPI source, ShipAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();

        if (engine.getCustomData().containsKey("goldion_infusion_" + target.getId())) return;

        engine.getCustomData().put("goldion_infusion_" + target.getId(), true);
        engine.addFloatingText(target.getLocation(), "Total Atomization Initiated", 25f, new Color(255, 200, 0), target, 1f, 0.5f);
        engine.addPlugin(new GoldionInfusionEffect(source, target));
        //Helix Orb Manager**
        engine.addPlugin(new AEG_4g_HelixOrbManager(engine, target));  // target is the infused ship
    }

    // ---- Inner plugin class: Infusion effect ----
    public static class GoldionInfusionEffect implements EveryFrameCombatPlugin {
        private final ShipAPI target, source;
        private final CombatEngineAPI engine;
        private final float DURATION = 20f;
        private float timeElapsed = 0f, orbTimer = 0f;
        private boolean exploded = false;

        public GoldionInfusionEffect(ShipAPI source, ShipAPI target) {
            this.source = source;
            this.target = target;
            this.engine = Global.getCombatEngine();
        }

        @Override
        public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (engine.isPaused() || target == null || !target.isAlive() || exploded) return;

            timeElapsed += amount;
            orbTimer += amount;

            if (!target.getFluxTracker().isOverloaded()) {
                target.getFluxTracker().forceOverload(1f);
            }

            float intensity = timeElapsed / DURATION;
            target.setJitter(this, new Color(255, 200 + (int)(55 * intensity), 50 + (int)(205 * intensity)), 1f * intensity, 4, 10f * intensity);

            float orbDelay = 1f - (timeElapsed / DURATION) * 0.7f; // speeds up over time
            if (orbTimer >= orbDelay) {
                orbTimer = 0f;
                spawnHelixOrb();
            }

            engine.addPlugin(new BuildupEffectPlugin(target, source, 20f));

            if (timeElapsed >= DURATION) {
                triggerFinalExplosion();
                engine.removePlugin(this);
                engine.getCustomData().remove("goldion_infusion_" + target.getId());
            }
        }

        private void spawnHelixOrb() {
            CombatEntityAPI nearest = getNearestEnemy(target.getLocation(), 2000f);
            if (nearest != null) {
                float angle = VectorUtils.getAngle(target.getLocation(), nearest.getLocation());
                engine.addPlugin(new data.weapons.scripts.AEG_4g_right_helixBall(source, target.getLocation(), angle, engine, 0)); // or whatever int you want
            }
        }

        private CombatEntityAPI getNearestEnemy(Vector2f loc, float range) {
            CombatEntityAPI nearest = null;
            float minDist = Float.MAX_VALUE;
            for (ShipAPI ship : engine.getShips()) {
                if (ship == source || ship == target || ship.getOwner() == source.getOwner() || !ship.isAlive()) continue;
                float dist = MathUtils.getDistance(loc, ship.getLocation());
                if (dist < minDist && dist <= range) {
                    nearest = ship;
                    minDist = dist;
                }
            }
            return nearest;
        }
        private void triggerFinalExplosion() {
            exploded = true;
            Vector2f loc = target.getLocation();
            Vector2f vel = target.getVelocity();

            // Massive explosion visuals
            engine.spawnExplosion(loc, vel, new Color(255, 255, 200), 500f, 2.5f);

            // Big gold hit particles
            for (int i = 0; i < 12; i++) {
                Vector2f dir = MathUtils.getPointOnCircumference(null, 150f + random.nextFloat() * 150f, random.nextFloat() * 360f);
                Color gold = new Color(255, 215 + random.nextInt(40), 50 + random.nextInt(80), 255);
                engine.addHitParticle(loc, dir, 20f + random.nextFloat() * 40f, 1f, 2f, gold);
            }

            // Flash burst - large soft particle to light up area
            engine.addHitParticle(loc, new Vector2f(), 1000f, 1f, 0.3f, new Color(255, 255, 220, 180));

            // Slow motion for 1.5 seconds to make the impact dramatic
            engine.getTimeMult().modifyMult("goldion_final_explosion_slowmo", 0.15f);
            engine.addPlugin(new BaseEveryFrameCombatPlugin() {
                float time = 0f;
                final float holdDuration = 0.5f;   // time to stay at 0.15x
                final float totalDuration = 1.5f;  // total duration including ramp-up

                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    time += amount;

                    if (time < holdDuration) {
                        // Hold slowmo
                        engine.getTimeMult().modifyMult("goldion_final_explosion_slowmo", 0.15f);
                    } else if (time < totalDuration) {
                        // Ease out from 0.15 to 1.0 over remaining time
                        float progress = (time - holdDuration) / (totalDuration - holdDuration);
                        // Optional easing: quadratic ease-out
                        float eased = 1f - (1f - progress) * (1f - progress);
                        float timeMult = 0.15f + (1f - 0.15f) * eased;
                        engine.getTimeMult().modifyMult("goldion_final_explosion_slowmo", timeMult);
                    } else {
                        // Done
                        engine.getTimeMult().unmodify("goldion_final_explosion_slowmo");
                        engine.removePlugin(this);
                    }
                }
            });
            // Play a dramatic explosion sound - replace with your preferred sound if needed
            Global.getSoundPlayer().playSound("realitydisruptor_emp_impact", 1f, 1f, loc, vel);

            // Optional: Shockwave distortion if shaderLib mod is enabled
            if (Global.getSettings().getModManager().isModEnabled("shaderLib")) {
                WaveDistortion wave = new WaveDistortion(loc, vel);
                wave.setSize(700f);
                wave.setIntensity(70f);
                wave.setLifetime(1.5f);
                wave.fadeInIntensity(0.1f);
                wave.fadeOutIntensity(1.0f);
                DistortionShader.addDistortion(wave);
            }

            // Deal massive damage here — adjust numbers as needed
            engine.applyDamage(target, loc, 999999f, DamageType.HIGH_EXPLOSIVE, 5000f, true, false, source);
        }


        @Override
        public void renderInWorldCoords(ViewportAPI viewport) {}
        @Override
        public void renderInUICoords(ViewportAPI viewport) {}

        @Override
        public void init(CombatEngineAPI engine) {

        }
    }

    // ---- Inner plugin class: Buildup particles ----
    public static class BuildupEffectPlugin implements EveryFrameCombatPlugin {
        private final ShipAPI target, source;
        private final float maxTime;
        private float elapsed = 0f;
        private final CombatEngineAPI engine;

        public BuildupEffectPlugin(ShipAPI target, ShipAPI source, float maxTime) {
            this.target = target;
            this.source = source;
            this.maxTime = maxTime;
            this.engine = Global.getCombatEngine();
        }

        @Override
        public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (engine.isPaused()) return;
            if (elapsed > maxTime || target == null || !target.isAlive()) {
                engine.removePlugin(this);
                return;
            }

            elapsed += amount;
            Vector2f loc = target.getLocation();
            float size = 40f + 30f * (elapsed / maxTime);
            float alpha = 0.5f + 0.5f * (elapsed / maxTime);
            Color c = new Color(255, 180 + (int)(75f * (elapsed / maxTime)), 50 + (int)(205f * (elapsed / maxTime)), (int)(255 * alpha));
            engine.addHitParticle(loc, new Vector2f(), size, 1f, 0.5f, c);
        }

        @Override
        public void renderInWorldCoords(ViewportAPI viewport) {}
        @Override
        public void renderInUICoords(ViewportAPI viewport) {}

        @Override
        public void init(CombatEngineAPI engine) {

        }
    }
    public static class BreakerProjectile implements EveryFrameCombatPlugin {
        private final ShipAPI source, target;
        private final Vector2f position;
        private final Vector2f velocity;
        private final CombatEngineAPI engine;
        private boolean hit = false;

        private final float SPEED = 1000f;
        private final float HIT_RANGE = 75f;

        private float rotation = 0f; // for rotating effects
        private final Random random = new Random();

        public BreakerProjectile(ShipAPI source, ShipAPI target, Vector2f launch, float angle) {
            this.source = source;
            this.target = target;
            this.position = new Vector2f(launch);
            this.velocity = MathUtils.getPoint(new Vector2f(), SPEED, angle);
            this.engine = Global.getCombatEngine();
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (engine.isPaused() || hit || !target.isAlive()) return;

            // Update rotation for visual effects
            rotation += 360f * amount; // full rotation per second
            if (rotation > 360f) rotation -= 360f;

            // Move projectile
            Vector2f move = new Vector2f(velocity);
            move.scale(amount);
            Vector2f.add(position, move, position);

            // Check for hit
            float dist = MathUtils.getDistance(position, target.getLocation());
            if (dist <= HIT_RANGE) {
                hit = true;
                triggerExplosion();
                return;
            }

            renderVisuals();
        }

        private void triggerExplosion() {
            Vector2f vel = new Vector2f(velocity);

            // Big gold explosion visuals
            engine.spawnExplosion(position, vel, new Color(255, 220, 100), 600f, 2.5f);
            engine.addSmoothParticle(position, vel, 400f, 3f, 2f, new Color(255, 240, 180, 255));
            engine.addHitParticle(position, vel, 250f, 1f, 1.5f, new Color(255, 255, 200));

            // Flash burst effect (large, soft hit particle)
            engine.addHitParticle(position, new Vector2f(), 800f, 1f, 0.3f, new Color(255, 255, 220, 180));

            Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                float time = 0f;
                public void advance(float amount, List<InputEventAPI> events) {
                    time += amount;
                    if (time > 1.2f) {
                        engine.getTimeMult().unmodify("goldion_dramatic");
                        engine.removePlugin(this);
                    }
                }

            });

            // Play dramatic sound
            Global.getSoundPlayer().playSound("riftcascade_rift", 1f, 1.3f, position, vel);

            // Shader shockwave distortion (if available)
            if (Global.getSettings().getModManager().isModEnabled("shaderLib")) {
                WaveDistortion wave = new WaveDistortion(position, vel);
                wave.setSize(500f);
                wave.setIntensity(50f);
                wave.setLifetime(1.2f);
                wave.fadeInIntensity(0.1f);
                wave.fadeOutIntensity(1.0f);
                DistortionShader.addDistortion(wave);
            }

            // Apply Goldion Infusion effect
            AEG_4g_HHGoldionBreaker.applyGoldionInfusion(source, target);

            // Clean up projectile
            engine.removePlugin(this);
        }

        private void renderVisuals() {
            // Pulsating core glow
            float pulseSize = 280f + 40f * (float)Math.sin(rotation * Math.PI / 180f);
            engine.addSmoothParticle(position, new Vector2f(), pulseSize, 2.5f, 0.2f, new Color(255, 240, 180, 200));

            // Aura particles
            for (int i = 0; i < 6; i++) {
                Vector2f puff = MathUtils.getRandomPointInCircle(null, 30f);
                engine.addSmoothParticle(position, puff, 120f, 1.3f, 0.3f, new Color(255, 230, 120, 180));
            }

            // Trail behind projectile
            Vector2f behind = new Vector2f(velocity);
            behind.normalise();
            behind.scale(-50f);
            Vector2f trailPoint = Vector2f.add(position, behind, null);
            engine.addNebulaParticle(trailPoint, new Vector2f(), 100f, 2f, 0.2f, 0.3f, 1.5f, new Color(255, 180, 80, 180));

            // Rotating ring glow (two particles opposite each other)
            float ringRadius = 40f;
            float angle1 = rotation;
            float angle2 = rotation + 180f; // opposite side

            Vector2f ringPoint1 = MathUtils.getPointOnCircumference(position, ringRadius, angle1);
            Vector2f ringPoint2 = MathUtils.getPointOnCircumference(position, ringRadius, angle2);

            engine.addHitParticle(ringPoint1, new Vector2f(), 20f, 0.5f, 0.3f, new Color(255, 220, 100, 200));
            engine.addHitParticle(ringPoint2, new Vector2f(), 20f, 0.5f, 0.3f, new Color(255, 220, 100, 200));

            // Random sparks flickering around projectile
            if (random.nextFloat() < 0.3f) {  // ~30% chance each frame
                Vector2f sparkOffset = MathUtils.getRandomPointInCircle(null, 35f);
                Vector2f sparkVel = MathUtils.getRandomPointInCircle(null, 50f);
                engine.addHitParticle(Vector2f.add(position, sparkOffset, null), sparkVel, 10f + random.nextFloat() * 10f, 0.5f, 0.3f, new Color(255, 255, 150, 255));
            }

            // Subtle light beam/streak in direction of travel
            Vector2f beamStart = new Vector2f(position);
            Vector2f beamEnd = new Vector2f(velocity);
            beamEnd.normalise();
            beamEnd.scale(80f);
            Vector2f.add(beamStart, beamEnd, beamEnd);

            engine.addNebulaParticle(beamStart, new Vector2f(), 15f, 0.4f, 0.1f, 0.3f, 0.5f, new Color(255, 220, 150, 150));
            engine.addNebulaParticle(beamEnd, new Vector2f(), 25f, 0.3f, 0.1f, 0.3f, 0.4f, new Color(255, 220, 150, 120));
        }

        @Override public void init(CombatEngineAPI engine) {}
        @Override public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {}
        @Override public void renderInWorldCoords(ViewportAPI viewport) {}
        @Override public void renderInUICoords(ViewportAPI viewport) {}
    }
}
