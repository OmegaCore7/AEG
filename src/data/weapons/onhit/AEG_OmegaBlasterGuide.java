package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class AEG_OmegaBlasterGuide implements EveryFrameWeaponEffectPlugin {

    private static final int MAX_PUSHES = 5; // Maximum number of pushes
    private static final float PUSH_INTERVAL = 0.2f; // Interval between pushes in seconds
    private static final float PUSH_FORCE = 50f; // Force of each push

    private int pushCount = 0;
    private float pushTimer = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
        for (DamagingProjectileAPI projectile : projectiles) {
            if (projectile.getWeapon() == weapon && pushCount < MAX_PUSHES) {
                pushTimer += amount;
                if (pushTimer >= PUSH_INTERVAL) {
                    pushTimer = 0f;
                    applyHomingPush(projectile, engine);
                    pushCount++;
                }
            }
        }
    }

    private void applyHomingPush(DamagingProjectileAPI projectile, CombatEngineAPI engine) {
        CombatEntityAPI target = findTarget(projectile, engine);
        if (target != null) {
            Vector2f direction = Vector2f.sub(target.getLocation(), projectile.getLocation(), null);
            direction.normalise();
            direction.scale(PUSH_FORCE);
            projectile.getVelocity().translate(direction.x, direction.y);
        }
    }

    private CombatEntityAPI findTarget(DamagingProjectileAPI projectile, CombatEngineAPI engine) {
        List<ShipAPI> ships = engine.getShips();
        for (ShipAPI ship : ships) {
            if (ship.getOwner() != projectile.getSource().getOwner()) {
                return ship; // Return the first enemy ship found
            }
        }
        return null;
    }
}