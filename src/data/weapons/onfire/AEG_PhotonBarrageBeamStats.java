package data.weapons.onfire;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

public class AEG_PhotonBarrageBeamStats implements BeamEffectPlugin {

    private static final float INITIAL_DAMAGE = 2000f;
    private static final float SPLIT_BEAM_DAMAGE = 500f;
    private static final float RANGE = 1000f;
    private static final float BEAM_THICKNESS = 40f; // Updated standard width
    private static final Color BEAM_COLOR = new Color(255, 255, 0, 255); // Bright yellow color
    private static final Color PARTICLE_COLOR = new Color(220, 180, 100, 255); // Flame color
    private static final int PARTICLE_COUNT = 50;
    private static final float PARTICLE_SIZE = 10f;
    private static final float PARTICLE_LIFETIME = 1.0f;
    private static final float PARTICLE_SPEED = 100f;
    private static final float CHARGE_UP_TIME = 3.0f; // Charge-up time in seconds

    private static final Color PLASMA_COLOR = new Color(255, 185, 0, 255); // Yellowish-orange color
    private static final int PLASMA_COUNT = 20;
    private static final float PLASMA_WIDTH = 5f;
    private static final float PLASMA_LENGTH = 20f;
    private static final float PLASMA_LIFETIME = 0.5f;
    private static final float PLASMA_SPEED = 200f;
    private static final float CONE_ANGLE = 45f; // Wide cone angle in degrees

    private static final float SPLIT_BEAM_SPREAD_RADIUS = 50f; // Spread radius for split beams

    private final IntervalUtil fireInterval = new IntervalUtil(0.1f, 0.1f);
    private float chargeUpProgress = 0f;
    private final Random random = new Random();

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        chargeUpProgress += amount;

