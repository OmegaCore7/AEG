package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class AEG_PowerRegulationMatrix extends BaseHullMod {

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // Campaign-related boosts
        stats.getFuelUseMod().modifyMult(id, 0.8f); // Reduces fuel consumption by 20%
        stats.getSuppliesPerMonth().modifyMult(id, 0.85f); // Reduces supply consumption by 15%
        stats.getSensorStrength().modifyMult(id, 1.25f); // Increases sensor range by 25%
        stats.getDynamic().getMod(Stats.SURVEY_COST_REDUCTION).modifyMult(id, 0.7f); // Reduces supply cost for surveying by 30%
        stats.getDynamic().getMod(Stats.SALVAGE_VALUE_MULT_MOD).modifyMult(id, 1.2f); // Increases salvage yield by 20%
        stats.getMaxBurnLevel().modifyFlat(id, 2f); // Increases fleet travel speed by 1 burn level
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        // Additional effects can be applied here if needed
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        // Combat-related effects can be applied here if needed
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "Reduces fuel consumption by 20%";
        if (index == 1) return "Reduces supply consumption by 15%";
        if (index == 2) return "Increases sensor range by 25%";
        if (index == 3) return "Reduces supply cost for surveying by 30%";
        if (index == 4) return "Increases salvage yield by 20%";
        if (index == 5) return "Increases fleet travel speed by 1 burn level";
        return null;
    }
}