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
    private static final float HEAL_PERCENTAGE = 0.50f; // Heal 50% of the damage
    private static final float REFLECT_PERCENTAGE = 0.50f; // Reflect 50% of the damage
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
            // Gradually increase the radius
            currentRadius = Math.min(DOMAIN_RADIUS, currentRadius + EXPANSION_SPEED * Global.getCombatEngine().getElapsedInLastFrame());

            // Create visual effects within the current radius
            visualsHelper.createDomainVisuals(engine, ship, true, currentRadius);

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
                enemy.getMutableStats().getMaxSpeed().modifyMult(id, 0.5f); // Example debuff
                enemy.getMutableStats().getWeaponDamageTakenMult().modifyMult(id, 1.5f); // Example debuff
                enemy.getMutableStats().getFluxDissipation().modifyMult(id, 0.5f); // Additional debuff
            }

            // Full Power Causality Weapon
            ship.addListener(new DamageTakenModifier() {
                @Override
                public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
                    if (target instanceof ShipAPI && target == ship) {
                        float hullLevel = ship.getHullLevel();
                        if (hullLevel <= 0.20f) {
                            float damageAmount = damage.getDamage();
                            float healAmount = damageAmount * HEAL_PERCENTAGE;
                            float reflectAmount = damageAmount * REFLECT_PERCENTAGE;

                            // Heal the ship
                            ship.setHitpoints(ship.getHitpoints() + healAmount);

                            // Reflect damage to enemies within the radius
                            for (ShipAPI enemy : getEnemiesWithinRange(ship, currentRadius)) {
                                engine.applyDamage(enemy, point, reflectAmount, damage.getType(), 0f, false, false, ship);
                            }

                            // Nullify the original damage
                            damage.setDamage(0);
                            return "full_power_causality_weapon";
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

    private void restoreArmor(ShipAPI ship, float amount) {
        ArmorGridAPI armorGrid = ship.getArmorGrid();
        float[][] armor = armorGrid.getGrid();
        float maxArmor = armorGrid.getMaxArmorInCell();

        for (int x = 0; x < armor.length; x++) {
            for (int y = 0; y < armor[x].length; y++) {
                float currentArmor = armor[x][y];
                if (currentArmor < maxArmor) {
                    armor[x][y] = Math.min(currentArmor + (amount * maxArmor), maxArmor);
                }
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
    }
}
