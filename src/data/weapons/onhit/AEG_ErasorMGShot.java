package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

public class AEG_ErasorMGShot implements OnHitEffectPlugin {
    private static final float ROTATION_RATE = 500f; // Rotation rate in degrees per second
    private static final float DOUBLE_DAMAGE_MULT = 2.0f; // Double damage multiplier
    private static final float EXPLOSION_RADIUS = 50f; // Radius of the small explosions
    private static final Color EXPLOSION_COLOR = new Color(100, 255, 0, 255); // green color for explosions

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (proj == null || target == null || point == null || engine == null) return;

        // Create small explosion effect
        engine.spawnExplosion(point, new Vector2f(0, 0), EXPLOSION_COLOR, EXPLOSION_RADIUS, 1f);

        if (target instanceof ShipAPI) {
            final ShipAPI ship = (ShipAPI) target;
            final ShipAPI sourceShip = proj.getSource();

            // Determine the quadrant the projectile hit
            int quadrant = getQuadrant(ship, point);

            if (quadrant == 4) {
                // Apply double damage if hitting quadrant 4
                engine.applyDamage(target, point, proj.getDamageAmount() * DOUBLE_DAMAGE_MULT, proj.getDamageType(), 0f, false, false, proj.getSource());
            } else {
                // Rotate the ship until quadrant 4 is facing the source ship
                Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                    private float elapsed = 0f;

                    @Override
                    public void advance(float amount, List<InputEventAPI> events) {
                        if (Global.getCombatEngine().isPaused()) return;
                        elapsed += amount;

                        // Calculate the angle to the source ship
                        Vector2f directionToSource = Vector2f.sub(sourceShip.getLocation(), ship.getLocation(), null);
                        float angleToSource = (float) Math.toDegrees(Math.atan2(directionToSource.y, directionToSource.x));
                        float currentAngle = ship.getFacing();
                        float angleDifference = MathUtils.getShortestRotation(currentAngle, angleToSource + 180f);

                        // Rotate the ship
                        float rotationAmount = ROTATION_RATE * amount;
                        if (Math.abs(angleDifference) < rotationAmount) {
                            ship.setFacing(angleToSource + 180f);
                            Global.getCombatEngine().removePlugin(this);
                        } else {
                            ship.setFacing(currentAngle + Math.signum(angleDifference) * rotationAmount);
                        }
                    }
                });
            }
        }
    }

    private int getQuadrant(ShipAPI ship, Vector2f point) {
        Vector2f shipLocation = ship.getLocation();
        float angle = (float) Math.toDegrees(Math.atan2(point.y - shipLocation.y, point.x - shipLocation.x)) - ship.getFacing();
        angle = MathUtils.clampAngle(angle);

        if (angle >= -45 && angle <= 45) {
            return 1; // Front
        } else if (angle > 45 && angle < 135) {
            return 2; // East side
        } else if (angle < -45 && angle > -135) {
            return 3; // West side
        } else {
            return 4; // Back
        }
    }
}
