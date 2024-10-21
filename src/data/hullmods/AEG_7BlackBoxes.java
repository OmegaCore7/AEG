package data.hullmods;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
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
    private boolean lastStandTriggered = false; // Flag to track if Last Stand Protocol has been triggered

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null) return;

        // Regeneration
        if (ship.getHullLevel() < 1.0f) {
            ship.getMutableStats().getHullCombatRepairRatePercentPerSecond().modifyFlat("AEG_7BlackBoxes", REGENERATION_RATE);
            restoreArmor(ship, amount);
        }

        // Assimilation
        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("AEG_7BlackBoxes", 1 + ASSIMILATION_DAMAGE_CONVERSION);
        ship.getMutableStats().getHardFluxDissipationFraction().modifyFlat("AEG_7BlackBoxes", ASSIMILATION_DAMAGE_CONVERSION);

        // Strengthening
        ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);
        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);
        ship.getMutableStats().getMissileWeaponDamageMult().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);
        ship.getMutableStats().getMaxSpeed().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);
        ship.getMutableStats().getAcceleration().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);
        ship.getMutableStats().getDeceleration().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);
        ship.getMutableStats().getTurnAcceleration().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);
        ship.getMutableStats().getMaxTurnRate().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);

        // Dimensional Prediction
        if (Math.random() < PREDICTION_EVASION_CHANCE) {
            ship.getMutableStats().getHullDamageTakenMult().modifyMult("AEG_7BlackBoxes", 0f);
            ship.getMutableStats().getArmorDamageTakenMult().modifyMult("AEG_7BlackBoxes", 0f);
        }
        ship.getMutableStats().getSensorStrength().modifyFlat("AEG_7BlackBoxes", SENSOR_RANGE_BOOST);

        // Adaptive Defense
        adaptiveDefenseTimer.advance(amount);
        if (adaptiveDefenseTimer.intervalElapsed()) {
            ship.getMutableStats().getHullDamageTakenMult().modifyMult("AEG_7BlackBoxes", ADAPTIVE_DEFENSE_REDUCTION);
            ship.getMutableStats().getArmorDamageTakenMult().modifyMult("AEG_7BlackBoxes", ADAPTIVE_DEFENSE_REDUCTION);
        }

        // Last Stand Protocol
        if (!lastStandTriggered && ship.getHullLevel() <= 0.05f) {
            // Find the attacking ship and destroy it
            ShipAPI attacker = findAttackingShip(ship);
            if (attacker != null) {
                attacker.setHitpoints(0);
                ship.setHitpoints(ship.getMaxHitpoints() * 0.25f);
                lastStandTriggered = true; // Set the flag to true after triggering
            }
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
                    armor[x][y] = Math.min(currentArmor + (REGENERATION_RATE * maxArmor * amount), maxArmor);
                }
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
            case 0:
                return "Regeneration: Rapid hull and armor regeneration";
            case 1:
                return "Assimilation: Converts incoming damage into a damage boost and converts hard flux to soft flux";
            case 2:
                return "Strengthening: Increases all ship attributes";
            case 3:
                return "Dimensional Prediction: Chance to evade incoming attacks and increases sensor range";
            case 4:
                return "Adaptive Defense: Boosts damage reduction against a specific weapon type for 20 seconds after being hit by it";
            case 5:
                return "Causality Weapon: One-time ability to destroy the attacking ship and recover 25% hull when the ship would be destroyed";
            default:
                return null;
        }
    }
}
