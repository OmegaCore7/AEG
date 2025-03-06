package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class AEG_right_kneedrill implements EveryFrameWeaponEffectPlugin {

    private static final float PUSH_FORCE = 25f; // Forward push force
    private static final float SPEED_BUFF = 1.2f; // 20% speed buff
    private static final float MANEUVER_BUFF = 1.5f; // 50% maneuverability buff
    private static final String BUFF_ID = "AEG_kneedrill_buff";

    private static final int FRAME_INVISIBLE = 0; // Invisible frame
    private static final int FRAME_MAX = 6; // Max visible frame (fully deployed)
    private static final String WEAPON_ID = "AEG_4g_rightkneedrill"; // Weapon ID to check

    private int currentFrame = FRAME_INVISIBLE; // Start in an invisible state
    private boolean coolingDown = false; // Tracks cooldown state
    private boolean wasFiring = false; // Tracks if weapon was firing to allow full animation cycle

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon == null || engine == null) return; // Safety check

        ShipAPI playerShip = weapon.getShip();
        if (playerShip == null) return;

        boolean isFiring = weapon.isFiring();

        if (isFiring) {
            wasFiring = true; // Weapon was fired, ensure animation plays fully
            if (currentFrame < FRAME_MAX) {
                currentFrame++;
            }
            coolingDown = false;
            applyEffects(playerShip);
            forceEngineBoost(playerShip, true);
        }
        // Allow animation to continue if weapon was fired, even if deselected
        else if (wasFiring) {
            if (currentFrame > FRAME_INVISIBLE) {
                currentFrame--;
                coolingDown = true;
            } else {
                // Once fully retracted, reset wasFiring
                wasFiring = false;
                coolingDown = false;
            }
            forceEngineBoost(playerShip, false);
        }

        // Apply the current animation frame
        weapon.getAnimation().setFrame(currentFrame);
    }

    private void applyEffects(ShipAPI playerShip) {
        // Buff stats while weapon is active
        playerShip.getMutableStats().getMaxSpeed().modifyMult(BUFF_ID, SPEED_BUFF);
        playerShip.getMutableStats().getTurnAcceleration().modifyMult(BUFF_ID, MANEUVER_BUFF);
        playerShip.getMutableStats().getMaxTurnRate().modifyMult(BUFF_ID, MANEUVER_BUFF);

        // Apply forward push relative to ship's facing direction
        applyForwardPush(playerShip);
    }

    private void applyForwardPush(ShipAPI playerShip) {
        float angle = (float) Math.toRadians(playerShip.getFacing()); // Convert to radians
        float pushX = (float) Math.cos(angle) * PUSH_FORCE; // X component
        float pushY = (float) Math.sin(angle) * PUSH_FORCE; // Y component

        Vector2f shipVelocity = playerShip.getVelocity();
        shipVelocity.x += pushX;
        shipVelocity.y += pushY;
    }

    private void forceEngineBoost(ShipAPI playerShip, boolean activate) {
        if (playerShip.getEngineController() != null) {
            if (activate) {
                playerShip.getEngineController().extendFlame(this, 0.5f, 2.0f, 2.0f);
            } else {
                playerShip.getEngineController().extendFlame(this, 0.0f, 1.0f, 1.0f);
            }
        }
    }
}
