package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

public class AEG_OmegaBlasterOnHit implements OnHitEffectPlugin, EveryFrameWeaponEffectPlugin {

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point,
                      boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        // No changes needed here for the new conditions
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
        for (DamagingProjectileAPI projectile : projectiles) {
            if (projectile.getWeapon() == weapon) {
                if (shouldTriggerEffects(projectile, engine)) {
                    triggerEffects(projectile, engine);
                }
            }
        }
    }

    private boolean shouldTriggerEffects(DamagingProjectileAPI projectile, CombatEngineAPI engine) {
        // Check if the projectile has expired
        if (projectile.isExpired()) {
            return true;
        }

        // Check if the projectile is within 10su of the target's collision radius
        for (CombatEntityAPI entity : engine.getShips()) {
            if (entity instanceof ShipAPI && entity.getOwner() != projectile.getSource().getOwner()) {
                ShipAPI ship = (ShipAPI) entity;
                float distanceToShip = MathUtils.getDistance(projectile, ship);
                if (distanceToShip <= ship.getCollisionRadius() + 10f) {
                    // Slow down the projectile
                    projectile.getVelocity().scale(0.1f); // Reduce speed to 10% of original
                    return true;
                }
            }
        }

        return false;
    }

    private void triggerEffects(DamagingProjectileAPI projectile, CombatEngineAPI engine) {
        Vector2f point = projectile.getLocation();
        ShipAPI ship = null;

        // Find the ship within the collision radius
        for (CombatEntityAPI entity : engine.getShips()) {
            if (entity instanceof ShipAPI && entity.getOwner() != projectile.getSource().getOwner()) {
                ShipAPI potentialShip = (ShipAPI) entity;
                if (MathUtils.getDistance(projectile, potentialShip) <= potentialShip.getCollisionRadius() + 10f) {
                    ship = potentialShip;
                    break;
                }
            }
        }

        if (ship != null) {
            float damage = projectile.getDamageAmount();

            // Create green explosion and deal damage
            engine.spawnExplosion(point, new Vector2f(0, 0), Misc.setAlpha(new Color(0, 255, 0), 255),
                    100f, 1f);
            engine.applyDamage(ship, point, damage, DamageType.ENERGY, 0, false, false, projectile.getSource());

            // Create ring of electricity and deal EMP damage
            for (int i = 0; i < 360; i += 30) {
                float angle = (float) Math.toRadians(i);
                Vector2f offset = new Vector2f((float) Math.cos(angle) * 50, (float) Math.sin(angle) * 50);
                Vector2f spawnPoint = Vector2f.add(point, offset, null);
                engine.spawnEmpArc(projectile.getSource(), point, ship, ship,
                        DamageType.ENERGY, 0, damage, 1000f, null, 10f,
                        new Color(0, 255, 0), new Color(0, 255, 255));
            }
        }
    }
}
