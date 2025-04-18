package data.weapons.onfire;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

public class
AEG_PhotonBarrageBeamStats implements BeamEffectPlugin {
    private static final float EMP_STRIKE_WIDTH = 5f;
    private static final float EMP_STRIKE_LENGTH = 75f;
    private boolean hasBurstFired = false;
    private float barrageTimer = 0f;
    private float barrageInterval = 0.3f;
    private float barrageIntensity = 1f;
    private final IntervalUtil beamLightningInterval = new IntervalUtil(1f, 2f); // Controls lightning arc timing
    private static final Color BEAM_COLOR = new Color(255, 255, 0, 255); // Bright yellow color
    private static final Color PARTICLE_COLOR = new Color(220, 180, 100, 255); // Flame color
    private static final float PARTICLE_SIZE = 10f;
    private static final float PARTICLE_LIFETIME = 1.0f;
    private static final float PARTICLE_SPEED = 100f;
    private static final float CHARGE_UP_TIME = 3.0f; // Charge-up time in seconds
    private static final Color PLASMA_COLOR = new Color(255, 185, 0, 255); // Yellowish-orange color
    private final IntervalUtil eyeLightningInterval = new IntervalUtil(0.5f, 1.2f);
    private final IntervalUtil fireInterval = new IntervalUtil(0.1f, 0.1f);
    private float chargeUpProgress = 0f;
    private static final Random random = new Random();

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        chargeUpProgress += amount;

        if (chargeUpProgress < CHARGE_UP_TIME) {
            // CHARGING PHASE: glowing central orb and inward particles
            float centralBallSize = PARTICLE_SIZE * (chargeUpProgress / CHARGE_UP_TIME) * 5;
            engine.addHitParticle(
                    beam.getWeapon().getLocation(),
                    new Vector2f(0, 0),
                    centralBallSize,
                    1.0f,
                    PARTICLE_LIFETIME,
                    new Color(255, 200 - random.nextInt(30), 150 - random.nextInt(40), 255)
            );

            int particleCount = 20 + random.nextInt(30); // Between 20–40
            for (int i = 0; i < particleCount; i++) {
                float angle = random.nextFloat() * 360f;
                float maxRadius = 300f * (1f - (chargeUpProgress / CHARGE_UP_TIME));
                float distance = random.nextFloat() * maxRadius;
                Vector2f startPosition = new Vector2f(
                        (float) Math.cos(Math.toRadians(angle)) * distance,
                        (float) Math.sin(Math.toRadians(angle)) * distance
                );
                Vector2f.add(startPosition, beam.getWeapon().getLocation(), startPosition);

                Vector2f velocity = new Vector2f(beam.getWeapon().getLocation());
                Vector2f.sub(velocity, startPosition, velocity);
                if (velocity.lengthSquared() > 0) {
                    velocity.normalise();
                    velocity.scale(PARTICLE_SPEED);
                } else {
                    velocity.set(1, 0);
                    velocity.scale(PARTICLE_SPEED);
                }

                velocity.scale(random.nextFloat() * 0.5f + 0.5f);
                float transparency = 1.0f - (distance / 300f);

                engine.addHitParticle(
                        startPosition,
                        velocity,
                        PARTICLE_SIZE,
                        transparency,
                        PARTICLE_LIFETIME,
                        PARTICLE_COLOR
                );
            }
        } else {
            // Burst fire after charge-up (only once)
            if (!hasBurstFired) {
                hasBurstFired = true;

                Vector2f weaponLoc = beam.getWeapon().getLocation();
                float baseAngle = beam.getWeapon().getCurrAngle();
                int beamCount = 6;
                float spread = 30f;

                for (int i = 0; i < beamCount; i++) {
                    float angle = baseAngle - spread / 2f + (spread / (beamCount - 1)) * i;
                    Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);
                    dir.scale(80f + random.nextFloat() * 300f); // Beam length
                    Vector2f end = Vector2f.add(weaponLoc, dir, null);

                    // Nebula trail
                    engine.addNebulaParticle(
                            weaponLoc,
                            Misc.ZERO,
                            80f,
                            1.8f,
                            0.2f,
                            0.3f,
                            0.6f,
                            new Color(255, 200 - random.nextInt(30), 100 - random.nextInt(40), 180)
                    );
                }
            }
        }

        if (hasBurstFired) {
            eyeLightningInterval.advance(amount);
            if (eyeLightningInterval.intervalElapsed()) {
                Vector2f weaponLoc = beam.getWeapon().getLocation();
                float baseAngle = beam.getWeapon().getCurrAngle();

                Vector2f leftEyeOffset = new Vector2f(9.5f, 7f);
                Vector2f rightEyeOffset = new Vector2f(9.5f, -7f);
                float angleRad = (float) Math.toRadians(baseAngle);

                Vector2f leftEyeWorld = Vector2f.add(weaponLoc, rotateVector(leftEyeOffset, angleRad), null);
                Vector2f rightEyeWorld = Vector2f.add(weaponLoc, rotateVector(rightEyeOffset, angleRad), null);

                int leftEyeArcs = 4;
                int rightEyeArcs = 4;

                for (int i = 0; i < leftEyeArcs; i++) {
                    spawnEmpArc(engine, leftEyeWorld, -1);
                }
                for (int i = 0; i < rightEyeArcs; i++) {
                    spawnEmpArc(engine, rightEyeWorld, 1);
                }
            }
        }
        // 🔥 FIRING PHASE: EMP arcs crawling up the beam
        beamLightningInterval.advance(amount);
        if (beamLightningInterval.intervalElapsed()) {
            Vector2f from = beam.getFrom();
            Vector2f to = beam.getTo();
            float empDistance = Misc.getDistance(from, to);

            for (float i = 0; i < empDistance; i += 10f + random.nextFloat() * 5f) {
                Vector2f point = new Vector2f(
                        from.x + (to.x - from.x) * (i / empDistance),
                        from.y + (to.y - from.y) * (i / empDistance)
                );

                Vector2f flicker = new Vector2f(
                        point.x + (random.nextFloat() * 10f - 5f),
                        point.y + (random.nextFloat() * 10f - 5f)
                );

                engine.spawnEmpArcVisual(
                        point,
                        null,
                        flicker,
                        null,
                        5f,
                        new Color(255, 200 - random.nextInt(30), 100 - random.nextInt(40), 255), // Core
                        new Color(255, 150 - random.nextInt(30), 50 - random.nextInt(40), 255)  // Fringe
                );
            }
        }

        // Barrage effect
        if (hasBurstFired) {
            barrageTimer += amount;
            if (barrageTimer >= barrageInterval) {
                barrageTimer -= barrageInterval;
                barrageInterval *= 0.95f;
                barrageInterval = Math.max(barrageInterval, 0.05f);
                barrageIntensity += 0.05f;

                Vector2f weaponLoc = beam.getWeapon().getLocation();
                float baseAngle = beam.getWeapon().getCurrAngle();
                float angle = baseAngle + (random.nextFloat() - 0.5f) * 30f * barrageIntensity;

                Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);
                dir.scale(300f + random.nextFloat() * 200f); // variable streak length
                Vector2f velocity = new Vector2f(dir);
                Vector2f particleStart = new Vector2f(weaponLoc);
                Vector2f.add(particleStart, new Vector2f(
                        (random.nextFloat() - 0.5f) * 30f,
                        (random.nextFloat() - 0.5f) * 30f), particleStart); // little spray offset

                // Main bright "plasma orb"
                engine.addHitParticle(
                        particleStart,
                        velocity,
                        20f + random.nextFloat() * 10f,
                        2 - random.nextInt(1),
                        0.2f + random.nextFloat() * 0.2f,
                        PLASMA_COLOR
                );

                // Trail effect using nebula
                engine.addNebulaParticle(
                        particleStart,
                        velocity,
                        30f + random.nextFloat() * 20f,
                        2f,
                        0.3f,
                        0.5f,
                        0.7f,
                        new Color(255, 120 + random.nextInt(80), 50 + random.nextInt(50), 180)
                );

                // Faint glow for flare
                engine.addSmoothParticle(
                        particleStart,
                        velocity,
                        40f,
                        0.8f,
                        0.1f,
                        new Color(255, 180, 100, 200)
                );
            }
        }
    }
    // Function to spawn the EMP arc at the given position (visual only)
    private void spawnEmpArc(CombatEngineAPI engine, Vector2f eyePosition, int direction) {
        float angle = random.nextFloat() * 180f; // total spread range
        float angleRad = (float) Math.toRadians(angle) * direction; // mirror for left/right
        float length = EMP_STRIKE_LENGTH * (0.7f + random.nextFloat() * 0.6f); // 70%–130% length
        float dx = (float) Math.cos(angleRad) * length;
        float dy = (float) Math.sin(angleRad) * length;
        Vector2f end = new Vector2f(eyePosition.x + dx, eyePosition.y + dy);

        Color EMP_CORE_COLOR = new Color(255, 225 - random.nextInt(30), 150 - random.nextInt(40), 255);
        Color EMP_FRINGE_COLOR = new Color(255, 150 - random.nextInt(30), 50 - random.nextInt(40), 255);

        engine.spawnEmpArcVisual(
                eyePosition,
                null,
                end,
                null,
                EMP_STRIKE_WIDTH,
                EMP_CORE_COLOR,
                EMP_FRINGE_COLOR
        );
    }
    private Vector2f rotateVector(Vector2f vec, float angleRad) {
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);
        return new Vector2f(
                vec.x * cos - vec.y * sin,
                vec.x * sin + vec.y * cos
        );
    }

}
