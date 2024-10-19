package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class AEG_MajinChange extends BaseShipSystemScript {

    private static final float DOMAIN_RADIUS = 2500f;
    private static final float EXPANSION_SPEED = 500f; // Speed at which the effect expands
    private static final float HEAL_PERCENTAGE = 0.25f; // Heal 25% of the damage
    private static final float SPEED_BOOST = 1.5f; // 50% speed boost
    private static final float MANEUVERABILITY_BOOST = 1.5f; // 50% maneuverability boost
    private final AEG_DomainExpansionVisuals visualsHelper = new AEG_DomainExpansionVisuals();
    private float currentRadius = 0f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        final ShipAPI ship = (ShipAPI) stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();

        if (ship == null) return;

        boolean isActive = state == State.IN || state == State.ACTIVE;

        if (isActive) {
            // Gradually increase the radius
            currentRadius = Math.min(DOMAIN_RADIUS, currentRadius + EXPANSION_SPEED * Global.getCombatEngine().getElapsedInLastFrame());

            // Create visual effects within the current radius
            visualsHelper.createDomainVisuals(engine, ship, isActive, currentRadius);

            // Boost all passive abilities
            stats.getHullRepairRatePercentPerSecond().modifyFlat(id, 0.1f); // Example boost for regeneration
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
                enemy.getMutableStats().getMaxSpeed().modifyMult(id, 0.5f); // Example debuff
                enemy.getMutableStats().getWeaponDamageTakenMult().modifyMult(id, 1.5f); // Example debuff
                enemy.getMutableStats().getFluxDissipation().modifyMult(id, 0.5f); // Additional debuff
            }

            // Ensure hull cannot be reduced below 1 while the domain is active
            if (ship.getHullLevel() < 0.01f) {
                ship.setHitpoints(1);
            }

            // Add damage taken modifier to handle last stand effect
            ship.addListener(new DamageTakenModifier() {
                @Override
                public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
                    if (target instanceof ShipAPI && target == ship) {
                        float hullLevel = ship.getHullLevel();
                        float damageAmount = damage.getDamage();
                        if (hullLevel * ship.getMaxHitpoints() - damageAmount < 1) {
                            float healAmount = damageAmount * HEAL_PERCENTAGE;
                            ship.setHitpoints(ship.getHitpoints() + healAmount);
                            damage.setDamage(0); // Nullify the fatal damage
                            return "last_stand";
                        }
                    }
                    return null;
                }
            });
        } else {
            // Remove all modifications when the system is not active
            deactivate(stats, id);
            currentRadius = 0f; // Reset the radius when the system is deactivated
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

        // Reset speed and maneuverability boosts
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
    }
}
