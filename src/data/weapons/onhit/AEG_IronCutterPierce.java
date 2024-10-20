package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;

public class AEG_IronCutterPierce implements OnHitEffectPlugin {
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        // Calculate entry and exit points
        Vector2f exitPoint = calculateExitPoint(target, point);

        // Deal damage along the line
        dealLineDamage(target, point, exitPoint, engine, projectile, shieldHit);

        // Visual effects
        createVisualEffects(engine, point, exitPoint, shieldHit);

        // Remove the original projectile
        engine.removeEntity(projectile);
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

    private void dealLineDamage(CombatEntityAPI target, Vector2f entryPoint, Vector2f exitPoint, CombatEngineAPI engine, DamagingProjectileAPI projectile, boolean shieldHit) {
        // Deal damage along the line between entry and exit points
        Vector2f direction = new Vector2f();
        Vector2f.sub(exitPoint, entryPoint, direction);
        direction.normalise();
        float damageAmount = projectile.getDamageAmount();
        if (shieldHit) {
            damageAmount *= 2;
        }
        for (float i = 0; i <= 1; i += 0.1f) {
            Vector2f point = new Vector2f(entryPoint);
            point.x += direction.x * i * target.getCollisionRadius();
            point.y += direction.y * i * target.getCollisionRadius();
            engine.applyDamage(target, point, damageAmount, DamageType.HIGH_EXPLOSIVE, 0, false, false, projectile.getSource());
        }
    }

    private void createVisualEffects(CombatEngineAPI engine, Vector2f entryPoint, Vector2f exitPoint, boolean shieldHit) {
        // Create visual effects along the line
        Vector2f direction = new Vector2f();
        Vector2f.sub(exitPoint, entryPoint, direction);
        direction.normalise();
        for (float i = 0; i <= 1; i += 0.1f) {
            Vector2f point = new Vector2f(entryPoint);
            point.x += direction.x * i * 100;
            point.y += direction.y * i * 100;
            engine.spawnExplosion(point, new Vector2f(), Color.ORANGE, 100, 1);
        }
        // Entry and exit explosions
        engine.spawnExplosion(entryPoint, new Vector2f(), Color.ORANGE, 300, 1);
        engine.spawnExplosion(exitPoint, new Vector2f(), Color.ORANGE, 300, 1);

        // EMP hit sparks if shield hit
        if (shieldHit) {
            engine.addHitParticle(entryPoint, new Vector2f(), 50, 1, 0.1f, Color.CYAN);
        }
    }
}
