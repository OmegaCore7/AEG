package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import com.fs.starfarer.api.util.IntervalUtil;

public class AEG_ThunderbreakerHover implements EveryFrameWeaponEffectPlugin {

    private float time = 0f;
    private static final float SWAY_AMPLITUDE = 10f; // Adjust as needed for left-right sway
    private static final float SWAY_FREQUENCY = 2f;  // Adjust as needed for left-right sway
    private static final float HOVER_AMPLITUDE = 1f; // Adjust as needed for up-down hover
    private static final float HOVER_FREQUENCY = 0.5f; // Adjust as needed for up-down hover
    private Vector2f originalLocation = null;
    private final IntervalUtil empInterval = new IntervalUtil(2f, 3f);
    private static final Color CORE_COLOR = new Color(255, 255, 255, 255);
    private static final Color FRINGE_COLOR = new Color(105, 105, 255, 255);

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

        // Handle the passive lightning charging effect
        empInterval.advance(amount);
        if (empInterval.intervalElapsed()) {
            for (int i = 0; i < weapon.getSpec().getHardpointAngleOffsets().size(); i++) {
                Vector2f point = weapon.getFirePoint(i);
                Vector2f randomPoint = new Vector2f(point.x + (float) (Math.random() * 100 - 50), point.y + (float) (Math.random() * 100 - 50));
                engine.spawnEmpArcVisual(randomPoint, null, point, null, 10f, CORE_COLOR, FRINGE_COLOR);
            }
        }
    }
}
