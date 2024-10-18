package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class AEG_IronCutterScript implements EveryFrameWeaponEffectPlugin {
    private boolean hasFired = false;

    @Override
    public void advance(float amount, final CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        if (weapon.isFiring() && !hasFired) {
            hasFired = true;

            // Get ship and weapon location
            ShipAPI ship = weapon.getShip();
            Vector2f weaponLocation = weapon.getLocation();

            // Calculate spawn location 100 units in front of the weapon
            Vector2f spawnLocation = VectorUtils.getDirectionalVector(ship.getLocation(), weaponLocation);
            spawnLocation.scale(100f);
            Vector2f.add(spawnLocation, weaponLocation, spawnLocation);

            // Spawn the Iron Cutter ship
            CombatFleetManagerAPI fleetManager = engine.getFleetManager(ship.getOwner());
            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "AEG_IronCutter_x");
            if (fleetManager != null && member != null) {
                fleetManager.spawnFleetMember(member, spawnLocation, ship.getFacing(), 0f);
                final ShipAPI ironCutter = (ShipAPI) member.getStats().getEntity();

                if (ironCutter != null) {
                    // Set the target for the Iron Cutter
                    CombatEntityAPI target = ship.getShipTarget();
                    if (target == null) {
                        target = findNearestEnemy(engine, ironCutter);
                    }

                    if (target != null) {
                        Vector2f targetLocation = target.getLocation();
                        Vector2f directionToTarget = VectorUtils.getDirectionalVector(ironCutter.getLocation(), targetLocation);
                        directionToTarget.scale(1000f);
                        ironCutter.getVelocity().set(directionToTarget);
                    }

                    // Activate the ship system
                    ironCutter.useSystem();
                }
            }
        }

        if (hasFired && !weapon.isFiring()) {
            hasFired = false;
        }
    }

    private CombatEntityAPI findNearestEnemy(CombatEngineAPI engine, ShipAPI ship) {
        List<ShipAPI> potentialTargets = engine.getShips();
        CombatEntityAPI closestTarget = null;
        float closestDistance = Float.MAX_VALUE;

        for (ShipAPI entity : potentialTargets) {
            if (entity.getOwner() == ship.getOwner() || entity.isAlly()) continue;

            float distance = MathUtils.getDistance(ship.getLocation(), entity.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestTarget = entity;
            }
        }

        return closestTarget;
    }
}
