package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;

public class AEG_BlitzwingJetpackEffect implements EveryFrameWeaponEffectPlugin {
    private static final int MAX_FRAMES = 10;
    private static final float FRAME_DURATION = 0.1f; // Duration of each frame in seconds

    private int currentFrame = 0;
    private float frameTimer = 0f;
    private boolean playingForward = false;

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
}