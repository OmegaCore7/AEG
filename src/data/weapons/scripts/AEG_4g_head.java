package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import java.awt.Color;

public class AEG_4g_head implements EveryFrameWeaponEffectPlugin {

    private static final int NUM_FRAMES = 11;  // Total number of frames
    private static final int LOOP_START_FRAME = 5;  // Frame to start looping from
    private float elapsed = 0;  // Elapsed time tracker
    private int currentFrame = 0;  // Current frame index
    private boolean initialCycleComplete = false;  // Flag to check if initial cycle is complete
    private static final float FRAME_DURATION = 4.0f / NUM_FRAMES;  // Duration for each frame (speed cut in half again)

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null || engine.isPaused()) return;

        elapsed += amount;

        // Check if the ship system is active
        if (ship.getSystem().isActive()) {
            weapon.getSprite().setColor(Color.GREEN);  // Change weapon color to green
        } else {
            weapon.getSprite().setColor(Color.WHITE);  // Reset weapon color to default (white)
        }

        // Cycle through frames every second
        if (elapsed > FRAME_DURATION) {
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
            elapsed -= FRAME_DURATION;
        }
    }
}