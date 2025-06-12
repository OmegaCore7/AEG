package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.Ship;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class AEG_4g_HHImpact extends BaseShipSystemScript {


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

        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);

                if (ship.getVelocity().length() < SPEED_THRESHOLD || currentTime - lastImpactTime < IMPACT_INTERVAL)
                    return;

                Vector2f fistPoint = transformRelativeToShip(ship, new Vector2f(70, 0));

                for (final ShipAPI target : Global.getCombatEngine().getShips()) {
                    if (target.getOwner() == ship.getOwner() || !target.isAlive()) continue;

                    if (isPointInsideBounds(fistPoint, target)) {

                        //Goldion Mode Not Active
                        if (!goldionActive) {
                            // DEFAULT SYSTEM MODE BEHAVIOR
                            Global.getCombatEngine().addFloatingText(fistPoint, "IMPACT!", 24f, Color.ORANGE, ship, 0.8f, 0.5f);

                            // Impact sparks and smoke
                            spawnImpactParticles(fistPoint);
                            spawnArmorSmoke(fistPoint);

                            if (ship.getVelocity().length() > HIGH_SPEED_THRESHOLD && !explosionTriggered) {
                                explosionTriggered = true;
                                // Increase break apart chance
                                target.getMutableStats().getBreakProb().modifyFlat(id, 1.0f); // Set to 100%

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

                                            // Spawn explosion Chunks
                                            spawnExplosionChunks(center, 5); // 5 chunks, tweak as needed

                                            // Final damage
                                            if (target.isAlive()) {
                                                Global.getCombatEngine().applyDamage(target, center, 8000f, DamageType.HIGH_EXPLOSIVE, 1000f, true, true, ship);
                                            }

                                            // Spawn explosion Chunks
                                            spawnExplosionChunks(center, 20); // 20 chunks, tweak as needed

                                            // 360° shockwave distortion ripple
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

                            lastImpactTime = currentTime;
                        }
                    } else {
                        // GOLDION MODE BEHAVIOR - ADD HERE
                        Global.getCombatEngine().addFloatingText(fistPoint, "GOLDION IMPACT!", 30f, new Color(255, 215, 0), ship, 1f, 0.6f);

                        // Extra radiant effects
                        spawnGoldionImpactParticles(fistPoint, ship);
                        // Optional extra: apply EMP arc, burn effect, or a temporary debuff
                        // Optional: Add custom shader glow or a flare burst

                        lastImpactTime = currentTime;
                    }
                  }
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
                Global.getCombatEngine().addHitParticle(point, velocity,
                        15f + random.nextFloat() * 20f,
                        1.5f + random.nextFloat() * 2f,
                        0.8f + random.nextFloat() * 1.0f,
                        new Color(255, 215, 0, 200));
            }
        }

        // Decorative random EMP arcs
        int numArcs = 3 + random.nextInt(4); // 3 to 6
        for (int i = 0; i < numArcs; i++) {
            float angle = random.nextFloat() * 360f;
            float distance = 100f + random.nextFloat() * 200f;
            Vector2f targetPoint = MathUtils.getPoint(location, distance, angle);
            Color arcColor = new Color(255, 255, 100 + random.nextInt(100), 100 + random.nextInt(155));

            Global.getCombatEngine().spawnEmpArcVisual(
                    null,
                    location,
                    null,
                    targetPoint,
                    arcColor,
                    arcColor
            );
        }

        // Add ripple distortion
        WaveDistortion ripple = new WaveDistortion();
        ripple.setLocation(location);
        ripple.setSize(100f + random.nextFloat() * 50f);
        ripple.setIntensity(10f);
        ripple.setArc(0, 360);
        ripple.fadeInIntensity(0.05f);
        ripple.fadeOutIntensity(0.3f);
        ripple.setLifetime(0.4f);
        DistortionShader.addDistortion(ripple);
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
}
