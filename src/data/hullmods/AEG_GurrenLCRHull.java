package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_GurrenLCRHull extends BaseHullMod {

    private static final float ARMOR_REGEN_MULTIPLIER = 3.0f;
    private static final float HULL_REGEN_RATE = 0.01f; // Hull repair rate per second
    private static final float EXPLOSION_RADIUS = 600f;
    private static final Color EXPLOSION_CORE_COLOR = new Color(144, 238, 144); // Light green core
    private static final Color EXPLOSION_FRINGE_COLOR = new Color(173, 255, 47); // Light greenish teal radius

    private static final float ARMOR_DAMAGE_REDUCTION_HIGH = 0.7f; // 30% reduction
    private static final float ARMOR_DAMAGE_REDUCTION_MEDIUM = 0.8f; // 20% reduction
    private static final float ARMOR_DAMAGE_REDUCTION_LOW = 0.9f; // 10% reduction

    private static final float HULL_DAMAGE_REDUCTION_HIGH = 0.75f; // 25% reduction
    private static final float HULL_DAMAGE_REDUCTION_MEDIUM = 0.85f; // 15% reduction
    private static final float HULL_DAMAGE_REDUCTION_LOW = 0.95f; // 5% reduction

    private static final float SPEED_BONUS_HIGH = 1.2f; // 20% increase
    private static final float SPEED_BONUS_MEDIUM = 1.1f; // 10% increase
    private static final float SPEED_BONUS_LOW = 1.05f; // 5% increase

    private static final float MANEUVERABILITY_BONUS_HIGH = 1.3f; // 30% increase
    private static final float MANEUVERABILITY_BONUS_MEDIUM = 1.2f; // 20% increase
    private static final float MANEUVERABILITY_BONUS_LOW = 1.1f; // 10% increase

    private static final float EMP_RESISTANCE_HIGH = 0.5f; // 50% reduction
    private static final float EMP_RESISTANCE_MEDIUM = 0.7f; // 30% reduction
    private static final float EMP_RESISTANCE_LOW = 0.9f; // 10% reduction

    private static final float CRITICAL_HIT_CHANCE_HIGH = 1.5f; // 50% increase
    private static final float CRITICAL_HIT_CHANCE_MEDIUM = 1.3f; // 30% increase
    private static final float CRITICAL_HIT_CHANCE_LOW = 1.1f; // 10% increase
//Spiral Phase Space Transwarp System
    private static final float PHASE_SPEED_BONUS = 6f; // 50% speed increase when phased
    private static final float PHASE_MANEUVERABILITY_BONUS = 6f; // 50% maneuverability increase when phased

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || ship.isHulk() || Global.getCombatEngine().isPaused()) {
            return;
        }

        float cr = ship.getCurrentCR();
        float armorRegenRate = HULL_REGEN_RATE * ARMOR_REGEN_MULTIPLIER * cr;
        float hullRegenRate = HULL_REGEN_RATE * cr;

        // Regenerate hull
        ship.setHitpoints(ship.getHitpoints() + hullRegenRate * amount * ship.getMaxHitpoints());
        if (ship.getHitpoints() > ship.getMaxHitpoints()) {
            ship.setHitpoints(ship.getMaxHitpoints());
        }

        // Regenerate armor
        float[][] armorGrid = ship.getArmorGrid().getGrid();
        for (int i = 0; i < armorGrid.length; i++) {
            for (int j = 0; j < armorGrid[i].length; j++) {
                armorGrid[i][j] += armorRegenRate * amount;
                if (armorGrid[i][j] > ship.getArmorGrid().getMaxArmorInCell()) {
                    armorGrid[i][j] = ship.getArmorGrid().getMaxArmorInCell();
                }
            }
        }

        // Apply bonuses based on CR
        applyBonuses(ship, cr);

        // Apply speed and maneuverability bonuses when phased
        if (ship.getPhaseCloak() != null && ship.getPhaseCloak().isActive()) {
            ship.getMutableStats().getMaxSpeed().modifyMult("AEG_GurrenLCRHull_phase", PHASE_SPEED_BONUS);
            ship.getMutableStats().getAcceleration().modifyMult("AEG_GurrenLCRHull_phase", PHASE_MANEUVERABILITY_BONUS);
            ship.getMutableStats().getDeceleration().modifyMult("AEG_GurrenLCRHull_phase", PHASE_MANEUVERABILITY_BONUS);
            ship.getMutableStats().getTurnAcceleration().modifyMult("AEG_GurrenLCRHull_phase", PHASE_MANEUVERABILITY_BONUS);
            ship.getMutableStats().getMaxTurnRate().modifyMult("AEG_GurrenLCRHull_phase", PHASE_MANEUVERABILITY_BONUS);
        } else {
            ship.getMutableStats().getMaxSpeed().unmodify("AEG_GurrenLCRHull_phase");
            ship.getMutableStats().getAcceleration().unmodify("AEG_GurrenLCRHull_phase");
            ship.getMutableStats().getDeceleration().unmodify("AEG_GurrenLCRHull_phase");
            ship.getMutableStats().getTurnAcceleration().unmodify("AEG_GurrenLCRHull_phase");
            ship.getMutableStats().getMaxTurnRate().unmodify("AEG_GurrenLCRHull_phase");
        }

        // Handle 0 CR explosion
        if (cr <= 0) {
            triggerExplosion(ship);
        }
    }

    private void applyBonuses(ShipAPI ship, float cr) {
        MutableShipStatsAPI stats = ship.getMutableStats();

        if (cr >= 0.7f) {
            // High CR bonuses
            stats.getArmorDamageTakenMult().modifyMult("AEG_GurrenLCRHull", ARMOR_DAMAGE_REDUCTION_HIGH);
            stats.getHullDamageTakenMult().modifyMult("AEG_GurrenLCRHull", HULL_DAMAGE_REDUCTION_HIGH);
            stats.getMaxSpeed().modifyMult("AEG_GurrenLCRHull", SPEED_BONUS_HIGH);
            stats.getAcceleration().modifyMult("AEG_GurrenLCRHull", MANEUVERABILITY_BONUS_HIGH);
            stats.getDeceleration().modifyMult("AEG_GurrenLCRHull", MANEUVERABILITY_BONUS_HIGH);
            stats.getTurnAcceleration().modifyMult("AEG_GurrenLCRHull", MANEUVERABILITY_BONUS_HIGH);
            stats.getMaxTurnRate().modifyMult("AEG_GurrenLCRHull", MANEUVERABILITY_BONUS_HIGH);
            stats.getEmpDamageTakenMult().modifyMult("AEG_GurrenLCRHull", EMP_RESISTANCE_HIGH);
            stats.getCriticalMalfunctionChance().modifyMult("AEG_GurrenLCRHull", CRITICAL_HIT_CHANCE_HIGH);
        } else if (cr >= 0.5f) {
            // Medium CR bonuses
            stats.getArmorDamageTakenMult().modifyMult("AEG_GurrenLCRHull", ARMOR_DAMAGE_REDUCTION_MEDIUM);
            stats.getHullDamageTakenMult().modifyMult("AEG_GurrenLCRHull", HULL_DAMAGE_REDUCTION_MEDIUM);
            stats.getMaxSpeed().modifyMult("AEG_GurrenLCRHull", SPEED_BONUS_MEDIUM);
            stats.getAcceleration().modifyMult("AEG_GurrenLCRHull", MANEUVERABILITY_BONUS_MEDIUM);
            stats.getDeceleration().modifyMult("AEG_GurrenLCRHull", MANEUVERABILITY_BONUS_MEDIUM);
            stats.getTurnAcceleration().modifyMult("AEG_GurrenLCRHull", MANEUVERABILITY_BONUS_MEDIUM);
            stats.getMaxTurnRate().modifyMult("AEG_GurrenLCRHull", MANEUVERABILITY_BONUS_MEDIUM);
            stats.getEmpDamageTakenMult().modifyMult("AEG_GurrenLCRHull", EMP_RESISTANCE_MEDIUM);
            stats.getCriticalMalfunctionChance().modifyMult("AEG_GurrenLCRHull", CRITICAL_HIT_CHANCE_MEDIUM);
        } else if (cr >= 0.3f) {
            // Low CR bonuses
            stats.getArmorDamageTakenMult().modifyMult("AEG_GurrenLCRHull", ARMOR_DAMAGE_REDUCTION_LOW);
            stats.getHullDamageTakenMult().modifyMult("AEG_GurrenLCRHull", HULL_DAMAGE_REDUCTION_LOW);
            stats.getMaxSpeed().modifyMult("AEG_GurrenLCRHull", SPEED_BONUS_LOW);
            stats.getAcceleration().modifyMult("AEG_GurrenLCRHull", MANEUVERABILITY_BONUS_LOW);
            stats.getDeceleration().modifyMult("AEG_GurrenLCRHull", MANEUVERABILITY_BONUS_LOW);
            stats.getTurnAcceleration().modifyMult("AEG_GurrenLCRHull", MANEUVERABILITY_BONUS_LOW);
            stats.getMaxTurnRate().modifyMult("AEG_GurrenLCRHull", MANEUVERABILITY_BONUS_LOW);
            stats.getEmpDamageTakenMult().modifyMult("AEG_GurrenLCRHull", EMP_RESISTANCE_LOW);
            stats.getCriticalMalfunctionChance().modifyMult("AEG_GurrenLCRHull", CRITICAL_HIT_CHANCE_LOW);
        }
    }

    private void triggerExplosion(ShipAPI ship) {
        Vector2f location = ship.getLocation();
        Global.getCombatEngine().spawnExplosion(location, new Vector2f(), EXPLOSION_CORE_COLOR, EXPLOSION_RADIUS, 1f);
        Global.getCombatEngine().spawnExplosion(location, new Vector2f(), EXPLOSION_FRINGE_COLOR, EXPLOSION_RADIUS * 1.5f, 1.5f);
        ship.setHitpoints(0); // Destroy the ship
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.setCRAtDeployment(1.0f); // Start combat with 100 CR
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getEmpDamageTakenMult().unmodify(id);
        stats.getCriticalMalfunctionChance().unmodify(id);
    }
}