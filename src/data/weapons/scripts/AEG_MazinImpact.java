package data.weapons.scripts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

public class AEG_MazinImpact implements EveryFrameWeaponEffectPlugin {

    private static final float SEARCH_RADIUS = 500f;
    private static final float CHARGE_SPEED = 300f;
    private static final float COLLISION_RANGE = 10f;
    private static final float BACKUP_DISTANCE = 20f;
    private static final float SPARK_LENGTH = 4f;
    private static final int DAMAGE_HULL = 2000;
    private static final int DAMAGE_SHIELD_MULTIPLIER = 2;

    private CombatEntityAPI target = null;
    private boolean charging = false;
    private boolean backingUp = false;
    private boolean secondCharge = false;
    private float backupTime = 0f;
    private float chargeTime = 0f;
    private int collisionCount = 0;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!(weapon.getShip() instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) weapon.getShip();

        // Check if the weapon is in cooldown
        if (weapon.getCooldownRemaining() > 0 && !charging && !backingUp && !secondCharge) {
            target = findNearestTarget(engine, ship);
            if (target != null) {
                tiltShip(ship, -45);
                charging = true;
            }
        }

        if (charging) {
            chargeTowardsTarget(ship, target, amount, engine);
        }

        if (backingUp) {
            backupFromTarget(ship, amount);
        }

        if (secondCharge) {
            chargeTowardsTarget(ship, target, amount, engine);
        }
    }

    private CombatEntityAPI findNearestTarget(CombatEngineAPI engine, ShipAPI ship) {
        List<ShipAPI> potentialTargets = engine.getAllShips();
        CombatEntityAPI closestTarget = null;
        float closestDistance = Float.MAX_VALUE;

        for (CombatEntityAPI entity : potentialTargets) {
            if (entity == ship || !(entity instanceof ShipAPI)) continue;
            if (((ShipAPI) entity).isAlly()) continue; // Skip allies

            float distance = Misc.getDistance(ship.getLocation(), entity.getLocation());
            if (distance < SEARCH_RADIUS && distance < closestDistance) {
                closestDistance = distance;
                closestTarget = entity;
            }
        }

        return closestTarget;
    }

    private void tiltShip(ShipAPI ship, float angle) {
        ship.setFacing(ship.getFacing() + angle);
    }

    private void chargeTowardsTarget(ShipAPI ship, CombatEntityAPI target, float amount, CombatEngineAPI engine) {
        Vector2f direction = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(ship.getLocation(), target.getLocation()));
        direction.scale(CHARGE_SPEED * amount);
        Vector2f.add(ship.getVelocity(), direction, ship.getVelocity());

        if (Misc.getDistance(ship.getLocation(), target.getLocation()) < COLLISION_RANGE) {
            charging = false;
            backingUp = true;
            backupTime = BACKUP_DISTANCE / CHARGE_SPEED;
            applyDamage(ship, target, engine);
            createSparks(engine, target.getLocation());
            collisionCount++;
        }
    }

    private void backupFromTarget(ShipAPI ship, float amount) {
        backupTime -= amount;
        if (backupTime <= 0) {
            backingUp = false;
            secondCharge = true;
            tiltShip(ship, 90); // Tilt 45 degrees clockwise from the initial position
        } else {
            Vector2f direction = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(ship.getLocation(), target.getLocation()) + 180);
            direction.scale(CHARGE_SPEED * amount);
            Vector2f.add(ship.getVelocity(), direction, ship.getVelocity());
        }
    }

    private void applyDamage(ShipAPI ship, CombatEntityAPI target, CombatEngineAPI engine) {
        float damage = DAMAGE_HULL;
        if (target instanceof ShipAPI && ((ShipAPI) target).getShield() != null && ((ShipAPI) target).getShield().isWithinArc(target.getLocation())) {
            damage *= DAMAGE_SHIELD_MULTIPLIER;
        }
        engine.applyDamage(target, target.getLocation(), damage, DamageType.KINETIC, 0, false, false, ship);
    }

    private void createSparks(CombatEngineAPI engine, Vector2f location) {
        for (int i = 0; i < collisionCount; i++) {
            Vector2f sparkLocation = Misc.getPointWithinRadius(location, COLLISION_RANGE);
            if (i % 2 == 0) {
                // White core, yellow fringe
                engine.addHitParticle(sparkLocation, new Vector2f(), SPARK_LENGTH, 1, 0.1f, new Color(255, 255, 255));
            } else {
                // Yellow core, orange fringe
                engine.addHitParticle(sparkLocation, new Vector2f(), SPARK_LENGTH, 1, 0.1f, new Color(255, 255, 0));
            }
        }
    }
}
