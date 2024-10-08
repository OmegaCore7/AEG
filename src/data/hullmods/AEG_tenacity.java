package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class AEG_tenacity extends BaseHullMod {

    public static final float HULL_BONUS = 40f;
    public static final float HULL_REGEN_RATE = 1.0f; // Hull regeneration rate per second (doubled)
    public static final float ARMOR_REGEN_RATE = 0.4f; // Armor regeneration rate per second (doubled)
    public static final float REGEN_THRESHOLD = 0.45f; // 45% HP threshold
    private static final String CHARGE_KEY = "TENACITYCharge";

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getHullBonus().modifyPercent(id, HULL_BONUS);
        stats.getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat(id, 1000f);
        stats.getBreakProb().modifyMult(id, 0f);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) return;

        // Initialize charges if not already done
        if (!ship.getCustomData().containsKey(CHARGE_KEY)) {
            int charges = ship.getVariant().hasHullMod("safetyoverrides") ? 2 : 1;
            ship.setCustomData(CHARGE_KEY, charges);
        }

        int charges = (int) ship.getCustomData().get(CHARGE_KEY);

        // Check if HP is below the threshold and there are charges left
        if (ship.getHitpoints() / ship.getMaxHitpoints() < REGEN_THRESHOLD && charges > 0) {
            // Regenerate hull
            float hullRegen = HULL_REGEN_RATE * amount;
            ship.setHitpoints(Math.min(ship.getHitpoints() + hullRegen, ship.getMaxHitpoints()));

            // Regenerate armor
            float armorRegen = ARMOR_REGEN_RATE * amount;
            for (int i = 0; i < ship.getArmorGrid().getGrid().length; i++) {
                for (int j = 0; j < ship.getArmorGrid().getGrid()[i].length; j++) {
                    float currentArmor = ship.getArmorGrid().getGrid()[i][j];
                    float maxArmor = ship.getArmorGrid().getMaxArmorInCell();
                    ship.getArmorGrid().getGrid()[i][j] = Math.min(currentArmor + armorRegen, maxArmor);
                }
            }

            // Decrease charge if fully healed
            if (ship.getHitpoints() == ship.getMaxHitpoints() && isArmorFullyRepaired(ship)) {
                charges--;
                ship.setCustomData(CHARGE_KEY, charges);
            }
        }
    }

    private boolean isArmorFullyRepaired(ShipAPI ship) {
        for (float[] row : ship.getArmorGrid().getGrid()) {
            for (float armor : row) {
                if (armor < ship.getArmorGrid().getMaxArmorInCell()) {
                    return false;
                }
            }
        }
        return true;
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        // No need to unapply anything specific for this hull mod
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int) HULL_BONUS + "%";
        if (index == 1) return "" + HULL_REGEN_RATE + " HP/s";
        if (index == 2) return "" + ARMOR_REGEN_RATE + " armor/s";
        if (index == 3) return "45%";
        if (index == 4) return "1 charge";
        if (index == 5) return "2 charges if S-modded";
        return null;
    }
}
