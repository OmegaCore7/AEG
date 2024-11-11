package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

public class AEG_GiganticPulseCharge implements EveryFrameWeaponEffectPlugin {

    private static final int INTERVAL = 100; // Interval in milliseconds
    private static final float MAX_DISTANCE = 10.0f; // Maximum distance for transparency calculation
    private static final Color[] COLORS = {new Color(144, 238, 144), new Color(0, 128, 0), new Color(173, 216, 230)}; // light green, green, light teal

    private long lastEffectTime = -1;
    private final Random random = new Random();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        // Check if the weapon is active (firing)
        if (weapon.isFiring()) {
            // Initialize the last effect time if not already set
            if (lastEffectTime < 0) {
                lastEffectTime = System.currentTimeMillis();
            }

            // Check if the interval has passed
            if (System.currentTimeMillis() - lastEffectTime >= INTERVAL) {
                lastEffectTime = System.currentTimeMillis();

                // Generate a random angle within 45 degrees
                float angle = random.nextFloat() * 45 - 22.5f;
                // Generate a random distance within the maximum distance
                float distance = random.nextFloat() * MAX_DISTANCE;
                // Calculate transparency based on distance
                float transparency = 1.0f - (distance / MAX_DISTANCE);
                // Select a random color from the array
                Color color = COLORS[random.nextInt(COLORS.length)];

                // Calculate the direction vector based on the weapon's current angle
                Vector2f direction = new Vector2f(1, 0);
                VectorUtils.rotate(direction, weapon.getCurrAngle() + angle);
                // Calculate the spawn point for the particle
                Vector2f spawnPoint = new Vector2f(weapon.getLocation());
                Vector2f.add(spawnPoint, (Vector2f) direction.scale(distance), spawnPoint);

                // Create the particle effect
                createParticle(engine, spawnPoint, transparency, color);
            }
        } else {
            // Reset the last effect time when the weapon stops firing
            lastEffectTime = -1;
        }
    }

    private void createParticle(CombatEngineAPI engine, Vector2f location, float transparency, Color color) {
        // Create a particle with the specified color and transparency
        Color particleColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (transparency * 255));
        engine.addHitParticle(location, new Vector2f(0, 0), 10.0f, 1.0f, 1.0f, particleColor);
    }
}
