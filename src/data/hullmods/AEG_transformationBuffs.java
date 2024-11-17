package data.hullmods;

import com.fs.starfarer.api.combat.ShipAPI;

public class AEG_transformationBuffs {

    public static void applyBuffs(ShipAPI ship, float powerGauge) {
        if (ship == null) return;

        if (powerGauge >= 0.1f) {
            increaseShieldUnfoldRate(ship);
        }
        if (powerGauge >= 0.2f) {
            increaseManeuverability(ship);
        }
        if (powerGauge >= 0.4f) {
            increaseFluxDissipation(ship);
        }
        if (powerGauge >= 0.6f) {
            increaseArmorEffectiveness(ship);
        }
        if (powerGauge >= 0.8f) {
            increaseWeaponStats(ship);
        }
        if (powerGauge >= 1.0f) {
            increaseEnergyDamage(ship);
            increaseEMPResistance(ship);
        }
    }

    private static void increaseShieldUnfoldRate(ShipAPI ship) {
        // Logic to increase shield unfold rate
        ship.getMutableStats().getShieldUnfoldRateMult().modifyMult("super_saiyan", 1.2f);
    }

    private static void increaseManeuverability(ShipAPI ship) {
        // Logic to increase maneuverability
        ship.getMutableStats().getAcceleration().modifyMult("super_saiyan", 1.2f);
        ship.getMutableStats().getDeceleration().modifyMult("super_saiyan", 1.2f);
        ship.getMutableStats().getTurnAcceleration().modifyMult("super_saiyan", 1.2f);
        ship.getMutableStats().getMaxTurnRate().modifyMult("super_saiyan", 1.2f);
    }

    private static void increaseFluxDissipation(ShipAPI ship) {
        // Logic to increase flux dissipation rate
        ship.getMutableStats().getFluxDissipation().modifyMult("super_saiyan", 1.2f);
    }

    private static void increaseArmorEffectiveness(ShipAPI ship) {
        // Logic to increase effective armor reduction
        ship.getMutableStats().getArmorDamageTakenMult().modifyMult("super_saiyan", 0.8f); // Reduces armor damage taken by 20%
    }

    private static void increaseWeaponStats(ShipAPI ship) {
        // Logic to increase weapon reload, cooldown, and rate of fire
        ship.getMutableStats().getBallisticRoFMult().modifyMult("super_saiyan", 1.2f);
        ship.getMutableStats().getEnergyRoFMult().modifyMult("super_saiyan", 1.2f);
        ship.getMutableStats().getMissileRoFMult().modifyMult("super_saiyan", 1.2f);
    }

    private static void increaseEnergyDamage(ShipAPI ship) {
        // Logic to increase energy damage
        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("super_saiyan", 1.5f);
    }

    private static void increaseEMPResistance(ShipAPI ship) {
        // Logic to increase EMP resistance
        ship.getMutableStats().getEmpDamageTakenMult().modifyMult("super_saiyan", 0.5f); // Reduces EMP damage taken by 50%
    }
}