package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_BlitzwingJetpackEffect implements EveryFrameWeaponEffectPlugin {
    private static final int MAX_FRAMES = 10;
    private static final float FRAME_DURATION = 0.1f; // Duration of each frame in seconds
    private static final float PARTICLE_SIZE = 5f;
    private static final float PARTICLE_DURATION = 0.25f;
    private static final float MAX_PARTICLE_VELOCITY = -50f;
    private static final int PARTICLE_COUNT = 2; // Reduced particle count for each location
    private static final float PARTICLE_DELAY = 1.0f; // 1-second delay

    private int currentFrame = 0;
    private float frameTimer = 0f;
    private boolean playingForward = false;
    private float particleTimer = 0f;
    private boolean delayCompleted = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        ShipSystemAPI system = ship.getSystem();

        if (system.getState() == SystemState.IN || system.getState() == SystemState.ACTIVE) {
            playingForward = true;
            if (!delayCompleted) {
                particleTimer += amount;
                if (particleTimer >= PARTICLE_DELAY) {
                    delayCompleted = true;
                    particleTimer = 0f; // Reset the timer after the delay
                }
            } else {
                spawnParticles(engine, ship, weapon);
            }
        } else if (system.getState() == SystemState.OUT) {
            playingForward = false;
            particleTimer = 0f; // Reset the timer when the system is deactivated
            delayCompleted = false; // Reset the delay flag
        }

        frameTimer += amount;
        if (frameTimer >= FRAME_DURATION) {
            frameTimer -= FRAME_DURATION;
            if (playingForward) {
                if (currentFrame < MAX_FRAMES - 1) {
                    currentFrame++;
                }
            } else {
                if (currentFrame > 0) {
                    currentFrame--;
                }
            }
        }

        weapon.getAnimation().setFrame(currentFrame);

        // Lock the angle at 0 when the system is not active
        if (system.getState() == SystemState.IDLE || system.getState() == SystemState.COOLDOWN) {
            weapon.setCurrAngle(ship.getFacing());
        }
    }

    private void spawnParticles(CombatEngineAPI engine, ShipAPI ship, WeaponAPI weapon) {
        float facing = ship.getFacing();
        float weaponFacing = weapon.getCurrAngle();
        float shipSpeed = ship.getVelocity().length();
        float particleVelocity = MAX_PARTICLE_VELOCITY * (shipSpeed / ship.getMaxSpeed());

        // Spawn particles in the area between -17, 26 and -17, 21
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float xOffset = -17;
            float yOffset = 21 + (float) Math.random() * 5; // Random y between 21 and 26
            Vector2f spawnLocation = new Vector2f(
                    weapon.getLocation().x + xOffset * (float) Math.cos(Math.toRadians(weaponFacing)) - yOffset * (float) Math.sin(Math.toRadians(weaponFacing)),
                    weapon.getLocation().y + xOffset * (float) Math.sin(Math.toRadians(weaponFacing)) + yOffset * (float) Math.cos(Math.toRadians(weaponFacing))
            );

            Vector2f velocity = new Vector2f(
                    particleVelocity * (float) Math.cos(Math.toRadians(facing)),
                    particleVelocity * (float) Math.sin(Math.toRadians(facing))
            );

            Color particleColor = new Color(105, 255, 105, 255); // Bright greenish white

            engine.addHitParticle(spawnLocation, velocity, PARTICLE_SIZE, 1.0f, PARTICLE_DURATION, particleColor);
        }

        // Spawn particles in the area between -17, -26 and -17, -21
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float xOffset = -17;
            float yOffset = -26 + (float) Math.random() * 5; // Random y between -26 and -21
            Vector2f spawnLocation = new Vector2f(
                    weapon.getLocation().x + xOffset * (float) Math.cos(Math.toRadians(weaponFacing)) - yOffset * (float) Math.sin(Math.toRadians(weaponFacing)),
                    weapon.getLocation().y + xOffset * (float) Math.sin(Math.toRadians(weaponFacing)) + yOffset * (float) Math.cos(Math.toRadians(weaponFacing))
            );

            Vector2f velocity = new Vector2f(
                    particleVelocity * (float) Math.cos(Math.toRadians(facing)),
                    particleVelocity * (float) Math.sin(Math.toRadians(facing))
            );

            Color particleColor = new Color(105, 255, 105, 255); // Bright greenish white

            engine.addHitParticle(spawnLocation, velocity, PARTICLE_SIZE, 1.0f, PARTICLE_DURATION, particleColor);
        }
    }
}