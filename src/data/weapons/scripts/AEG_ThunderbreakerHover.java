package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;

public class AEG_ThunderbreakerHover implements EveryFrameWeaponEffectPlugin {

    private float time = 0f;
    private static final float SWAY_AMPLITUDE = 10f; // Adjust as needed for left-right sway
    private static final float SWAY_FREQUENCY = 2f;  // Adjust as needed for left-right sway
    private static final float HOVER_AMPLITUDE = 1f; // Adjust as needed for up-down hover
    private static final float HOVER_FREQUENCY = 0.5f; // Adjust as needed for up-down hover
    private Vector2f originalLocation = null;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon.getShip() == null) return;

        if (originalLocation == null) {
            originalLocation = new Vector2f(weapon.getSlot().getLocation());
        }

        time += amount;

        // Calculate sway offset based on ship's facing direction
        float swayOffset = (float) Math.sin(time * SWAY_FREQUENCY) * SWAY_AMPLITUDE;
        float hoverOffset = (float) Math.sin(time * HOVER_FREQUENCY) * HOVER_AMPLITUDE;

        // Calculate the new position
        float angle = weapon.getShip().getFacing();
        float rad = (float) Math.toRadians(angle);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float newX = originalLocation.x + swayOffset * cos - hoverOffset * sin;
        float newY = originalLocation.y + swayOffset * sin + hoverOffset * cos;

        Vector2f newLocation = new Vector2f(newX, newY);
        weapon.getSlot().getLocation().set(newLocation);
    }
}
