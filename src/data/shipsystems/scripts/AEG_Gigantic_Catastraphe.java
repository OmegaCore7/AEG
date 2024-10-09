package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;

public class AEG_Gigantic_Catastraphe extends BaseShipSystemScript {

    public static float DAMAGE_MULT = 1.2f; // Increase damage by 20%
    private static final Color SHIELD_COLOR = new Color(100, 220, 100, 225); // Green color with some transparency

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        // Plasma Jets effect
        if (state == State.OUT) {
            stats.getMaxSpeed().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
        } else {
            stats.getMaxSpeed().modifyFlat(id, 50f);
            stats.getAcceleration().modifyPercent(id, 200f * effectLevel);
            stats.getDeceleration().modifyPercent(id, 200f * effectLevel);
            stats.getTurnAcceleration().modifyFlat(id, 30f * effectLevel);
            stats.getTurnAcceleration().modifyPercent(id, 200f * effectLevel);
            stats.getMaxTurnRate().modifyFlat(id, 15f);
            stats.getMaxTurnRate().modifyPercent(id, 100f);
        }

        // Blaster Shell effect
        if (state == State.ACTIVE) {
            stats.getShieldDamageTakenMult().modifyMult(id, 1f - DAMAGE_MULT * effectLevel);
            stats.getShieldUpkeepMult().modifyMult(id, 0f);

            // Activate shield and change color
            if (ship.getShield() != null && !ship.getShield().isOn()) {
                ship.getShield().toggleOn();
                ship.getShield().setRingColor(SHIELD_COLOR);
                ship.getShield().setInnerColor(SHIELD_COLOR);
            }

            // Apply range, turn rate, and damage buffs
            stats.getBallisticWeaponRangeBonus().modifyPercent(id, 20f * effectLevel);
            stats.getEnergyWeaponRangeBonus().modifyPercent(id, 20f * effectLevel);
            stats.getMissileWeaponRangeBonus().modifyPercent(id, 20f * effectLevel);
            stats.getBallisticWeaponDamageMult().modifyMult(id, DAMAGE_MULT);
            stats.getEnergyWeaponDamageMult().modifyMult(id, DAMAGE_MULT);
            stats.getMissileWeaponDamageMult().modifyMult(id, DAMAGE_MULT);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship != null && ship.getShield() != null) {
            ship.getShield().toggleOff();
            ship.getShield().setRingColor(null); // Reset to default color
            ship.getShield().setInnerColor(null); // Reset to default color
        }

        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getShieldDamageTakenMult().unmodify(id);
        stats.getShieldUpkeepMult().unmodify(id);
        stats.getBallisticWeaponRangeBonus().unmodify(id);
        stats.getEnergyWeaponRangeBonus().unmodify(id);
        stats.getMissileWeaponRangeBonus().unmodify(id);
        stats.getBallisticWeaponDamageMult().unmodify(id);
        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getMissileWeaponDamageMult().unmodify(id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("improved maneuverability", false);
        } else if (index == 1) {
            return new StatusData("+50 top speed", false);
        } else if (index == 2) {
            return new StatusData("shield absorbs 10x damage", false);
        } else if (index == 3) {
            return new StatusData("increased weapon range and damage", false);
        }
        return null;
    }
}
