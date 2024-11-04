package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

public class AEG_GiganticPulseCharge implements EveryFrameWeaponEffectPlugin {

    private static final int DURATION = 4000; // 4 seconds in milliseconds
    private static final int INTERVAL = 100; // Interval in milliseconds
    private static final float MAX_DISTANCE = 10.0f; // Maximum distance for transparency calculation
    private static final Color[] COLORS = {new Color(144, 238, 144), new Color(0, 128, 0), new Color(173, 216, 230)}; // light green, green, light teal

    private long startTime = -1;
    private final Random random = new Random();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (startTime < 0) {
            startTime = System.currentTimeMillis();
        }

        if (System.currentTimeMillis() - startTime < DURATION) {
            if (engine.getElapsedInLastFrame() >= INTERVAL / 1000.0f) {
                float angle = random.nextFloat() * 45 - 22.5f; // Random angle within 45 degrees
                float distance = random.nextFloat() * MAX_DISTANCE;
                float transparency = 1.0f - (distance / MAX_DISTANCE);
                Color color = COLORS[random.nextInt(COLORS.length)];

                Vector2f direction = new Vector2f(1, 0);
                VectorUtils.rotate(direction, weapon.getCurrAngle() + angle);
                Vector2f spawnPoint = new Vector2f(weapon.getLocation());
                Vector2f.add(spawnPoint, (Vector2f) direction.scale(distance), spawnPoint);

                createParticle(engine, spawnPoint, transparency, color);
            }
        }
    }

    private void createParticle(CombatEngineAPI engine, Vector2f location, float transparency, Color color) {
        Color particleColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (transparency * 255));
        engine.addHitParticle(location, new Vector2f(0, 0), 10.0f, 1.0f, 1.0f, particleColor);
    }
}