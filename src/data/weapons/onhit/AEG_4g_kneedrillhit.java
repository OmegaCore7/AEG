package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

public class AEG_4g_kneedrillhit implements OnHitEffectPlugin {
    private final Random random = new Random();

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (shieldHit) {
            // Spawn EMP lightning arcs
            spawnEmpLightning(engine, point, target);
        } else {
            // Spawn explosion if no shield hit
            spawnExplosion(engine, point);
        }

        // Remove the original projectile
        engine.removeEntity(projectile);
    }

    private void spawnEmpLightning(CombatEngineAPI engine, Vector2f point, CombatEntityAPI target) {
        // Generate a random point within the ship's collision radius
        float angle = random.nextFloat() * 360f;  // Random angle
        float distance = random.nextFloat() * target.getCollisionRadius(); // Random distance within the collision radius
        Vector2f targetPoint = new Vector2f(
                (float) Math.cos(Math.toRadians(angle)) * distance + target.getLocation().x,
                (float) Math.sin(Math.toRadians(angle)) * distance + target.getLocation().y
        );

        // Spawn the EMP lightning arc
        engine.spawnEmpArcVisual(point, target, targetPoint, null, 5f, new Color(100,255,100,175), new Color(180,255,180,200));
    }

    private void spawnExplosion(CombatEngineAPI engine, Vector2f point) {
        // Spawn explosion at the point of impact
        engine.spawnExplosion(point, new Vector2f(), Color.gray, 10,1);
    }
}
