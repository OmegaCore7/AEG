package data.weapons.onfire;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

public class AEG_BlasterShell implements OnFireEffectPlugin {
    private static final float DAMAGE_MULT = 1.2f; // Increase damage by 20%
    private static final float RANGE_BONUS = 20f; // Increase range by 20%
    private static final Color SHIELD_COLOR = new Color(100, 220, 100, 225); // Green color with some transparency
    private static final Color DEFAULT_SHIELD_COLOR = Color.WHITE; // Default shield color

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        final ShipAPI ship = weapon.getShip();
        if (ship == null) return;

        // Apply shield effects
        if (ship.getShield() == null || !ship.getShield().isOn()) {
            // Temporarily modify stats to simulate a shield
            ship.getMutableStats().getShieldDamageTakenMult().modifyMult("AEG_BlasterShell_shield", 0.1f); // Simulate shield absorbing 90% damage
            ship.getMutableStats().getShieldUpkeepMult().modifyMult("AEG_BlasterShell_shield", 0f); // No shield upkeep cost
            ship.setJitterUnder(this, SHIELD_COLOR, 1f, 10, 5f, 10f); // Visual effect
        } else {
            ship.getShield().toggleOn();
            ship.getShield().setRingColor(SHIELD_COLOR);
            ship.getShield().setInnerColor(SHIELD_COLOR);
        }

        // Apply weapon range and damage buffs
        ship.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent("AEG_BlasterShell_range", RANGE_BONUS);
        ship.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent("AEG_BlasterShell_range", RANGE_BONUS);
        ship.getMutableStats().getMissileWeaponRangeBonus().modifyPercent("AEG_BlasterShell_range", RANGE_BONUS);
        ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult("AEG_BlasterShell_damage", DAMAGE_MULT);
        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("AEG_BlasterShell_damage", DAMAGE_MULT);
        ship.getMutableStats().getMissileWeaponDamageMult().modifyMult("AEG_BlasterShell_damage", DAMAGE_MULT);

        // Schedule the removal of the effects after a short duration
        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            private float elapsed = 0f;
            private static final float EFFECT_DURATION = 5f; // Duration of the effects in seconds

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
                        ship.getShield().setRingColor(DEFAULT_SHIELD_COLOR); // Reset to default color
                        ship.getShield().setInnerColor(DEFAULT_SHIELD_COLOR); // Reset to default color
                    }

                    Global.getCombatEngine().removePlugin(this);
                }
            }
        });
    }
}
