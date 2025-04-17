package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class AEG_4g_HHImpact extends BaseShipSystemScript {

    private static final float SPEED_THRESHOLD = 230f;
    public static final float HIGH_SPEED_THRESHOLD = 600f;
    private static final float IMPACT_INTERVAL = 1f;
    public static final float BUILDUP_DURATION = 2f;
    private float lastImpactTime = 0f;
    private boolean explosionTriggered = false;
    private final Random random = new Random();

    @Override
    public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
        final ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null || state != State.ACTIVE) return;

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
                        Global.getCombatEngine().addFloatingText(fistPoint, "IMPACT!", 24f, Color.ORANGE, ship, 0.5f, 0.5f);

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
                                        spawnExplosionChunks(center, 5); // 20 chunks, tweak as needed

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
                }
            }

            private boolean isPointInsideBounds(Vector2f point, ShipAPI target) {
                return MathUtils.getDistance(point, target.getLocation()) <= target.getCollisionRadius();
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
        int numParticles = Math.min(5, random.nextInt(8) + 3);  // Randomize but cap at 5-10 particles
        for (int i = 0; i < numParticles; i++) {
            float angle = random.nextFloat() * 360f;
            float speed = 50f + random.nextFloat() * 100f;
            Vector2f velocity = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(speed);
            Vector2f point = new Vector2f(
                    location.x + 10f * (float) Math.cos(angle),
                    location.y + 10f * (float) Math.sin(angle)
            );
            Global.getCombatEngine().addHitParticle(point, velocity, 4f + random.nextFloat() * 6f, 2f, 1.5f + random.nextFloat() * 2f, new Color(255, 240, 180, 255));
            Global.getCombatEngine().addHitParticle(point, velocity, 5f + random.nextFloat() * 12f, 1.5f, 1.5f + random.nextFloat() * 2f, new Color(255, 160, 80, 225));
            Global.getCombatEngine().addHitParticle(point, velocity, 11f + random.nextFloat() * 18f, 1f, 1.5f + random.nextFloat() * 2f, new Color(100, 100, 100, 100));
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
}