        if (chargeUpProgress < CHARGE_UP_TIME) {
            // Calculate the size of the central ball based on charge-up progress
            float centralBallSize = PARTICLE_SIZE * (chargeUpProgress / CHARGE_UP_TIME) * 5;

            // Create the central ball
            engine.addHitParticle(
                    beam.getWeapon().getLocation(),
                    new Vector2f(0, 0), // No velocity for the central ball
                    centralBallSize,
                    1.0f, // Brightness
                    PARTICLE_LIFETIME,
                    new Color(255, 255, 255, 255) // White core
            );

            // Create smaller balls moving towards the central ball
            for (int i = 0; i < PARTICLE_COUNT; i++) {
                float angle = random.nextFloat() * 360f; // Random angle in degrees
                float distance = random.nextFloat() * 300f; // Reduced radius to 300
                Vector2f startPosition = new Vector2f(
                        (float) Math.cos(Math.toRadians(angle)) * distance,
                        (float) Math.sin(Math.toRadians(angle)) * distance
                );
                Vector2f.add(startPosition, beam.getWeapon().getLocation(), startPosition);

                // Calculate velocity towards the central ball
                Vector2f velocity = new Vector2f(beam.getWeapon().getLocation());
                Vector2f.sub(velocity, startPosition, velocity);

                // Check if the velocity vector is zero
                if (velocity.lengthSquared() > 0) {
                    velocity.normalise();
                    velocity.scale(PARTICLE_SPEED);
                } else {
                    // Handle the zero vector case, e.g., set a default direction or skip this iteration
                    velocity.set(1, 0); // Example: setting a default direction
                    velocity.scale(PARTICLE_SPEED);
                }

                // Stagger the particles so they don't reach the center at the same time
                float staggerFactor = random.nextFloat() * 0.5f + 0.5f; // Between 0.5 and 1.0
                velocity.scale(staggerFactor);

                // Calculate transparency based on distance to the center
                float transparency = 1.0f - (distance / 300f);

                engine.addHitParticle(
                        startPosition,
                        velocity,
                        PARTICLE_SIZE,
                        transparency, // Adjust transparency
                        PARTICLE_LIFETIME,
                        PARTICLE_COLOR
                );
            }
        } else {
            fireInterval.advance(amount);
            if (fireInterval.intervalElapsed()) {
                // Fire the initial beam
                if (beam.getWeapon().getShip().getShipTarget() != null) {
                    engine.spawnEmpArcPierceShields(
                            beam.getWeapon().getShip(), beam.getWeapon().getLocation(), beam.getWeapon().getShip(), beam.getWeapon().getShip().getShipTarget(),
                            DamageType.ENERGY, // Damage type
                            INITIAL_DAMAGE * fireInterval.getIntervalDuration(), // Damage amount
                            0f, // EMP damage
                            RANGE, // Max range
                            "tachyon_lance_emp_impact", // Impact sound
                            BEAM_THICKNESS, // Thickness
                            BEAM_COLOR, // Fringe color
                            new Color(255, 255, 255, 255) // White core
                    );
                }

                // Create particle effects for the initial beam
                for (int i = 0; i < PARTICLE_COUNT; i++) {
                    Vector2f particleLocation = new Vector2f(beam.getWeapon().getLocation());
                    Vector2f particleVelocity = new Vector2f((float) Math.random() * PARTICLE_SPEED - PARTICLE_SPEED / 2, (float) Math.random() * PARTICLE_SPEED - PARTICLE_SPEED / 2);
                    engine.addHitParticle(
                            particleLocation,
                            particleVelocity,
                            PARTICLE_SIZE,
                            1.0f, // Brightness
                            PARTICLE_LIFETIME,
                            new Color(255, 255, 255, 255) // White core
                    );
                }

                // Create plasma projectiles in a wide cone
                for (int i = 0; i < PLASMA_COUNT; i++) {
                    float angle = (random.nextFloat() * CONE_ANGLE) - (CONE_ANGLE / 2); // Random angle within the cone
                    Vector2f plasmaDirection = new Vector2f((float) Math.cos(Math.toRadians(angle)), (float) Math.sin(Math.toRadians(angle)));
                    plasmaDirection.scale(PLASMA_SPEED);

                    Vector2f plasmaLocation = new Vector2f(beam.getWeapon().getLocation());
                    engine.addHitParticle(
                            plasmaLocation,
                            plasmaDirection,
                            PLASMA_WIDTH,
                            1.0f, // Brightness
                            PLASMA_LIFETIME,
                            new Color(255, 255, 255, 255) // White core
                    );

                    // Create the beam-like spike effect
                    Vector2f plasmaEndLocation = new Vector2f(plasmaLocation);
                    Vector2f.add(plasmaEndLocation, (Vector2f) plasmaDirection.scale(PLASMA_LENGTH / PLASMA_SPEED), plasmaEndLocation);
                    engine.addHitParticle(
                            plasmaEndLocation,
                            new Vector2f(0, 0), // No velocity for the end point
                            PLASMA_WIDTH,
                            1.0f, // Brightness
                            PLASMA_LIFETIME,
                            new Color(255, 255, 255, 255) // White core
                    );
                }
                // Split into smaller beams with random angles
                Vector2f beamStart = beam.getWeapon().getLocation();
                Vector2f beamEnd = new Vector2f(beamStart);
                Vector2f beamDirection = new Vector2f((float) Math.cos(Math.toRadians(beam.getWeapon().getCurrAngle())), (float) Math.sin(Math.toRadians(beam.getWeapon().getCurrAngle())));
                beamDirection.scale(RANGE);
                Vector2f.add(beamEnd, beamDirection, beamEnd);

                for (int i = 0; i < 5; i++) {
                    // Random position along the initial beam
                    float t = random.nextFloat(); // Random value between 0 and 1
                    Vector2f splitBeamStart = new Vector2f(
                            beamStart.x + t * (beamEnd.x - beamStart.x),
                            beamStart.y + t * (beamEnd.y - beamStart.y)
                    );

                    // Random offset within a 50-unit radius
                    float angle = random.nextFloat() * 360f; // Random angle in degrees
                    float distance = random.nextFloat() * SPLIT_BEAM_SPREAD_RADIUS; // Random distance within 50 units
                    Vector2f offset = new Vector2f(
                            (float) Math.cos(Math.toRadians(angle)) * distance,
                            (float) Math.sin(Math.toRadians(angle)) * distance
                    );
                    Vector2f.add(splitBeamStart, offset, splitBeamStart);

                    // Calculate the direction and end position of the split beam
                    Vector2f splitBeamDirection = new Vector2f((float) Math.cos(Math.toRadians(angle)), (float) Math.sin(Math.toRadians(angle)));
                    splitBeamDirection.scale(RANGE);

                    Vector2f splitBeamEnd = new Vector2f(splitBeamStart);
                    Vector2f.add(splitBeamEnd, splitBeamDirection, splitBeamEnd);

                    if (beam.getWeapon().getShip().getShipTarget() != null) {
                        engine.spawnEmpArcPierceShields(
                                beam.getWeapon().getShip(), splitBeamStart, beam.getWeapon().getShip(), beam.getWeapon().getShip().getShipTarget(),
                                DamageType.ENERGY, // Damage type
                                SPLIT_BEAM_DAMAGE, // Damage amount
                                0f, // EMP damage
                                RANGE, // Max range
                                "tachyon_lance_emp_impact", // Impact sound
                                BEAM_THICKNESS / 2, // Thickness
                                BEAM_COLOR, // Fringe color
                                new Color(255, 255, 255, 255) // White core
                        );
                    }
                }
            }
        }
    }
}
