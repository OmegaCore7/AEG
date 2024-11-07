package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

public class AEG_OmegaBlasterCharge implements EveryFrameWeaponEffectPlugin {
    private static final Color PARTICLE_COLOR = new Color(0, 255, 0); // green
    private static final float MAX_RADIUS = 300f;
    private static final int NUM_CYCLES = 3;
    private static final float CYCLE_DURATION = 0.66f; // 0.66 seconds per cycle
    private static final int PARTICLES_PER_CYCLE = 50;
    private static final int TRAIL_PARTICLES = 20;

    private float elapsedTime = 0f;
    private int currentCycle = 0;
    private final Random random = new Random();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        // Check if the weapon is charging
        if (weapon.getChargeLevel() > 0) {
            elapsedTime += amount;
            if (elapsedTime >= CYCLE_DURATION) {
                elapsedTime = 0f;
                currentCycle++;
                if (currentCycle >= NUM_CYCLES) {
                    currentCycle = 0;
                }
            }

            float progress = elapsedTime / CYCLE_DURATION;
            float radius = MAX_RADIUS * (1f - progress);
            Vector2f weaponLocation = weapon.getLocation();

            // Draw a solid circle
            engine.addSmoothParticle(weaponLocation, new Vector2f(0, 0), radius, 1f, 0.1f, PARTICLE_COLOR);

            // Add trailing particles for visual effect
            for (int i = 0; i < PARTICLES_PER_CYCLE; i++) {
                Vector2f position = MathUtils.getPointOnCircumference(weaponLocation, radius, MathUtils.getRandomNumberInRange(0f, 360f));
                float size = MathUtils.getRandomNumberInRange(3f, 7f); // Random thickness
                float alpha = MathUtils.getRandomNumberInRange(0.5f, 1f); // Random transparency
                Color particleColor = new Color(PARTICLE_COLOR.getRed(), PARTICLE_COLOR.getGreen(), PARTICLE_COLOR.getBlue(), (int) (alpha * 255));
                engine.addHitParticle(position, new Vector2f(0, 0), size, 1f, 0.1f, particleColor);

                // Add trailing particles
                for (int j = 0; j < TRAIL_PARTICLES; j++) {
                    float trailSize = MathUtils.getRandomNumberInRange(2f, 4f);
                    float trailAlpha = MathUtils.getRandomNumberInRange(0.2f, 0.5f);
                    Color trailColor = new Color(PARTICLE_COLOR.getRed(), PARTICLE_COLOR.getGreen(), PARTICLE_COLOR.getBlue(), (int) (trailAlpha * 255));
                    Vector2f trailPosition = MathUtils.getPointOnCircumference(position, MathUtils.getRandomNumberInRange(1f, 4f), MathUtils.getRandomNumberInRange(0f, 360f));
                    engine.addHitParticle(trailPosition, new Vector2f(0, 0), trailSize, 1f, 0.1f, trailColor);
                }
            }
        }
    }
}
