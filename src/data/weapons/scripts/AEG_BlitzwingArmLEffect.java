package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;

public class AEG_BlitzwingArmLEffect implements EveryFrameWeaponEffectPlugin {
    private static final int MAX_FRAMES = 15;
    private static final float FRAME_DURATION = 0.066667f; // Duration of each frame in seconds (1 second / 15 frames)

    private int currentFrame = 0;
    private float frameTimer = 0f;
    private boolean playingForward = false;
    private boolean firing = false;
    private float fireRateMultiplier = 1.0f;
    private float recoilReduction = 0.0f;
    private static final float MAX_FIRE_RATE_MULTIPLIER = 2.5f;
    private static final float MAX_RECOIL_REDUCTION = 1.0f;
    private static final float FIRE_RATE_INCREMENT = 0.05f;
    private static final float RECOIL_REDUCTION_INCREMENT = 0.05f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        ShipSystemAPI system = ship.getSystem();

        if (system.getState() == SystemState.IN) {
            playingForward = true;
        } else if (system.getState() == SystemState.OUT) {
            playingForward = false;
        }

        frameTimer += amount;
        if (frameTimer >= FRAME_DURATION) {
            frameTimer -= FRAME_DURATION;
            if (playingForward) {
                if (currentFrame < 9) {
                    currentFrame++;
                } else {
                    currentFrame = 9; // Hold at Blitzwing_armL09
                }
            } else {
                if (currentFrame > 0) {
                    currentFrame--;
                }
            }
        }

        if (weapon.isFiring()) {
            firing = true;
            if (currentFrame >= 9 && currentFrame < 14) {
                currentFrame++;
            } else {
                currentFrame = 9;
            }

            // Increase fire rate and decrease recoil
            if (fireRateMultiplier < MAX_FIRE_RATE_MULTIPLIER) {
                fireRateMultiplier += FIRE_RATE_INCREMENT;
            }
            if (recoilReduction < MAX_RECOIL_REDUCTION) {
                recoilReduction += RECOIL_REDUCTION_INCREMENT;
            }
        } else {
            firing = false;
        }

        // Maintain values while the system is active
        if (system.getState() == SystemState.IDLE || system.getState() == SystemState.COOLDOWN) {
            fireRateMultiplier = 1.0f;
            recoilReduction = 0.0f;
        }

        weapon.getAnimation().setFrame(currentFrame);

        // Apply fire rate and recoil reduction
        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getBallisticRoFMult().modifyMult("BlitzwingArmLEffect", fireRateMultiplier);
        stats.getRecoilPerShotMult().modifyMult("BlitzwingArmLEffect", 1.0f - recoilReduction);

        // Lock the angle at 0 when the system is not active
        if (system.getState() == SystemState.IDLE || system.getState() == SystemState.COOLDOWN) {
            weapon.setCurrAngle(ship.getFacing());
        }
    }
}