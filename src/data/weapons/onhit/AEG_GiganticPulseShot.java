package data.weapons.onhit;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

public class AEG_GiganticPulseShot implements OnHitEffectPlugin {
    private static final Color[] COLORS = {
            new Color(144, 238, 144), // light green
            new Color(0, 128, 0),     // green
            new Color(173, 216, 230)  // light teal
    };
    private static final float MIN_SIZE = 1f;
    private static final float MAX_SIZE = 15f;
    private static final int MIN_PARTICLES = 15;
    private static final int MAX_PARTICLES = 25;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        Random random = new Random();
        int numParticles = MathUtils.getRandomNumberInRange(MIN_PARTICLES, MAX_PARTICLES);

        for (int i = 0; i < numParticles; i++) {
            float size = MathUtils.getRandomNumberInRange(MIN_SIZE, MAX_SIZE);
            Color color = COLORS[random.nextInt(COLORS.length)];
            Vector2f velocity = MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(50f, 200f), random.nextFloat() * 360f);

            // Adjust velocity to create a splatter effect
            velocity.scale(random.nextFloat() * 2f); // Randomize the speed
            velocity.x += random.nextFloat() * 50f - 25f; // Add some randomness to the direction
            velocity.y += random.nextFloat() * 50f - 25f;

            engine.addHitParticle(point, velocity, size, 1f, 1f, color);
        }
    }
}
