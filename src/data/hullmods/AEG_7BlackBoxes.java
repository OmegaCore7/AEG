package data.hullmods;

package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.IntervalUtil;

public class AEG_7BlackBoxes extends BaseHullMod {

    private static final float REGENERATION_RATE = 0.05f;
    private static final float ASSIMILATION_DAMAGE_CONVERSION = 0.1f;
    private static final float STRENGTHENING_MULT = 1.1f;
    private static final float PREDICTION_EVASION_CHANCE = 0.2f;
    private static final float SENSOR_RANGE_BOOST = 200f;
    private static final float ADAPTIVE_DEFENSE_DURATION = 20f;
    private static final float ADAPTIVE_DEFENSE_REDUCTION = 0.5f;

    private final IntervalUtil adaptiveDefenseTimer = new IntervalUtil(ADAPTIVE_DEFENSE_DURATION, ADAPTIVE_DEFENSE_DURATION);

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null) return;

        // Regeneration
        if (ship.getHullLevel() < 1.0f) {
            ship.getMutableStats().getHullRepairRatePercentPerSecond().modifyFlat("MazingerZeroHullMod", REGENERATION_RATE);
            ship.getMutableStats().getArmorRepairRatePercentPerSecond().modifyFlat("MazingerZeroHullMod", REGENERATION_RATE);
        }

        // Assimilation
        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("MazingerZeroHullMod", 1 + ASSIMILATION_DAMAGE_CONVERSION);
        ship.getMutableStats().getHardFluxDissipationFraction().modifyFlat("MazingerZeroHullMod", ASSIMILATION_DAMAGE_CONVERSION);

        // Strengthening
        ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult("MazingerZeroHullMod", STRENGTHENING_MULT);
        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("MazingerZeroHullMod", STRENGTHENING_MULT);
        ship.getMutableStats().getMissileWeaponDamageMult().modifyMult("MazingerZeroHullMod", STRENGTHENING_MULT);
        ship.getMutableStats().getMaxSpeed().modifyMult("MazingerZeroHullMod", STRENGTHENING_MULT);
        ship.getMutableStats().getAcceleration().modifyMult("MazingerZeroHullMod", STRENGTHENING_MULT);
        ship.getMutableStats().getDeceleration().modifyMult("MazingerZeroHullMod", STRENGTHENING_MULT);
        ship.getMutableStats().getTurnAcceleration().modifyMult("MazingerZeroHullMod", STRENGTHENING_MULT);
        ship.getMutableStats().getMaxTurnRate().modifyMult("MazingerZeroHullMod", STRENGTHENING_MULT);

        // Dimensional Prediction
        if (Math.random() < PREDICTION_EVASION_CHANCE) {
            ship.getMutableStats().getHullDamageTakenMult().modifyMult("MazingerZeroHullMod", 0f);
            ship.getMutableStats().getArmorDamageTakenMult().modifyMult("MazingerZeroHullMod", 0f);
        }
        ship.getMutableStats().getSensorStrength().modifyFlat("MazingerZeroHullMod", SENSOR_RANGE_BOOST);

        // Adaptive Defense
        adaptiveDefenseTimer.advance(amount);
        if (adaptiveDefenseTimer.intervalElapsed()) {
            ship.getMutableStats().getHullDamageTakenMult().modifyMult("MazingerZeroHullMod", ADAPTIVE_DEFENSE_REDUCTION);
            ship.getMutableStats().getArmorDamageTakenMult().modifyMult("MazingerZeroHullMod", ADAPTIVE_DEFENSE_REDUCTION);
        }

        // Last Stand Protocol
        if (ship.getHullLevel() <= 0.05f) {
            // Find the attacking ship and destroy it
            ShipAPI attacker = findAttackingShip(ship);
            if (attacker != null) {
                attacker.getHull().setHitpoints(0);
                ship.setHitpoints(ship.getMaxHitpoints() * 0.25f);
            }
        }
    }

    private ShipAPI findAttackingShip(ShipAPI ship) {
        // Implement logic to find the ship that last attacked this ship
        // This is a placeholder and needs to be replaced with actual logic
        return null;
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // Apply any effects that need to be set before the ship is created
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        // Apply any effects that need to be set after the ship is created
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        switch (index) {
            case 0: return "rapid hull and armor regeneration";
            case 1: return "converts incoming damage into a damage boost and converts hard flux to soft flux";
            case 2: return "increases all ship attributes";
            case 3: return "chance to evade incoming attacks and increases sensor range";
            case 4: return "boosts damage reduction against a specific weapon type for 20 seconds after being hit by it";
            case 5: return "one-time ability to destroy the attacking ship and recover 25% hull when the ship would be destroyed";
            default: return null;
        }
    }
}
