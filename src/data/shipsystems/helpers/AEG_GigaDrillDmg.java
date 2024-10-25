package data.shipsystems.helpers;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class AEG_GigaDrillDmg {

    private static final float DAMAGE_PER_SECOND = 500f; // Adjust this value as needed

    public static void applyDamage(CombatEngineAPI engine, ShipAPI ship, float amount) {
        Vector2f shipLocation = ship.getLocation();
        Vector2f[] trianglePoints = {
                new Vector2f(0, 0),
                new Vector2f(27, 70),
                new Vector2f(220, 0),
                new Vector2f(27, -70)
        };

        List<CombatEntityAPI> entities = CombatUtils.getEntitiesWithinRange(shipLocation, 220f); // Max range of the triangle

        if (entities != null) {
            for (CombatEntityAPI entity : entities) {
                if (entity instanceof ShipAPI && entity != ship) {
                    Vector2f entityLocation = entity.getLocation();
                    if (isPointInTriangle(entityLocation, shipLocation, trianglePoints)) {
                        float damage = DAMAGE_PER_SECOND * amount;
                        engine.applyDamage(entity, entity.getLocation(), damage, DamageType.ENERGY, 0f, false, false, ship);
                    }
                }
            }
        }
    }

    private static boolean isPointInTriangle(Vector2f point, Vector2f origin, Vector2f[] trianglePoints) {
        Vector2f p1 = new Vector2f(trianglePoints[0].x + origin.x, trianglePoints[0].y + origin.y);
        Vector2f p2 = new Vector2f(trianglePoints[1].x + origin.x, trianglePoints[1].y + origin.y);
        Vector2f p3 = new Vector2f(trianglePoints[2].x + origin.x, trianglePoints[2].y + origin.y);
        Vector2f p4 = new Vector2f(trianglePoints[3].x + origin.x, trianglePoints[3].y + origin.y);

        return isPointInTriangle(point, p1, p2, p3) || isPointInTriangle(point, p1, p3, p4);
    }

    private static boolean isPointInTriangle(Vector2f pt, Vector2f v1, Vector2f v2, Vector2f v3) {
        float d1, d2, d3;
        boolean hasNeg, hasPos;

        d1 = sign(pt, v1, v2);
        d2 = sign(pt, v2, v3);
        d3 = sign(pt, v3, v1);

        hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);

        return !(hasNeg && hasPos);
    }

    private static float sign(Vector2f p1, Vector2f p2, Vector2f p3) {
        return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y);
    }
}