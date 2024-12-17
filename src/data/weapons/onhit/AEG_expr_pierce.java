package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class AEG_expr_pierce implements OnHitEffectPlugin {
    private static final float MAX_EXPLOSION_DAMAGE = 100f;
    private static final Color FRINGE_COLOR = new Color(150,50,255,150);
    private static final Color CORE_COLOR = new Color(255,200,120,255);
    private static final float DAMAGE_MULTIPLIER = 2f;
    private static final int MAX_HITS = 3;
    private static final float HIT_WINDOW = 3f; // 3 seconds

    private static final Map<ShipAPI, Integer> hitCounts = new HashMap<ShipAPI, Integer>();
    private static final Map<ShipAPI, Float> hitTimers = new HashMap<ShipAPI, Float>();
    private Random random = new Random();

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (!(target instanceof ShipAPI)) {
            return;
        }

        ShipAPI ship = (ShipAPI) target;
        Vector2f hitPoint = new Vector2f(point);
        Vector2f direction = Misc.getUnitVectorAtDegreeAngle(projectile.getFacing()); // Direction of the projectile

        // Splash effect
        generateSplashEffect(engine, hitPoint, direction);

        // Calculate entry and exit points for the piercing effect
        Vector2f exitPoint = calculateExitPoint(ship, hitPoint, direction);

        // Deal damage along the line
        dealLineDamage(ship, hitPoint, exitPoint, engine, projectile, shieldHit);

        // Visual effects
        createVisualEffects(engine, hitPoint, exitPoint, shieldHit);

        // Update hit counts and timers
        updateHitCounts(ship);
    }

    private void generateSplashEffect(CombatEngineAPI engine, Vector2f hitPoint, Vector2f direction) {
        int numParticles = 10; // Number of particles to generate
        for (int i = 0; i < numParticles; i++) {
            float angle = random.nextFloat() * 360f;
            float speed = 50f + random.nextFloat() * 50f;
            Vector2f velocity = new Vector2f((float) Math.cos(Math.toRadians(angle)) * speed, (float) Math.sin(Math.toRadians(angle)) * speed);
            velocity.scale(-1); // Move opposite to the target

            float size = 5f + random.nextFloat() * 5f;
            float brightness = 1f;
            float duration = 0.5f + random.nextFloat() * 0.5f;

            engine.addHitParticle(hitPoint, velocity, size, brightness, duration, FRINGE_COLOR);
        }
    }

    private Vector2f calculateExitPoint(ShipAPI ship, Vector2f entryPoint, Vector2f direction) {
        Vector2f exitPoint = new Vector2f(entryPoint);
        direction.normalise();
        direction.scale(ship.getCollisionRadius() * 2);
        Vector2f.add(entryPoint, direction, exitPoint);
        return exitPoint;
    }

    private void dealLineDamage(ShipAPI ship, Vector2f entryPoint, Vector2f exitPoint, CombatEngineAPI engine, DamagingProjectileAPI projectile, boolean shieldHit) {
        Vector2f direction = new Vector2f();
        Vector2f.sub(exitPoint, entryPoint, direction);
        direction.normalise();
        float damageAmount = projectile.getDamageAmount();
        if (shieldHit) {
            damageAmount *= 2;
        }

        float explosionSizeIncrement = (50f - 10f) / 10f; // Increment size from 10 to 50 over 10 steps
        float currentExplosionSize = 10f;

        for (float i = 0; i <= 1; i += 0.1f) {
            Vector2f point = new Vector2f(entryPoint);
            point.x += direction.x * i * ship.getCollisionRadius();
            point.y += direction.y * i * ship.getCollisionRadius();
            engine.applyDamage(ship, point, damageAmount, DamageType.HIGH_EXPLOSIVE, 0, false, false, projectile.getSource());
            engine.spawnExplosion(point, new Vector2f(), CORE_COLOR, currentExplosionSize, 1f);
            currentExplosionSize += explosionSizeIncrement;
        }

        // Spawn a larger explosion at the rear of the ship
        engine.spawnExplosion(exitPoint, new Vector2f(), CORE_COLOR, 50f, 1f);
        engine.applyDamage(ship, exitPoint, MAX_EXPLOSION_DAMAGE, DamageType.HIGH_EXPLOSIVE, 0, false, false, projectile.getSource());
    }

    private void createVisualEffects(CombatEngineAPI engine, Vector2f entryPoint, Vector2f exitPoint, boolean shieldHit) {
        Vector2f direction = new Vector2f();
        Vector2f.sub(exitPoint, entryPoint, direction);
        direction.normalise();
        for (float i = 0; i <= 1; i += 0.1f) {
            Vector2f point = new Vector2f(entryPoint);
            point.x += direction.x * i * 100;
            point.y += direction.y * i * 100;
            engine.spawnExplosion(point, new Vector2f(), CORE_COLOR, 100, 1);
        }
        // Entry and exit explosions
        engine.spawnExplosion(entryPoint, new Vector2f(), CORE_COLOR, 300, 1);
        engine.spawnExplosion(exitPoint, new Vector2f(), CORE_COLOR, 300, 1);

        // EMP hit sparks if shield hit
        if (shieldHit) {
            engine.addHitParticle(entryPoint, new Vector2f(), 50, 1, 0.1f, Color.CYAN);
        }
    }

    private void updateHitCounts(ShipAPI ship) {
        Integer hitCount = hitCounts.get(ship);
        hitCounts.put(ship, Math.min(hitCount != null ? hitCount + 1 : 1, MAX_HITS));
        hitTimers.put(ship, HIT_WINDOW);
    }

    public static void advance(float amount) {
        Iterator<Map.Entry<ShipAPI, Float>> iterator = hitTimers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ShipAPI, Float> entry = iterator.next();
            float newTime = entry.getValue() - amount;
            if (newTime <= 0) {
                hitCounts.put(entry.getKey(), 0);
                iterator.remove();
            } else {
                hitTimers.put(entry.getKey(), newTime);
            }
        }
    }
}