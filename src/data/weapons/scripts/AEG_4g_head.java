package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import java.util.Random;

public class AEG_4g_head implements EveryFrameWeaponEffectPlugin {

    private static final int NUM_FRAMES = 11;  // Total number of frames
    private static final int LOOP_START_FRAME = 5;  // Frame to start looping from
    private float elapsed = 0;  // Elapsed time tracker
    private int currentFrame = 0;  // Current frame index
    private boolean initialCycleComplete = false;  // Flag to check if initial cycle is complete
    private static final float BASE_FRAME_DURATION = 4.0f / NUM_FRAMES;  // Base duration for each frame (speed cut in half again)
    private static final float MAX_FRAME_DURATION = 0.75f / (NUM_FRAMES - LOOP_START_FRAME);  // Max speed for looped animation
    private Random random = new Random();
    private float chargeUpTime = 0;  // Timer for charge-up duration
    private static final float CHARGE_UP_DURATION = 4.0f;  // Charge-up duration in seconds
    private static final float PARTICLE_DELAY = 2.0f;  // Delay before particles start spawning
    private float combatStartTime = 0;  // Timer for combat start

    private static final Vector2f[] PARTICLE_OFFSETS = {
            new Vector2f(-19, 7),
            new Vector2f(-19, -7),
            new Vector2f(-21, 17),
            new Vector2f(-21, -17),
            new Vector2f(-40, 17),
            new Vector2f(-40, -17)
    };

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null || engine.isPaused()) return;

        elapsed += amount;

        // Track charge-up time
        if (ship.getSystem().isActive()) {
            chargeUpTime += amount;
        } else {
            chargeUpTime = 0;
        }

        // Change weapon color based on charge-up time
        Color particleColor;
        if (chargeUpTime >= CHARGE_UP_DURATION) {
            weapon.getSprite().setColor(Color.GREEN);  // Change weapon color to green
            particleColor = Color.GREEN;
        } else {
            weapon.getSprite().setColor(Color.WHITE);  // Reset weapon color to default (white)
            particleColor = new Color(255, 140, 0);  // Initial color (orange)
        }

        // Adjust frame duration based on ship's speed
        float speedFactor = Math.min(ship.getVelocity().length() / ship.getMaxSpeed(), 1.0f);
        float frameDuration = BASE_FRAME_DURATION * (1.0f - speedFactor) + MAX_FRAME_DURATION * speedFactor;

        // Cycle through frames
        if (elapsed > frameDuration) {
            if (!initialCycleComplete) {
                currentFrame = (currentFrame + 1) % NUM_FRAMES;
                if (currentFrame == 0) {
                    initialCycleComplete = true;
                    currentFrame = LOOP_START_FRAME;  // Jump directly to frame 5
                }
            } else {
                currentFrame = LOOP_START_FRAME + (currentFrame - LOOP_START_FRAME + 1) % (NUM_FRAMES - LOOP_START_FRAME);
            }
            weapon.getAnimation().setFrame(currentFrame);
            elapsed -= frameDuration;
        }

        // Track combat start time
        if (combatStartTime < PARTICLE_DELAY) {
            combatStartTime += amount;
            return;  // Skip particle spawning until delay is over
        }

        // Spawn particles flowing backward relative to weapon's facing
        for (Vector2f offset : PARTICLE_OFFSETS) {
            Vector2f spawnLocation = new Vector2f(weapon.getLocation());
            Vector2f.add(spawnLocation, Misc.rotateAroundOrigin(new Vector2f(offset), weapon.getCurrAngle()), spawnLocation);

            float angleOffset = random.nextFloat() * 30f - 15f;  // Random angle offset within a range
            float angle = weapon.getCurrAngle() + 180f + angleOffset;  // Reverse direction with random offset
            float speed = 50f + random.nextFloat() * 50f;  // Adjust speed range as needed
            float size = 5f + random.nextFloat() * 5f;  // Adjust size range as needed
            float duration = 1f + random.nextFloat() * 1f;  // Adjust duration range as needed

            engine.addHitParticle(
                    spawnLocation,
                    (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(speed),
                    size,
                    1f,  // Initial brightness
                    duration,
                    particleColor  // Color based on charge-up time
            );
        }
    }
}