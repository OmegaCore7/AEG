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
        if (stats == null) return;

        stats.getHullBonus().modifyPercent(id, HULL_BONUS);
        stats.getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat(id, 1000f);
        stats.getBreakProb().modifyMult(id, 0f);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) return;

        // Initialize charges if not already done
        if (!ship.getCustomData().containsKey(CHARGE_KEY)) {
            int charges = ship.getVariant().getSMods().contains("AEG_tenacity") ? 2 : 1;
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
            float[][] armorGrid = ship.getArmorGrid().getGrid();
            float maxArmor = ship.getArmorGrid().getMaxArmorInCell();
            for (int i = 0; i < armorGrid.length; i++) {
                for (int j = 0; j < armorGrid[i].length; j++) {
                    armorGrid[i][j] = Math.min(armorGrid[i][j] + armorRegen, maxArmor);
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
        if (ship == null) return false;

        float[][] armorGrid = ship.getArmorGrid().getGrid();
        float maxArmor = ship.getArmorGrid().getMaxArmorInCell();
        for (float[] row : armorGrid) {
            for (float armor : row) {
                if (armor < maxArmor) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        switch (index) {
            case 0: return "" + (int) HULL_BONUS + "%";
            case 1: return "" + HULL_REGEN_RATE + " HP/s";
            case 2: return "" + ARMOR_REGEN_RATE + " armor/s";
            case 3: return "45%";
            case 4: return "1 charge";
            case 5: return "2 charges if S-modded";
            default: return null;
        }
    }
}
