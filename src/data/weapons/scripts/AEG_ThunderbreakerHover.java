package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;

public class AEG_ThunderbreakerHover implements EveryFrameWeaponEffectPlugin {

    private float time = 0f;
    private static final float SWAY_AMPLITUDE = 10f; // Adjust as needed for left-right sway
    private static final float SWAY_FREQUENCY = 2f;  // Adjust as needed for left-right sway
    private static final float HOVER_AMPLITUDE = 1f; // Adjust as needed for up-down hover
    private static final float HOVER_FREQUENCY = 0.5f; // Adjust as needed for up-down hover
    private static final float JITTER_AMPLITUDE = 2f; // Adjust as needed for jitter effect
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

        // Calculate jitter offset
        float jitterOffsetX = (float) (Math.random() * JITTER_AMPLITUDE - JITTER_AMPLITUDE / 2);
        float jitterOffsetY = (float) (Math.random() * JITTER_AMPLITUDE - JITTER_AMPLITUDE / 2);

        // Calculate the new position
        float angle = weapon.getShip().getFacing();
        float rad = (float) Math.toRadians(angle);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float newX = originalLocation.x + swayOffset * cos - hoverOffset * sin + jitterOffsetX;
        float newY = originalLocation.y + swayOffset * sin + hoverOffset * cos + jitterOffsetY;

        Vector2f newLocation = new Vector2f(newX, newY);
        weapon.getSlot().getLocation().set(newLocation);

        // Add golden glitter effect when the weapon fires
        if (weapon.isFiring()) {
            addGoldenGlitterEffect(engine, weapon);
        }
    }

    private void addGoldenGlitterEffect(CombatEngineAPI engine, WeaponAPI weapon) {
        Vector2f weaponLocation = weapon.getLocation();
        for (int i = 0; i < 10; i++) {
            Vector2f particleVelocity = new Vector2f((float) Math.random() * 20 - 10, (float) Math.random() * 20 - 10);
            engine.addHitParticle(weaponLocation, particleVelocity, 5, 1, 0.5f, Color.YELLOW);
        }
    }
}
