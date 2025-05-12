package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
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
            // Define ellipse dimensions relative to the weapon's slot
            float spreadForward = 225f;
            float spreadBackward = 225f;
            float spreadLeft = 100f;
            float spreadRight = 45f;

            // Generate random points within the ellipse
            for (int i = 0; i < 4; i++) {
                // Random angle within the ellipse
                float theta = (float) (Math.random() * 2f * Math.PI);
                float dirX = (float) Math.cos(theta);
                float dirY = (float) Math.sin(theta);

                // Scale to fit ellipse dimensions
                float scaledX = dirX >= 0f ? dirX * spreadRight : dirX * spreadLeft;
                float scaledY = dirY >= 0f ? dirY * spreadForward : dirY * spreadBackward;

                Vector2f localEllipsePoint = new Vector2f(scaledX, scaledY);

                // Rotate to match weapon's orientation
                float totalAngle = weapon.getCurrAngle();
                float totalAngleRad = (float) Math.toRadians(totalAngle);
                float wcos = (float) Math.cos(totalAngleRad);
                float wsin = (float) Math.sin(totalAngleRad);

                float rotatedX = localEllipsePoint.x * wcos - localEllipsePoint.y * wsin;
                float rotatedY = localEllipsePoint.x * wsin + localEllipsePoint.y * wcos;

                Vector2f ellipseWorldPoint = new Vector2f(
                        weapon.getLocation().x + rotatedX,
                        weapon.getLocation().y + rotatedY
                );

                // Generate a point further outward along the same direction (source of arc)
                float distanceOut = 300f + (float) Math.random() * 200f;
                Vector2f sourcePoint = new Vector2f(
                        ellipseWorldPoint.x + dirX * distanceOut,
                        ellipseWorldPoint.y + dirY * distanceOut
                );

                // Spawn arc from outer point to the ellipse
                engine.spawnEmpArcVisual(
                        sourcePoint,
                        null,
                        ellipseWorldPoint,
                        null,
                        5 + MathUtils.getRandom().nextInt(75),
                        new Color(255, 180 - MathUtils.getRandom().nextInt(130), 0, 255 - MathUtils.getRandom().nextInt(130)), // core color
                        new Color(255, 255 - MathUtils.getRandom().nextInt(50), 200 - MathUtils.getRandom().nextInt(60), 255 - MathUtils.getRandom().nextInt(80)) // fringe color
                );
                Global.getSoundPlayer().playSound(
                        "terrain_hyperspace_lightning", // sound id from sounds.json
                        1f, // pitch
                        1f, // volume
                        ellipseWorldPoint, // where the sound plays
                        new Vector2f(0f, 0f) // no relative velocity
                );
            }
        }
        //Spawn Nebula
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
        float totalAngleRad = (float) Math.toRadians(weapon.getCurrAngle());
        float wcos = (float) Math.cos(totalAngleRad);
        float wsin = (float) Math.sin(totalAngleRad);

        float rotatedX = offset.x * wcos - offset.y * wsin;
        float rotatedY = offset.x * wsin + offset.y * wcos;

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
