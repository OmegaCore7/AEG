package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;

public class AEG_4g_head implements EveryFrameWeaponEffectPlugin {

    private static final int NUM_FRAMES = 9;  // Total number of frames
    private float elapsed = 0;  // Elapsed time tracker
    private int currentFrame = 0;  // Current frame index

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null || engine.isPaused()) return;

        elapsed += amount;

        // Cycle through frames every second
        if (elapsed > 1.0f / NUM_FRAMES) {
            currentFrame = (currentFrame + 1) % NUM_FRAMES;
            weapon.getAnimation().setFrame(currentFrame);
            elapsed -= 1.0f / NUM_FRAMES;
        }
    }
}