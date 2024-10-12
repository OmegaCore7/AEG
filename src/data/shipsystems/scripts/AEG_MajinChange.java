package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class AEG_MajinChange extends BaseShipSystemScript {

    private static final float DOMAIN_RADIUS = 2500f;
    private AEG_DomainExpansionVisuals visualsHelper = new AEG_DomainExpansionVisuals();

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();

        if (ship == null) return;

        boolean isActive = state == State.IN || state == State.ACTIVE;

        if (isActive) {
            // Create visual effects
            visualsHelper.createDomainVisuals(engine, ship, isActive);

            // Boost all passive abilities
            stats.getHullRepairRatePercentPerSecond().modifyFlat(id, 0.1f); // Example boost for regeneration
            stats.getEnergyWeaponDamageMult().modifyMult(id, 1.5f); // Example boost for assimilation
            stats.getBallisticWeaponDamageMult().modifyMult(id, 1.5f); // Example boost for strengthening
            stats.getSensorStrength().modifyFlat(id, 200f); // Example boost for dimensional prediction
            stats.getHullDamageTakenMult().modifyMult(id, 0.5f); // Example boost for adaptive defense
            stats.getArmorDamageTakenMult().modifyMult(id, 0.5f); // Example boost for adaptive defense
            stats.getHullDamageTakenMult().modifyFlat(id, 0f); // Example boost for last stand

            // Apply debilitating effects to enemy ships within the domain
            for (ShipAPI enemy : getEnemiesWithinRange(ship, DOMAIN_RADIUS)) {
                enemy.getMutableStats().getMaxSpeed().modifyMult(id, 0.5f); // Example debuff
                enemy.getMutableStats().getWeaponDamageTakenMult().modifyMult(id, 1.5f); // Example debuff
            }

            // Ensure hull cannot be reduced below 1 while the domain is active
            if (ship.getHullLevel() < 0.01f) {
                ship.setHitpoints(1);
            }
        } else {
            // Remove all modifications when the system is not active
            deactivate(stats, id);
        }
    }

    private List<ShipAPI> getEnemiesWithinRange(ShipAPI ship, float range) {
        List<ShipAPI> enemies = new ArrayList<>();
        for (ShipAPI other : Global.getCombatEngine().getShips()) {
            if (other.getOwner() != ship.getOwner() && MathUtils.getDistance(ship, other) <= range) {
                enemies.add(other);
            }
        }
        return enemies;
    }

    private void deactivate(MutableShipStatsAPI stats, String id) {
        // Reset all stats to their original values
        stats.getHullRepairRatePercentPerSecond().unmodify(id);
        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getBallisticWeaponDamageMult().unmodify(id);
        stats.getSensorStrength().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);
    }
}
