package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.List;

public class AEG_BrokenMagnumEffect extends BaseEveryFrameCombatPlugin {
    private DamagingProjectileAPI projectile;
    private boolean hasPierced = false;

    // No-argument constructor
    public AEG_BrokenMagnumEffect() {
    }

    // Constructor with projectile parameter
    public AEG_BrokenMagnumEffect(DamagingProjectileAPI projectile) {
        this.projectile = projectile;
        this.projectile.setCollisionClass(CollisionClass.NONE);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (projectile == null || projectile.didDamage()) {
            return;
        }

        CombatEntityAPI target = projectile.getDamageTarget();
        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            if (ship.getShield() != null && ship.getShield().isWithinArc(projectile.getLocation())) {
                spawnEMPArc(projectile.getLocation(), ship);
                applyKnockback(ship, projectile.getVelocity());
            } else if (ship.getCollisionClass() != CollisionClass.NONE) {
                spawnExplosion(projectile.getLocation());
                applyKnockback(ship, projectile.getVelocity());
                applyDamageOverTime(ship);
            }
        }
    }

    private void spawnEMPArc(Vector2f location, ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        engine.spawnEmpArcPierceShields(
                projectile.getSource(),
                location,
                null,
                ship,
                DamageType.ENERGY,
                100f,
                100f,
                10000f,
                null,
                10f,
                Color.CYAN,
                Color.WHITE
        );
    }

    private void spawnExplosion(Vector2f location) {
        CombatEngineAPI engine = Global.getCombatEngine();
        engine.spawnExplosion(location, new Vector2f(), new Color(255, 100, 0, 255), 100f, 1f);
    }

    private void applyKnockback(ShipAPI ship, Vector2f projectileVelocity) {
        Vector2f knockback = new Vector2f(projectileVelocity);
        knockback.scale(0.1f); // Adjust the scale factor as needed
        ship.getVelocity().translate(knockback.x, knockback.y);
    }

    private void applyDamageOverTime(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        float damagePerSecond = 10f; // Adjust as needed
        float duration = 5f; // Duration of the damage over time effect

        // Apply initial damage
        engine.applyDamage(
                ship,
                ship.getLocation(),
                projectile.getDamageAmount(),
                DamageType.HIGH_EXPLOSIVE,
                0f,
                false,
                false,
                projectile.getSource()
        );

        // Apply damage over time
        for (int i = 0; i < duration; i++) {
            engine.applyDamage(
                    ship,
                    ship.getLocation(),
                    damagePerSecond,
                    DamageType.HIGH_EXPLOSIVE,
                    0f,
                    false,
                    true,
                    projectile.getSource()
            );
        }
    }
}