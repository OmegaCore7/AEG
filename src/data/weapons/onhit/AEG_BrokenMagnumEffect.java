package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AEG_BrokenMagnumEffect implements OnHitEffectPlugin {
    private static final float MIN_EXPLOSION_RADIUS = 30f;
    private static final float MAX_EXPLOSION_RADIUS = 50f;
    private static final float MAX_EXPLOSION_DAMAGE = 200f;
    private static final float BASE_EXPLOSION_DAMAGE = 50f;
    private static final float MAX_SPARK_DAMAGE = 100f; // EMP damage for sparks
    private static final float MAX_LINE_LENGTH = 300f; // Length of explosion line
    private static final Random random = new Random();

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (projectile == null || target == null || engine == null || point == null) {
            return; // Prevent crashes if any required object is missing
        }

        // Get missile's velocity and calculate its trajectory if possible
        Vector2f velocity = new Vector2f(projectile.getVelocity());
        Vector2f position = new Vector2f(point);

        // Determine explosion path length
        int explosionCount = Math.max(5, (int) (MAX_LINE_LENGTH / 50f)); // Approximate number of explosion points

        // If the projectile hits a shield, spawn sparks and connected explosions
        if (shieldHit) {
            spawnShieldEffects(projectile, position, velocity, explosionCount, engine);
        } else {
            // If it doesn't hit a shield, spawn only explosions
            spawnExplosionEffects(projectile, position, velocity, explosionCount, engine);
        }
    }

    // Spawn sparks and connected explosions on shield hit
    private void spawnShieldEffects(DamagingProjectileAPI projectile, Vector2f start, Vector2f velocity, int explosionCount, CombatEngineAPI engine) {
        // Randomly decide the number of sparks and their length
        int sparkCount = random.nextInt(5) + 3; // Between 3 and 7 sparks
        for (int i = 0; i < sparkCount; i++) {
            // Create random spark points within the explosion trajectory
            Vector2f sparkPoint = new Vector2f(start.x + random.nextFloat() * MAX_LINE_LENGTH, start.y + random.nextFloat() * MAX_LINE_LENGTH);
            engine.spawnExplosion(sparkPoint, new Vector2f(), projectile.getProjectileSpec().getFringeColor(), random.nextFloat() * 20f + 10f, 0.2f); // Spark explosion

            // Apply EMP damage for each spark
            engine.applyDamage(null, sparkPoint, MAX_SPARK_DAMAGE, DamageType.KINETIC, 100f, false, true, projectile.getSource());
        }

        // Spawn connected explosions along the missile's trajectory
        List<Vector2f> explosionPoints = calculateExplosionPath(start, velocity, explosionCount);
        for (int i = 0; i < explosionPoints.size(); i++) {
            Vector2f explosionPoint = explosionPoints.get(i);
            float explosionRadius = MIN_EXPLOSION_RADIUS + random.nextFloat() * (MAX_EXPLOSION_RADIUS - MIN_EXPLOSION_RADIUS);
            engine.spawnExplosion(explosionPoint, new Vector2f(), projectile.getProjectileSpec().getFringeColor(), explosionRadius, 0.2f);

            // Apply EMP damage as part of the connected explosions
            float explosionDamage = BASE_EXPLOSION_DAMAGE + (MAX_EXPLOSION_DAMAGE - BASE_EXPLOSION_DAMAGE) * ((float) i / explosionCount);
            engine.applyDamage(null, explosionPoint, explosionDamage, DamageType.HIGH_EXPLOSIVE, 0f, false, false, projectile.getSource());
        }
    }

    // Spawn explosion effects when no shield is hit
    private void spawnExplosionEffects(DamagingProjectileAPI projectile, Vector2f start, Vector2f velocity, int explosionCount, CombatEngineAPI engine) {
        // Spawn connected explosions along the missile's trajectory
        List<Vector2f> explosionPoints = calculateExplosionPath(start, velocity, explosionCount);
        for (int i = 0; i < explosionPoints.size(); i++) {
            Vector2f explosionPoint = explosionPoints.get(i);
            float explosionRadius = MIN_EXPLOSION_RADIUS + random.nextFloat() * (MAX_EXPLOSION_RADIUS - MIN_EXPLOSION_RADIUS);
            engine.spawnExplosion(explosionPoint, new Vector2f(), projectile.getProjectileSpec().getFringeColor(), explosionRadius, 0.2f);

            // Apply explosion damage, scaling as the explosions go along the trajectory
            float explosionDamage = BASE_EXPLOSION_DAMAGE + (MAX_EXPLOSION_DAMAGE - BASE_EXPLOSION_DAMAGE) * ((float) i / explosionCount);
            engine.applyDamage(null, explosionPoint, explosionDamage, DamageType.HIGH_EXPLOSIVE, 0f, false, false, projectile.getSource());
        }
    }

    // Calculate the path of explosions along the missile's trajectory
    private List<Vector2f> calculateExplosionPath(Vector2f start, Vector2f velocity, int explosionCount) {
        List<Vector2f> points = new ArrayList<>();

        if (velocity.length() == 0) return points; // Prevent division by zero

        float stepSize = velocity.length() / explosionCount; // Adjust explosion spacing
        Vector2f direction = new Vector2f(velocity);
        direction.normalise();

        for (int i = 1; i <= explosionCount; i++) {
            float scale = i * stepSize;
            Vector2f newPoint = new Vector2f(start.x + direction.x * scale, start.y + direction.y * scale);
            points.add(newPoint);
        }
        return points;
    }
}
