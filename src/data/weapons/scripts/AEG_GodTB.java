package data.weapons.scripts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

public class AEG_GodTB implements EveryFrameWeaponEffectPlugin {

    private static final Color[] LIGHTNING_COLORS = {
            new Color(255, 255, 255), // white core
            new Color(0, 255, 255),   // cyan fringe
            new Color(255, 255, 255), // white core
            new Color(0, 0, 255),     // blue fringe
            new Color(255, 0, 255),   // magenta core
            new Color(75, 0, 130)     // dark purple fringe
    };

    private IntervalUtil interval = new IntervalUtil(0.1f, 0.2f);
    private boolean doneStriking = false;
    private int strikes = 0;
    private int maxStrikes = 5;
    private CombatEntityAPI currentTarget = null;
    private float explosionDelay = 1.0f;
    private boolean explosionScheduled = false;
    private List<Vector2f> targetLocations = new ArrayList<>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!(weapon.getShip() instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) weapon.getShip();

        interval.advance(amount);
        if (interval.intervalElapsed() && !doneStriking) {
            if (currentTarget == null) {
                currentTarget = findInitialTarget(engine, ship);
            } else {
                currentTarget = findNextTarget(engine, currentTarget);
            }

            if (currentTarget != null) {
                strikeTargets(engine, ship, currentTarget);
                targetLocations.add(new Vector2f(currentTarget.getLocation()));
                strikes++;
                if (strikes >= maxStrikes) {
                    doneStriking = true;
                    explosionScheduled = true;
                }
            }
        }

        if (doneStriking && explosionScheduled) {
            explosionDelay -= amount;
            if (explosionDelay <= 0) {
                createExplosions(engine);
                explosionScheduled = false;
            }
        }
    }

    private void strikeTargets(CombatEngineAPI engine, ShipAPI ship, CombatEntityAPI target) {
        Vector2f from = ship.getLocation();
        Vector2f to = target.getLocation();

        for (Color color : LIGHTNING_COLORS) {
            engine.spawnEmpArc(ship, from, ship, target, DamageType.ENERGY, 0, 0, 1000, null, 10, color, color);
        }
    }

    private CombatEntityAPI findInitialTarget(CombatEngineAPI engine, ShipAPI ship) {
        // Implement logic to find the initial target
        return findNextTarget(engine, ship);
    }

    private CombatEntityAPI findNextTarget(CombatEngineAPI engine, CombatEntityAPI currentTarget) {
        float searchRadius = 1000f; // Adjust as needed
        List<ShipAPI> potentialTargets = engine.getAllShips();

        CombatEntityAPI closestTarget = null;
        float closestDistance = Float.MAX_VALUE;

        for (CombatEntityAPI entity : potentialTargets) {
            if (entity == currentTarget || !(entity instanceof ShipAPI)) continue;
            if (((ShipAPI) entity).isAlly()) continue; // Skip allies

            float distance = Misc.getDistance(currentTarget.getLocation(), entity.getLocation());
            if (distance < searchRadius && distance < closestDistance) {
                closestDistance = distance;
                closestTarget = entity;
            }
        }

        return closestTarget;
    }

    private void createExplosions(CombatEngineAPI engine) {
        for (Vector2f location : targetLocations) {
            engine.spawnExplosion(location, new Vector2f(), new Color(255, 255, 255), 100, 1);
        }
        targetLocations.clear(); // Clear the list after creating explosions
    }
}
