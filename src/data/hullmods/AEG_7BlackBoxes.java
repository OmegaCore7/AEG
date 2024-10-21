package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class AEG_7BlackBoxes extends BaseHullMod {

    private static final float REGENERATION_RATE = 0.01f;
    private static final float ASSIMILATION_DAMAGE_CONVERSION = 0.05f;
    private static final float STRENGTHENING_MULT = 1.05f;
    private static final float PREDICTION_EVASION_CHANCE = 0.1f;
    private static final float SENSOR_RANGE_BOOST = 200f;
    private static final float ADAPTIVE_DEFENSE_DURATION = 10f; // Reduced from 20f
    private static final float ADAPTIVE_DEFENSE_REDUCTION = 0.1f; // Reduced from 0.75f
    private static final float ADAPTIVE_DEFENSE_COOLDOWN = 5f; // Added cooldown
    private static final float DAMAGE_REDUCTION_DURATION = 5f; // Duration for reduced damage effect

    private final IntervalUtil adaptiveDefenseTimer = new IntervalUtil(ADAPTIVE_DEFENSE_DURATION, ADAPTIVE_DEFENSE_DURATION + ADAPTIVE_DEFENSE_COOLDOWN);
    private final IntervalUtil damageReductionTimer = new IntervalUtil(DAMAGE_REDUCTION_DURATION, DAMAGE_REDUCTION_DURATION);
    private boolean lastStandTriggered = false; // Flag to track if Last Stand Protocol has been triggered
    private boolean damageReductionActive = false; // Flag to track if damage reduction is active

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null) return;

        // Continuous Regeneration
        if (ship.getHullLevel() < 1.0f) {
            float newHitpoints = ship.getHitpoints() + (REGENERATION_RATE * ship.getMaxHitpoints() * amount);
            ship.setHitpoints(Math.min(newHitpoints, ship.getMaxHitpoints()));
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
        } else {
            ship.getMutableStats().getHullDamageTakenMult().unmodify("AEG_7BlackBoxes");
            ship.getMutableStats().getArmorDamageTakenMult().unmodify("AEG_7BlackBoxes");
        }

        // Last Stand Protocol
        if (!lastStandTriggered && (ship.getHullLevel() <= 0.05f || ship.getHitpoints() - ship.getMaxHitpoints() * 0.05f <= 4000)) {
            Global.getLogger(this.getClass()).info("Last Stand Protocol triggered");
            ShipAPI attacker = findAttackingShip(ship);
            if (attacker != null) {
                Global.getLogger(this.getClass()).info("Attacker found: " + attacker.getName());
                dealFatalDamage(attacker);
                ship.setHitpoints(ship.getMaxHitpoints() * 0.25f);
                lastStandTriggered = true; // Set the flag to true after triggering
                damageReductionActive = true; // Activate damage reduction
                damageReductionTimer.advance(0); // Reset the timer
            } else {
                Global.getLogger(this.getClass()).info("No attacker found");
            }
        }

        // Damage Reduction
        if (damageReductionActive) {
            damageReductionTimer.advance(amount);
            if (damageReductionTimer.intervalElapsed()) {
                damageReductionActive = false; // Deactivate damage reduction after the duration
            } else {
                ship.getMutableStats().getHullDamageTakenMult().modifyFlat("AEG_7BlackBoxes_DamageReduction", 0.01f);
                ship.getMutableStats().getArmorDamageTakenMult().modifyFlat("AEG_7BlackBoxes_DamageReduction", 0.01f);
            }
        } else {
            ship.getMutableStats().getHullDamageTakenMult().unmodify("AEG_7BlackBoxes_DamageReduction");
            ship.getMutableStats().getArmorDamageTakenMult().unmodify("AEG_7BlackBoxes_DamageReduction");
        }
    }

    private void dealFatalDamage(ShipAPI attacker) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null) {
            Vector2f location = attacker.getLocation();
            engine.applyDamage(attacker, location, attacker.getMaxHitpoints(), DamageType.HIGH_EXPLOSIVE, 0f, true, false, null);
        }
    }

    private ShipAPI findAttackingShip(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return null;

        for (ShipAPI enemy : engine.getShips()) {
            if (enemy.getOwner() != ship.getOwner() && enemy.isAlive() && !enemy.isFighter()) {
                // Additional logic to determine if this enemy is the one that last attacked the ship
                // This is a placeholder and needs to be replaced with actual logic
                return enemy;
            }
        }
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
                return "Black Box 1: Continuous hull regeneration.";
            case 1:
                return "Black Box 2: Converts incoming damage into a damage boost and converts hard flux to soft flux.";
            case 2:
                return "Black Box 3: Increases all ship attributes.";
            case 3:
                return "Black Box 4: Chance to evade incoming attacks and increases sensor range.";
            case 4:
                return "Black Box 5: Boosts damage reduction against a specific weapon type for 10 seconds after being hit by it, with a 5-second cooldown.";
            case 5:
                return "Black Box 6: One-time ability to destroy the attacking ship and recover 25% hull when the ship would be destroyed.";
            default:
                return null;
        }
    }
}
