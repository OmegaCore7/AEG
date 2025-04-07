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

    private static final int NUM_FRAMES = 11;
    private static final int LOOP_START_FRAME = 5;
    private float elapsed = 0;
    private int currentFrame = 0;
    private boolean initialCycleComplete = false;
    private boolean overloadCycleComplete = false;
    private boolean particlesActive = true;
    private float particleRestartTimer = 0;
    private static final float BASE_FRAME_DURATION = 4.0f / NUM_FRAMES;
    private static final float MAX_FRAME_DURATION = 0.75f / (NUM_FRAMES - LOOP_START_FRAME);
    private Random random = new Random();
    private float chargeUpTime = 0;
    private static final float CHARGE_UP_DURATION = 4.0f;
    private static final float PARTICLE_DELAY = 2.0f;
    private float combatStartTime = 0;

    private static final Vector2f[] PARTICLE_OFFSETS = {
            new Vector2f(-23, 7),
            new Vector2f(-23, -7),
            new Vector2f(-25, 28),
            new Vector2f(-25, -28),
            new Vector2f(-40, 17),
            new Vector2f(-40, -17),
            new Vector2f(-55, 0),
            new Vector2f(-55, -45),
            new Vector2f(-55, 45),
            new Vector2f(-80, 0),
            new Vector2f(-70, -18),
            new Vector2f(-70, 18)
    };

    private static final int MIN_PARTICLES = 2;
    private static final int MAX_PARTICLES = 5;
    private static final float SPAWN_RADIUS = 5.0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null || engine.isPaused()) return;

        elapsed += amount;

        if (ship.getSystem().isActive()) {
            chargeUpTime += amount;
        } else {
            chargeUpTime = 0;
        }

        Color particleColor;
        if (chargeUpTime >= CHARGE_UP_DURATION) {
            weapon.getSprite().setColor(new Color(0, 255, 100, 255));
            particleColor = new Color(0, 255, 100, 255);
        } else {
            weapon.getSprite().setColor(Color.WHITE);
            particleColor = new Color(255, 140, 0);
        }

        float speedFactor = Math.min(ship.getVelocity().length() / ship.getMaxSpeed(), 1.0f);
        float frameDuration = BASE_FRAME_DURATION * (1.0f - speedFactor) + MAX_FRAME_DURATION * speedFactor;

        if (elapsed > frameDuration) {
            if (ship.getFluxTracker().isOverloaded() || ship.getFluxTracker().isVenting()) {
                if (!overloadCycleComplete) {
                    currentFrame = (currentFrame - 1 + NUM_FRAMES) % NUM_FRAMES;
                    if (currentFrame == 0) {
                        overloadCycleComplete = true;
                        currentFrame = 0;  // Hold on frame 1
                        particlesActive = false;  // Stop particles during overload and venting
                    }
                }
            } else {
                if (overloadCycleComplete) {
                    currentFrame = (currentFrame + 1) % NUM_FRAMES;
                    if (currentFrame == NUM_FRAMES - 1) {
                        overloadCycleComplete = false;
                        initialCycleComplete = false;
                        particleRestartTimer = 0;  // Reset particle restart timer
                    }
                } else {
                    if (!initialCycleComplete) {
                        currentFrame = (currentFrame + 1) % NUM_FRAMES;
                        if (currentFrame == 0) {
                            initialCycleComplete = true;
                            currentFrame = LOOP_START_FRAME;
                        }
                    } else {
                        currentFrame = LOOP_START_FRAME + (currentFrame - LOOP_START_FRAME + 1) % (NUM_FRAMES - LOOP_START_FRAME);
                    }
                }
            }
            weapon.getAnimation().setFrame(currentFrame);
            elapsed -= frameDuration;
        }

        if (combatStartTime < PARTICLE_DELAY) {
            combatStartTime += amount;
            return;
        }

        if (!particlesActive) {
            particleRestartTimer += amount;
            if (particleRestartTimer >= 2.0f) {
                particlesActive = true;  // Restart particles after 2 seconds
            }
            return;
        }

        int numParticles = random.nextInt(MAX_PARTICLES - MIN_PARTICLES + 1) + MIN_PARTICLES;

        for (int i = 0; i < numParticles; i++) {
            Vector2f offset = PARTICLE_OFFSETS[random.nextInt(PARTICLE_OFFSETS.length)];
            Vector2f spawnLocation = new Vector2f(weapon.getLocation());
            Vector2f.add(spawnLocation, Misc.rotateAroundOrigin(new Vector2f(offset), weapon.getCurrAngle()), spawnLocation);

            float angleOffset = random.nextFloat() * 30f - 15f;
            float angle = weapon.getCurrAngle() + 180f + angleOffset;
            float speed = 50f + random.nextFloat() * 50f;
            float size = 5f + random.nextFloat() * 5f;
            float duration = 1f + random.nextFloat() * 1f;

            Vector2f randomOffset = new Vector2f(random.nextFloat() * SPAWN_RADIUS * 2 - SPAWN_RADIUS, random.nextFloat() * SPAWN_RADIUS * 2 - SPAWN_RADIUS);
            Vector2f.add(spawnLocation, randomOffset, spawnLocation);

            engine.addHitParticle(
                    spawnLocation,
                    (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(speed),
                    size,
                    1f,
                    duration,
                    particleColor
            );
        }
    }
}