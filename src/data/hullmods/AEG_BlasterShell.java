package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

public class AEG_BlasterShell extends BaseHullMod {
    private static final float DAMAGE_MULT = 1.2f;
    private static final float RANGE_BONUS = 20f;
    private static final Color SHIELD_COLOR = new Color(100, 255, 100, 225);
    private static final Color DEFAULT_SHIELD_COLOR = Color.WHITE;
    private boolean isActive = false;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) return;

        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (isSpecificWeaponSlot(weapon) && weapon.isFiring()) {
                if (!isActive) {
                    activateHullMod(ship);
                    isActive = true;
                }
            }
        }

        if (!isAnySpecificWeaponFiring(ship)) {
            isActive = false;
        }
    }

    private boolean isSpecificWeaponSlot(WeaponAPI weapon) {
        // Add your logic to check if the weapon is in a specific slot
        return true;
    }

    private boolean isAnySpecificWeaponFiring(ShipAPI ship) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (isSpecificWeaponSlot(weapon) && weapon.isFiring()) {
                return true;
            }
        }
        return false;
    }

    private void activateHullMod(final ShipAPI ship) {
        if (ship.getShield() == null || !ship.getShield().isOn()) {
            ship.getMutableStats().getShieldDamageTakenMult().modifyMult("AEG_BlasterShell_shield", 0.1f);
            ship.getMutableStats().getShieldUpkeepMult().modifyMult("AEG_BlasterShell_shield", 0f);
            ship.setJitterUnder(this, SHIELD_COLOR, 1f, 10, 5f, 10f);
        } else {
            ship.getShield().toggleOn();
            ship.getShield().setRingColor(SHIELD_COLOR);
            ship.getShield().setInnerColor(SHIELD_COLOR);
        }

        ship.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent("AEG_BlasterShell_range", RANGE_BONUS);
        ship.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent("AEG_BlasterShell_range", RANGE_BONUS);
        ship.getMutableStats().getMissileWeaponRangeBonus().modifyPercent("AEG_BlasterShell_range", RANGE_BONUS);
        ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult("AEG_BlasterShell_damage", DAMAGE_MULT);
        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("AEG_BlasterShell_damage", DAMAGE_MULT);
        ship.getMutableStats().getMissileWeaponDamageMult().modifyMult("AEG_BlasterShell_damage", DAMAGE_MULT);

        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            private float elapsed = 0f;
            private static final float EFFECT_DURATION = 5f;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (Global.getCombatEngine().isPaused()) return;
                elapsed += amount;
                if (elapsed >= EFFECT_DURATION) {
                    ship.getMutableStats().getShieldDamageTakenMult().unmodify("AEG_BlasterShell_shield");
                    ship.getMutableStats().getShieldUpkeepMult().unmodify("AEG_BlasterShell_shield");
                    ship.getMutableStats().getBallisticWeaponRangeBonus().unmodify("AEG_BlasterShell_range");
                    ship.getMutableStats().getEnergyWeaponRangeBonus().unmodify("AEG_BlasterShell_range");
                    ship.getMutableStats().getMissileWeaponRangeBonus().unmodify("AEG_BlasterShell_range");
                    ship.getMutableStats().getBallisticWeaponDamageMult().unmodify("AEG_BlasterShell_damage");
                    ship.getMutableStats().getEnergyWeaponDamageMult().unmodify("AEG_BlasterShell_damage");
                    ship.getMutableStats().getMissileWeaponDamageMult().unmodify("AEG_BlasterShell_damage");

                    if (ship.getShield() != null) {
                        ship.getShield().setRingColor(DEFAULT_SHIELD_COLOR);
                        ship.getShield().setInnerColor(DEFAULT_SHIELD_COLOR);
                    }

                    Global.getCombatEngine().removePlugin(this);
                }
            }
        });
    }
}
