package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
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
    private final IntervalUtil empInterval = new IntervalUtil(4f, 6f);
    private float empArcLength = 200f; // Default EMP arc length

    public void setEmpArcLength(float length) {
        this.empArcLength = length;
    }

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
                engine.spawnEmpArc(
                        weapon.getShip(),
                        randomPoint,              // source point
                        null,                     // source entity (null = just a visual origin)
                        weapon.getShip(),         // target entity (or null if not hitting anything)
                        DamageType.ENERGY,
                        0f,                       // damage
                        0f,                       // emp
                        empArcLength + MathUtils.getRandom().nextInt(800), // max range
                        "terrain_hyperspace_lightning",                     // sound to play
                        15 + MathUtils.getRandom().nextInt(75),             // thickness
                        new Color(255,180 - MathUtils.getRandom().nextInt(130), 0, 255 - MathUtils.getRandom().nextInt(130)),                         // core color
                        new Color(255, 255 - MathUtils.getRandom().nextInt(50), 200 - MathUtils.getRandom().nextInt(60), 255 - MathUtils.getRandom().nextInt(80))                       // fringe color
                );
            }
        }
        // Configurable ellipse bounds (relative to ship's orientation)
        //Damn SS Flops around x,y from what you see in the editor
        float spreadForward = 225f; // Side
        float spreadBackward = 225f; // Side
        float spreadLeft = 100f; //Back
        float spreadRight = 45f;  //Front

// Random angle for polar generation
        angle = (float) (Math.random() * 2f * Math.PI);

// Determine unit vector (X and Y before rotation)
        float x = (float) Math.cos(angle);
        float y = (float) Math.sin(angle);

// Scale X and Y independently based on direction
        float scaledX = 0f;
        float scaledY = 0f;

        if (x >= 0f) {
            scaledX = x * spreadRight;
        } else {
            scaledX = x * spreadLeft;
        }

        if (y >= 0f) {
            scaledY = y * spreadForward;
        } else {
            scaledY = y * spreadBackward;
        }

// Combine into vector
        Vector2f offset = new Vector2f(scaledX, scaledY);

// Rotate offset to match ship facing
        float shipAngleRad = (float) Math.toRadians(weapon.getShip().getFacing());
        cos = (float) Math.cos(shipAngleRad);
        sin = (float) Math.sin(shipAngleRad);

        float rotatedX = offset.x * cos - offset.y * sin;
        float rotatedY = offset.x * sin + offset.y * cos;

// Final spawn position
        Vector2f spawnLoc = new Vector2f(
                weapon.getLocation().x + rotatedX,
                weapon.getLocation().y + rotatedY
        );

// Nebula particle under the ship/weapon
        engine.addNebulaParticle(
                spawnLoc,
                new Vector2f(0f, 0f),
                40f + (float) Math.random() * 20f, // size
                1.5f,
                0.2f,
                0.3f,
                1.0f,
                new Color(255, 80 + (int)(Math.random() * 120), 30, 50 + (int)(Math.random() * 150)),
                false // false = render under ship
        );

    }
}
