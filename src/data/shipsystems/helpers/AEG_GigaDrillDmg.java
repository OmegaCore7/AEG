package data.shipsystems.helpers;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_GigaDrillDmg {

    private static final float CHECK_INTERVAL = 0.1f; // Updated to 0.1 seconds
    private static final float ELLIPSE_WIDTH = 200f; // 100f on either side
    private static final float ELLIPSE_LENGTH = 400f; // Adjust as needed
    private static final float EXPLOSION_RADIUS = 100f; // Adjust as needed
    private static final float KINETIC_DAMAGE = 1000f; // Adjust as needed
    private static final float HIGH_EXPLOSIVE_DAMAGE = 1000f; // Adjust as needed

    private final IntervalUtil interval = new IntervalUtil(CHECK_INTERVAL, CHECK_INTERVAL);
    private final IntervalUtil empInterval = new IntervalUtil(1f, 2f); // EMP arcs every 1-2 seconds

    public void applyDrillDamage(ShipAPI ship, WeaponAPI drillWeapon, CombatEngineAPI engine, float collisionDamage) {
        interval.advance(engine.getElapsedInLastFrame());
        empInterval.advance(engine.getElapsedInLastFrame());

        if (interval.intervalElapsed()) {
            Vector2f shipLocation = ship.getLocation();
            float shipFacing = ship.getFacing();

            for (ShipAPI otherShip : engine.getShips()) {
                if (otherShip == ship || otherShip.isHulk() || otherShip.isShuttlePod()) continue;

                Vector2f otherLocation = otherShip.getLocation();
                if (isWithinEllipse(shipLocation, otherLocation, shipFacing)) {
                    spawnExplosion(engine, otherLocation);
                    dealDamage(ship, otherShip, engine, collisionDamage);
                }
            }
        }

        if (empInterval.intervalElapsed()) {
            spawnEMPArcs(ship, engine);
        }
    }

    private boolean isWithinEllipse(Vector2f center, Vector2f point, float facing) {
        float dx = point.x - center.x;
        float dy = point.y - center.y;

        float cos = (float) Math.cos(Math.toRadians(facing));
        float sin = (float) Math.sin(Math.toRadians(facing));

        float rotatedX = cos * dx + sin * dy;
        float rotatedY = -sin * dx + cos * dy;

        float ellipseWidth = ELLIPSE_WIDTH / 2;
        float ellipseLength = ELLIPSE_LENGTH / 2;

        return (rotatedX * rotatedX) / (ellipseLength * ellipseLength) + (rotatedY * rotatedY) / (ellipseWidth * ellipseWidth) <= 1;
    }

    private void spawnExplosion(CombatEngineAPI engine, Vector2f location) {
        engine.spawnExplosion(location, new Vector2f(), Color.ORANGE, EXPLOSION_RADIUS, 1f);
        engine.addHitParticle(location, new Vector2f(), EXPLOSION_RADIUS, 1f, 0.1f, Color.WHITE); // Railgun impact visual effect
    }

    private void dealDamage(ShipAPI ship, CombatEntityAPI entity, CombatEngineAPI engine, float collisionDamage) {
        DamageType damageType = entity instanceof ShipAPI && ((ShipAPI) entity).getShield() != null && ((ShipAPI) entity).getShield().isWithinArc(entity.getLocation())
                ? DamageType.KINETIC : DamageType.HIGH_EXPLOSIVE;

        engine.applyDamage(entity, entity.getLocation(), collisionDamage, damageType, 0f, false, false, ship);
    }

    private void spawnEMPArcs(ShipAPI ship, CombatEngineAPI engine) {
        Vector2f shipLocation = ship.getLocation();
        float shipFacing = ship.getFacing();

        for (int i = 0; i < 10; i++) { // Adjust the number of EMP arcs as needed
            float angleOffset = (i - 5) * 10; // Adjust the angle offset as needed
            float distance = i * (ELLIPSE_LENGTH / 10); // Adjust the distance as needed

            Vector2f arcStart = new Vector2f(shipLocation.x + distance * (float) Math.cos(Math.toRadians(shipFacing)),
                    shipLocation.y + distance * (float) Math.sin(Math.toRadians(shipFacing)));

            Vector2f arcEnd = new Vector2f(arcStart.x + ELLIPSE_WIDTH / 4 * (float) Math.cos(Math.toRadians(shipFacing + angleOffset)),
                    arcStart.y + ELLIPSE_WIDTH / 4 * (float) Math.sin(Math.toRadians(shipFacing + angleOffset)));

            engine.spawnEmpArc(ship, arcStart, null, new SimpleEntity(arcEnd), DamageType.ENERGY,
                    KINETIC_DAMAGE, // Damage
                    HIGH_EXPLOSIVE_DAMAGE, // Emp damage
                    200f, // Max range
                    "tachyon_lance_emp_impact",
                    10f,
                    new Color(105, 255, 105, 255),
                    Color.WHITE);
        }
    }
}
