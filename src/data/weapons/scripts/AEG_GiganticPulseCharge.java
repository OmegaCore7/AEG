package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

public class AEG_GiganticPulseCharge implements EveryFrameWeaponEffectPlugin {

    private static final int MIN_INTERVAL = 1000; // Minimum interval in milliseconds
    private static final int MAX_INTERVAL = 2000; // Maximum interval in milliseconds
    private static final float MAX_RADIUS = 100.0f; // Maximum radius for EMP arc start point
    private static final Color ARC_COLOR = new Color(173, 255, 47); // Bright green with a hint of yellow

    private long lastEffectTime = -1;
    private final Random random = new Random();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        // Initialize the last effect time if not already set
        if (lastEffectTime < 0) {
            lastEffectTime = System.currentTimeMillis();
        }

        // Check if the interval has passed
        if (System.currentTimeMillis() - lastEffectTime >= getRandomInterval()) {
            lastEffectTime = System.currentTimeMillis();

            // Generate a random angle and distance within the maximum radius
            float angle = random.nextFloat() * 360;
            float distance = random.nextFloat() * MAX_RADIUS;

            // Calculate the start point for the EMP arc
            Vector2f startPoint = new Vector2f(weapon.getLocation());
            Vector2f direction = new Vector2f(1, 0);
            VectorUtils.rotate(direction, angle);
            Vector2f.add(startPoint, (Vector2f) direction.scale(distance), startPoint);

            // Create the EMP arc
            createEMPArc(engine, startPoint, weapon.getLocation());
        }
    }

    private int getRandomInterval() {
        return MIN_INTERVAL + random.nextInt(MAX_INTERVAL - MIN_INTERVAL + 1);
    }

    private void createEMPArc(CombatEngineAPI engine, Vector2f start, Vector2f end) {
        engine.spawnEmpArcVisual(start, null, end, null, 10.0f, ARC_COLOR, ARC_COLOR);
    }
}
