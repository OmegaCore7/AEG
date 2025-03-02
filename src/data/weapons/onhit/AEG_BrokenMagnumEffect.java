package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class AEG_BrokenMagnumEffect extends BaseEveryFrameCombatPlugin {
    private DamagingProjectileAPI projectile;
    private Vector2f previousLocation;

    public AEG_BrokenMagnumEffect() {
    }

    public AEG_BrokenMagnumEffect(DamagingProjectileAPI projectile) {
        this.projectile = projectile;
        this.projectile.setCollisionClass(CollisionClass.NONE);
        this.previousLocation = new Vector2f(projectile.getLocation());
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (projectile == null || projectile.didDamage()) {
            return;
        }

        CombatEntityAPI target = projectile.getDamageTarget();
        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            calculateTrajectoryAndSpawnExplosions(ship);
        }

        // Update previous location
        previousLocation.set(projectile.getLocation());
    }

    private void calculateTrajectoryAndSpawnExplosions(ShipAPI ship) {
        Vector2f currentLocation = projectile.getLocation();
        Vector2f velocity = projectile.getVelocity();
        float shipSize = ship.getCollisionRadius();
        float explosionSize = 50f; // Constant explosion size

        // Calculate the end point of the trajectory based on ship size
        Vector2f endPoint = new Vector2f(currentLocation);
        endPoint.translate(velocity.x * (shipSize / 100), velocity.y * (shipSize / 100)); // Adjust the multiplier as needed

        // Calculate the number of explosions based on ship size
        int numExplosions = (int) (shipSize / 50); // Adjust the divisor as needed

        // Spawn explosions along the trajectory
        for (int i = 0; i < numExplosions; i++) {
            float t = (float) i / (numExplosions - 1);
            Vector2f explosionPoint = new Vector2f(
                    previousLocation.x + t * (endPoint.x - previousLocation.x),
                    previousLocation.y + t * (endPoint.y - previousLocation.y)
            );
            spawnExplosion(explosionPoint, explosionSize);
        }
    }

    private void spawnExplosion(Vector2f location, float size) {
        CombatEngineAPI engine = Global.getCombatEngine();
        engine.spawnExplosion(location, new Vector2f(), new Color(255, 50, 0, 255), size, 1f);
    }
}