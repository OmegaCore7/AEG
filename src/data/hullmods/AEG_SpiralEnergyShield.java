package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class AEG_SpiralEnergyShield extends BaseHullMod {

    private static final float MAX_DAMAGE_REDUCTION = 0.5f; // 50% damage reduction at max flux
    private static final float MAX_DAMAGE_BOOST = 0.5f; // 50% damage boost at zero flux
    private static final float MAX_ROF_BOOST = 0.5f; // 50% rate of fire boost at zero flux
    private static final float MAX_RELOAD_BOOST = 0.5f; // 50% reload speed boost at zero flux

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || ship.isHulk() || Global.getCombatEngine().isPaused()) {
            return;
        }

        float fluxLevel = ship.getFluxTracker().getFluxLevel(); // Get the current flux level (0 to 1)
        float damageReduction = fluxLevel * MAX_DAMAGE_REDUCTION;
        float damageBoost = (1 - fluxLevel) * MAX_DAMAGE_BOOST;
        float rofBoost = (1 - fluxLevel) * MAX_ROF_BOOST;
        float reloadBoost = (1 - fluxLevel) * MAX_RELOAD_BOOST;

        MutableShipStatsAPI stats = ship.getMutableStats();

        // Apply damage reduction based on flux level
        stats.getHullDamageTakenMult().modifyMult("AEG_SpiralEnergyShield", 1 - damageReduction);
        stats.getArmorDamageTakenMult().modifyMult("AEG_SpiralEnergyShield", 1 - damageReduction);
        stats.getShieldDamageTakenMult().modifyMult("AEG_SpiralEnergyShield", 1 - damageReduction);

        // Apply damage boost based on flux level
        stats.getBallisticWeaponDamageMult().modifyMult("AEG_SpiralEnergyShield", 1 + damageBoost);
        stats.getEnergyWeaponDamageMult().modifyMult("AEG_SpiralEnergyShield", 1 + damageBoost);
        stats.getMissileWeaponDamageMult().modifyMult("AEG_SpiralEnergyShield", 1 + damageBoost);

        // Apply rate of fire boost based on flux level
        stats.getBallisticRoFMult().modifyMult("AEG_SpiralEnergyShield", 1 + rofBoost);
        stats.getEnergyRoFMult().modifyMult("AEG_SpiralEnergyShield", 1 + rofBoost);
        stats.getMissileRoFMult().modifyMult("AEG_SpiralEnergyShield", 1 + rofBoost);

        // Apply reload speed boost based on flux level
        stats.getBallisticAmmoRegenMult().modifyMult("AEG_SpiralEnergyShield", 1 + reloadBoost);
        stats.getEnergyAmmoRegenMult().modifyMult("AEG_SpiralEnergyShield", 1 + reloadBoost);
        stats.getMissileAmmoRegenMult().modifyMult("AEG_SpiralEnergyShield", 1 + reloadBoost);
    }
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getShieldDamageTakenMult().unmodify(id);
        stats.getBallisticWeaponDamageMult().unmodify(id);
        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getMissileWeaponDamageMult().unmodify(id);
        stats.getBallisticRoFMult().unmodify(id);
        stats.getEnergyRoFMult().unmodify(id);
        stats.getMissileRoFMult().unmodify(id);
        stats.getBallisticAmmoRegenMult().unmodify(id);
        stats.getEnergyAmmoRegenMult().unmodify(id);
        stats.getMissileAmmoRegenMult().unmodify(id);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int) (MAX_DAMAGE_REDUCTION * 100) + "%";
        if (index == 1) return "" + (int) (MAX_DAMAGE_BOOST * 100) + "%";
        if (index == 2) return "" + (int) (MAX_ROF_BOOST * 100) + "%";
        if (index == 3) return "" + (int) (MAX_RELOAD_BOOST * 100) + "%";
        return null;
    }
}