package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

public class AEG_4G_SpectacularBeamEffect implements BeamEffectPlugin {
    private int pulseCount = 0;
    private float pulseTimer = 0f;
    private static final float PULSE_INTERVAL = 0.3f;

    private static final float EMP_DAMAGE = 100f;
    private static final float STRIKE_DAMAGE = 50f;
    private static final float KINETIC_DAMAGE = 100f;
    private static final float HIGH_EXPLOSIVE_DAMAGE = 100f;
    private static final float FRAGMENTATION_DAMAGE = 50f;
    private static final float ARMOR_DAMAGE_MULTIPLIER = 2f;
    private static final float LIGHTNING_INTERVAL = 1f;
    private static final float SMOKE_START_SIZE = 5f;
    private static final float SMOKE_END_SIZE = 20f;
    private static final float SMOKE_DURATION = 1f;
    private static final float FIGHTER_DAMAGE_MULTIPLIER = 5f;
    private static final float MALFUNCTION_INCREMENT = 0.01f;
    private float baseBeamWidth = 10f; // from JSON
    private float lightningTimer = 0f;
    private float beamHitDuration = 0f;
    private Random random = new Random();

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine.isPaused()) return;

        ShipAPI firingShip = beam.getSource();
        boolean isGoldionActive = false;

        if (firingShip != null && firingShip.getCustomData().get("goldion_active") instanceof Boolean) {
            isGoldionActive = (Boolean) firingShip.getCustomData().get("goldion_active");
        }

        CombatEntityAPI target = beam.getDamageTarget();
        Vector2f hitPoint = beam.getTo();

        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            float damageMultiplier = ship.isFighter() ? FIGHTER_DAMAGE_MULTIPLIER : 1f;

            if (ship.getShield() != null && ship.getShield().isWithinArc(hitPoint)) {
                beam.getDamage().setDamage(beam.getDamage().getDamage() + KINETIC_DAMAGE * damageMultiplier);
                lightningTimer += amount;
                if (lightningTimer >= LIGHTNING_INTERVAL) {
                    lightningTimer = 0f;
                    spawnEmpLightning(engine, hitPoint, ship, damageMultiplier);
                }
            } else {
                beam.getDamage().setDamage(beam.getDamage().getDamage() + HIGH_EXPLOSIVE_DAMAGE * ARMOR_DAMAGE_MULTIPLIER * damageMultiplier);
                spawnEnergySplash(engine, hitPoint);
                spawnSmoke(engine, hitPoint);
                beamHitDuration += amount;
                ship.getMutableStats().getCriticalMalfunctionChance().modifyFlat("SpectacularBeamEffect", beamHitDuration * MALFUNCTION_INCREMENT);
            }
        } else {
            beamHitDuration = 0f;
        }

        // ⭐ Goldion Mode Visual FX
        if (isGoldionActive && firingShip != null && target instanceof ShipAPI) {
            ShipAPI targetShip = (ShipAPI) target;

            pulseTimer += amount;
            if (pulseTimer >= PULSE_INTERVAL) {
                pulseTimer -= PULSE_INTERVAL;
                pulseCount++;

                // Orb reached end: do jitter and splash only now
                if (targetShip != null) {
                    if (pulseCount % 4 == 0) {
                        // Big pulse: big splash + explosion + more damage + big jitter
                        targetShip.setJitter(this, new Color(255, 215 + random.nextInt(30), 0, 200), 1.5f, 15, 16f);
                        targetShip.setJitterUnder(this, new Color(255, 180 + random.nextInt(30), 0, 150), 1.5f, 10, 16f);

                        // Layered explosions for glow effect
                        engine.spawnExplosion(beam.getTo(), new Vector2f(), new Color(255, 215, 130, 220), 70f, 0.6f);
                        engine.spawnExplosion(beam.getTo(), new Vector2f(), new Color(255, 255, 180, 150), 50f, 0.9f);

                        // Ripple effect
                        float rippleSize = 100f + random.nextFloat() * 40f;
                        float rippleDuration = 0.5f + random.nextFloat() * 0.3f;
                        engine.addHitParticle(beam.getTo(), new Vector2f(), rippleSize, rippleDuration, 0.1f, new Color(255, 215, 100, 100));

                        // Bouncing golden chunks
                        for (int i = 0; i < 8; i++) {
                            float angle = random.nextFloat() * 360f;
                            float speed = 80f + random.nextFloat() * 60f;
                            Vector2f velocity = Misc.getUnitVectorAtDegreeAngle(angle);
                            velocity.scale(speed);
                            engine.addHitParticle(beam.getTo(), velocity, 8f, 1.2f, 0.1f, new Color(255, 200, 100, 220));
                        }
                        // Extra damage application on big pulse impact
                        engine.applyDamage(targetShip, beam.getTo(),
                                HIGH_EXPLOSIVE_DAMAGE * ARMOR_DAMAGE_MULTIPLIER,
                                DamageType.HIGH_EXPLOSIVE, 0f, true, true, null, true);
                    } else {
                        // Normal pulse jitter on orb arrival
                        targetShip.setJitter(this, new Color(255, 100 + random.nextInt(50), 50, 150), 0.75f, 5, 8f);
                        targetShip.setJitterUnder(this, new Color(255, 80 + random.nextInt(50), 30, 100), 0.75f, 3, 8f);
                        // Extra damage application on big pulse impact
                        engine.applyDamage(targetShip, beam.getTo(),
                                FRAGMENTATION_DAMAGE * ARMOR_DAMAGE_MULTIPLIER,
                                DamageType.FRAGMENTATION, 0f, true, true, null, true);
                    }
                }
            }

            // Animate beam width pulse
            float widthPulse = (float) Math.sin((pulseTimer / PULSE_INTERVAL) * Math.PI);
            float widthScale = 1f + widthPulse * 0.5f; // 1.0 - 1.5x width
            if (pulseCount % 4 == 0) {
                // Big pulse: widen beam to 20 (2x base width)
                // You can even make it smoothly ramp up/down around the pulse time if you want
                widthScale = 3f;
            } else {
                // Normal pulse between 1.0 and 1.5
                widthScale = 1f + widthPulse * 2f;
            }
            beam.setWidth(baseBeamWidth * widthScale);

            // Nebula particles along the beam
            for (int i = 0; i < 3; i++) {
                Vector2f pos = Misc.interpolateVector(beam.getFrom(), beam.getTo(), random.nextFloat());
                Vector2f vel = Misc.getUnitVectorAtDegreeAngle(random.nextFloat() * 360f);
                vel.scale(30f + random.nextFloat() * 20f);

                engine.addNebulaParticle(
                        pos,
                        vel,
                        10f + random.nextFloat() * 15f,
                        1.8f,
                        0.2f,
                        0.3f,
                        0.6f,
                        new Color(255, 240, 130, 200)
                );
            }

            // Calculate orb position along beam [0..1]
            float orbProgress = pulseTimer / PULSE_INTERVAL;
            Vector2f orbPos = Misc.interpolateVector(beam.getFrom(), beam.getTo(), orbProgress);

// Orb size varies by pulse type
            float orbSize = (pulseCount % 4 == 0) ? 60f : 34f;

// Add glowing orb particle (you can tweak colors/sizes)
            Vector2f orbVelocity = new Vector2f(10f, 0f); // or something like (10f, 0f)
            engine.addHitParticle(orbPos, orbVelocity, orbSize, 1.5f, 0.2f, new Color(255, 255 - random.nextInt(70), 255 - random.nextInt(100), 255 - random.nextInt(60)));}
    }

    private void spawnEmpLightning(CombatEngineAPI engine, Vector2f hitPoint, ShipAPI ship, float damageMultiplier) {
        for (int i = 0; i < 3; i++) {
            Vector2f targetPoint = new Vector2f(
                    ship.getLocation().x + random.nextFloat() * ship.getCollisionRadius() * 2 - ship.getCollisionRadius(),
                    ship.getLocation().y + random.nextFloat() * ship.getCollisionRadius() * 2 - ship.getCollisionRadius()
            );
            engine.spawnEmpArcVisual(hitPoint, null, targetPoint, ship, 10f,
                    new Color(255, 150, 0, 255), Color.WHITE);
            engine.applyDamage(ship, targetPoint,
                    STRIKE_DAMAGE * damageMultiplier,
                    DamageType.KINETIC,
                    EMP_DAMAGE * damageMultiplier,
                    true, true, null, true);
        }
    }

    private void spawnEnergySplash(CombatEngineAPI engine, Vector2f hitPoint) {
        for (int i = 0; i < 5; i++) {
            float angle = random.nextFloat() * 360f;
            float speed = 50f + random.nextFloat() * 50f;
            Vector2f velocity = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(speed);
            engine.addHitParticle(hitPoint, velocity, 5f, 1f, 0.5f, Color.YELLOW);
        }
    }

    private void spawnSmoke(CombatEngineAPI engine, Vector2f hitPoint) {
        for (int i = 0; i < 5; i++) {
            float angle = random.nextFloat() * 360f;
            float speed = 10f + random.nextFloat() * 10f;
            Vector2f velocity = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(speed);
            engine.addSmokeParticle(hitPoint, velocity, SMOKE_START_SIZE, 1f, SMOKE_DURATION, Color.GRAY);
        }
    }
}
