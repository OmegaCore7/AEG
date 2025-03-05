package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipAPI;
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

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon == null || engine == null) return; // Safety check

        ShipAPI playerShip = engine.getPlayerShip();
        if (playerShip == null || playerShip.getWeaponGroupFor(weapon) == null) return;

        boolean correctWeaponSelected = isWeaponGroupSelected(playerShip, WEAPON_ID);
        boolean isFiring = weapon.isFiring();

        if (correctWeaponSelected && isFiring) {
            // Charging up: Move from 1 → 6
            if (currentFrame < FRAME_MAX) {
                currentFrame++;
            }
            // Stay at frame 6 while firing
            coolingDown = false;
            applyEffects(playerShip);
        } else if (currentFrame > FRAME_INVISIBLE) {
            // Cooling down: Move from 6 → 0
            coolingDown = true;
            currentFrame--;
        } else {
            // Fully retracted, stay invisible
            coolingDown = false;
        }

        // Apply the current animation frame
        weapon.getAnimation().setFrame(currentFrame);
    }

    private boolean isWeaponGroupSelected(ShipAPI playerShip, String weaponId) {
        if (playerShip.getSelectedGroupAPI() == null) return false;
        for (WeaponAPI w : playerShip.getSelectedGroupAPI().getWeaponsCopy()) {
            if (weaponId.equals(w.getId())) return true;
        }
        return false;
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
}
