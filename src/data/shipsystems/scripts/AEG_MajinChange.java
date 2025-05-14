package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class AEG_MajinChange extends BaseShipSystemScript {
    private final List<ShipAPI> affectedEnemies = new ArrayList<>();
    private float arcTimer = 0f;
    private float fogTimer = 0f;
    private final float ARC_INTERVAL = 0.4f; // every 0.4 seconds
    // every 0.3 seconds
    // For visuals
    private static final float DOMAIN_RADIUS = 2500f;
    private static final float EXPANSION_SPEED = 500f; // Speed at which the effect expands
    private static final float SPEED_BOOST = 1.5f; // 50% speed boost
    private static final float MANEUVERABILITY_BOOST = 1.5f; // 50% maneuverability boost
    private final AEG_DomainExpansionVisuals visualsHelper = new AEG_DomainExpansionVisuals();
    private float currentRadius = 0f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        final ShipAPI ship = (ShipAPI) stats.getEntity();
        final CombatEngineAPI engine = Global.getCombatEngine();

        if (ship == null) return;

        boolean isActive = state == State.IN || state == State.ACTIVE;

        if (isActive) {
            // Tick timers
            arcTimer -= engine.getElapsedInLastFrame();
            fogTimer -= engine.getElapsedInLastFrame();

            if (arcTimer <= 0f) {
                AEG_DomainExpansionVisuals.spawnEdgeLightning(engine, ship, currentRadius);
                arcTimer = ARC_INTERVAL;
            }

            // Gradually increase the radius
            currentRadius = Math.min(DOMAIN_RADIUS, currentRadius + EXPANSION_SPEED * Global.getCombatEngine().getElapsedInLastFrame());

            // visualsHelper.createDomainVisuals(engine, ship, true, currentRadius);
            visualsHelper.renderDomain(engine, ship, currentRadius, isActive);

            // Boost all passive abilities
            restoreArmor(ship, 0.1f * Global.getCombatEngine().getElapsedInLastFrame()); // Example boost for armor regeneration
            stats.getEnergyWeaponDamageMult().modifyMult(id, 1.5f); // Example boost for assimilation
            stats.getBallisticWeaponDamageMult().modifyMult(id, 1.5f); // Example boost for strengthening
            stats.getSensorStrength().modifyFlat(id, 200f); // Example boost for dimensional prediction
            stats.getHullDamageTakenMult().modifyMult(id, 0.5f); // Example boost for adaptive defense
            stats.getArmorDamageTakenMult().modifyMult(id, 0.5f); // Example boost for adaptive defense

            // Speed and maneuverability boosts
            stats.getMaxSpeed().modifyMult(id, SPEED_BOOST);
            stats.getAcceleration().modifyMult(id, MANEUVERABILITY_BOOST);
            stats.getDeceleration().modifyMult(id, MANEUVERABILITY_BOOST);
            stats.getTurnAcceleration().modifyMult(id, MANEUVERABILITY_BOOST);
            stats.getMaxTurnRate().modifyMult(id, MANEUVERABILITY_BOOST);

            // Apply debilitating effects to enemy ships within the domain
            for (ShipAPI enemy : getEnemiesWithinRange(ship, currentRadius)) {
                if (!affectedEnemies.contains(enemy)) {
                    affectedEnemies.add(enemy);
                }
                enemy.getMutableStats().getMaxSpeed().modifyMult(id, 0.5f);
                enemy.getMutableStats().getWeaponDamageTakenMult().modifyMult(id, 1.5f);
                enemy.getMutableStats().getFluxDissipation().modifyMult(id, 0.5f);
            }

            // Full Power Causality Weapon
            if (!ship.hasListenerOfClass(AEG_CausalityListener.class)) {
                ship.addListener(new AEG_CausalityListener(ship, engine));
            }
        } else {
            // Remove all modifications when the system is not active
            deactivate(stats, id);
            currentRadius = 0f; // Reset the radius when the system is deactivated
            //Remove Listeners
            ship.removeListenerOfClass(AEG_CausalityListener.class);
        }
    }

    private void restoreArmor(ShipAPI ship, float amount) {
        ArmorGridAPI armorGrid = ship.getArmorGrid();
        float[][] armor = armorGrid.getGrid();
        float maxArmor = armorGrid.getMaxArmorInCell();

        for (int x = 0; x < armor.length; x++) {
            for (int y = 0; y < armor[x].length; y++) {
                float currentArmor = armor[x][y];
                if (currentArmor >= maxArmor) continue; // Skip fully-healed cells
                armor[x][y] = Math.min(currentArmor + (amount * maxArmor), maxArmor);
            }
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
        ShipAPI ship = (ShipAPI) stats.getEntity();
        // Reset all stats to their original values
        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getBallisticWeaponDamageMult().unmodify(id);
        stats.getSensorStrength().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);

        // Reset speed and maneuverability boosts
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        //Clear Timers
        arcTimer = 0f;
        fogTimer = 0f;

        // Clear visuals
        if (ship != null && Global.getCombatEngine() != null) {
            visualsHelper.renderDomain(Global.getCombatEngine(), ship, 0f, false);
        }
        for (ShipAPI enemy : affectedEnemies) {
            enemy.getMutableStats().getMaxSpeed().unmodify(id);
            enemy.getMutableStats().getWeaponDamageTakenMult().unmodify(id);
            enemy.getMutableStats().getFluxDissipation().unmodify(id);
        }
        affectedEnemies.clear();
    }
}
