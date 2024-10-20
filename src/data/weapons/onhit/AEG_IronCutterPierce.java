package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import java.util.List;

public class AEG_IronCutterPierce implements OnHitEffectPlugin {
    private int bouncesRemaining = 2;
    private CombatEntityAPI lastTarget = null;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (shieldHit) {
            handleShieldHit(projectile, target, point, engine);
        } else {
            handleUnshieldedHit(projectile, target, point, engine);
        }
    }

    private void handleShieldHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, CombatEngineAPI engine) {
        // Apply kinetic damage
        float kineticDamage = projectile.getDamageAmount();
        engine.applyDamage(target, point, kineticDamage, DamageType.KINETIC, 0, false, false, projectile.getSource());

        // Spawn hit sparks
        engine.addHitParticle(point, new Vector2f(), 50, 1, 0.1f, Color.YELLOW);

        // Play shield hit sound
        Global.getSoundPlayer().playSound("shield_hit", 1.0f, 1.0f, point, new Vector2f());

        // Bounce logic
        if (bouncesRemaining > 0) {
            bouncesRemaining--;
            findNewTarget(projectile, engine, target);
        } else {
            explode(projectile, point, engine);
        }
    }

    private void handleUnshieldedHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, CombatEngineAPI engine) {
        // Calculate entry and exit points
        Vector2f exitPoint = calculateExitPoint(target, point);

        // Spawn entry explosion
        engine.spawnExplosion(point, new Vector2f(), Color.ORANGE, 300, 1);

        // Play explosion sound
        Global.getSoundPlayer().playSound("explosion", 1.0f, 1.0f, point, new Vector2f());

        // Teleport projectile to exit point
        projectile.getLocation().set(exitPoint);

        // Spawn exit explosion
        engine.spawnExplosion(exitPoint, new Vector2f(), Color.ORANGE, 300, 1);

        // Play explosion sound
        Global.getSoundPlayer().playSound("explosion", 1.0f, 1.0f, exitPoint, new Vector2f());

        // Deal damage along the line
        dealLineDamage(target, point, exitPoint, engine, projectile);

        // Find new target
        findNewTarget(projectile, engine, target);
    }

    private Vector2f calculateExitPoint(CombatEntityAPI target, Vector2f entryPoint) {
        // Calculate the exit point on the opposite side of the ship
        Vector2f direction = new Vector2f();
        Vector2f.sub(target.getLocation(), entryPoint, direction);
        direction.normalise();
        direction.scale(target.getCollisionRadius() * 2);
        Vector2f exitPoint = new Vector2f();
        Vector2f.add(entryPoint, direction, exitPoint);
        return exitPoint;
    }

    private void dealLineDamage(CombatEntityAPI target, Vector2f entryPoint, Vector2f exitPoint, CombatEngineAPI engine, DamagingProjectileAPI projectile) {
        // Deal damage along the line between entry and exit points
        Vector2f direction = new Vector2f();
        Vector2f.sub(exitPoint, entryPoint, direction);
        direction.normalise();
        for (float i = 0; i <= 1; i += 0.1f) {
            Vector2f point = new Vector2f(entryPoint);
            point.x += direction.x * i * target.getCollisionRadius();
            point.y += direction.y * i * target.getCollisionRadius();
            engine.applyDamage(target, point, 50, DamageType.HIGH_EXPLOSIVE, 0, false, false, projectile.getSource());
        }
    }

    private void findNewTarget(DamagingProjectileAPI projectile, CombatEngineAPI engine, CombatEntityAPI currentTarget) {
        CombatEntityAPI newTarget = findTarget(engine, projectile);
        if (newTarget != null) {
            // Set new target and continue
            // Custom logic to handle targeting
            // Note: DamagingProjectileAPI does not have a setTarget method, so you need to handle this logic differently
        } else if (bouncesRemaining > 0 && currentTarget != lastTarget) {
            // Circle back to the same target
            lastTarget = currentTarget;
            // Custom logic to handle targeting
        } else {
            // Explode if no new target found or already hit the same target twice
            explode(projectile, projectile.getLocation(), engine);
        }
    }

    private CombatEntityAPI findTarget(CombatEngineAPI engine, DamagingProjectileAPI projectile) {
        // Logic to find a new target within a certain range
        List<CombatEntityAPI> entities = CombatUtils.getEntitiesWithinRange(projectile.getLocation(), 1000);
        for (CombatEntityAPI entity : entities) {
            if (entity != projectile.getSource() && entity != lastTarget) {
                return entity;
            }
        }
        return null;
    }

    private void explode(DamagingProjectileAPI projectile, Vector2f point, CombatEngineAPI engine) {
        engine.spawnExplosion(point, new Vector2f(), Color.RED, 300, 1);
        Global.getSoundPlayer().playSound("explosion", 1.0f, 1.0f, point, new Vector2f());
        engine.removeEntity(projectile);
    }
}
