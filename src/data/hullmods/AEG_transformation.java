package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.AEG_transformationFX;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicUI;

import java.awt.Color;
import java.util.List;

public class AEG_transformation extends BaseHullMod {

    private static final float GAUGE_MAX = 1.0f; // Max gauge (100%)
    private static final float[] BOOST_THRESHOLDS = {0.5f, 0.75f, 1.0f};
    private static final float TRANSFORMATION_DURATION = 60f; // Duration of the transformation in seconds

    private float powerGauge = 0f; // Current gauge value
    private AEG_transformationFX particleEffect;

    public void applyEffectsBeforeShipCreation(HullModSpecAPI spec, ShipAPI ship, List ships) {
        if (ship == null) return;
        powerGauge = 0f; // Initialize gauge when applied
        particleEffect = new AEG_transformationFX();
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null) return;

        // Increase the power gauge by 1% per second
        powerGauge = Math.min(powerGauge + (0.01f * amount), GAUGE_MAX);
        applyBuffs(ship);

        // Update particles based on charge level
        particleEffect.updateParticles(ship, powerGauge);

        // Check if the power gauge is at maximum
        if (powerGauge >= GAUGE_MAX) {
            // Trigger the transformation
            triggerTransformation(ship);
        }

        // Update the HUD with the transformation bar
        updateHUD(ship);
    }

    private void applyBuffs(ShipAPI ship) {
        if (ship == null) return;

        for (float threshold : BOOST_THRESHOLDS) {
            if (powerGauge >= threshold) {
                increaseArmorEffectiveness(ship);
                increaseWeaponStats(ship);
                increaseEnergyDamage(ship);
                if (threshold == 1.0f) {
                    increaseEMPResistance(ship); // Apply EMP resistance at 100% gauge
                }
                break; // Exit the loop once the buffs are applied
            }
        }
    }

    private void increaseArmorEffectiveness(ShipAPI ship) {
        if (ship == null) return;
        // Logic to increase effective armor reduction
    }

    private void increaseWeaponStats(ShipAPI ship) {
        if (ship == null) return;
        // Logic to increase weapon reload, cooldown, and rate of fire
    }

    private void increaseEnergyDamage(ShipAPI ship) {
        if (ship == null) return;
        // Logic to increase energy damage
    }

    private void increaseEMPResistance(ShipAPI ship) {
        if (ship == null) return;
        // Logic to increase EMP resistance
        ship.getMutableStats().getEmpDamageTakenMult().modifyMult("super_saiyan", 0.5f); // Reduces EMP damage taken by 50%
    }

    private void triggerTransformation(final ShipAPI ship) {
        if (ship == null) return;

        // Apply transformation effects
        ship.getMutableStats().getMaxSpeed().modifyFlat("super_saiyan", 100f);
        ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult("super_saiyan", 2f);
        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("super_saiyan", 2f);
        ship.getMutableStats().getMissileWeaponDamageMult().modifyMult("super_saiyan", 2f);
        ship.getMutableStats().getShieldUpkeepMult().modifyMult("super_saiyan", 0.5f); // Reduce shield upkeep by 50%

        // Create transformation particle effect
        particleEffect.createTransformationEffect(ship);

        // Sync hair deco weapon rotation and animation
        syncHairDecoWeapon(ship);

        // Set a timer to revert the transformation after the duration
        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            private float timer = TRANSFORMATION_DURATION;

            public void advance(float amount, List events) {
                timer -= amount;
                if (timer <= 0) {
                    revertTransformation(ship);
                    Global.getCombatEngine().removePlugin(this);
                }
            }
        });
    }

    private void syncHairDecoWeapon(ShipAPI ship) {
        if (ship == null) return;

        WeaponAPI headWeapon = null;
        WeaponAPI hairWeapon = null;

        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if ("WS0011".equals(weapon.getSlot().getId())) {
                headWeapon = weapon;
            } else if ("WS0012".equals(weapon.getSlot().getId())) {
                hairWeapon = weapon;
            }
        }

        if (headWeapon != null && hairWeapon != null) {
            hairWeapon.setCurrAngle(headWeapon.getCurrAngle());
            hairWeapon.getAnimation().play();
            hairWeapon.getAnimation().setFrame(0);
            hairWeapon.getAnimation().setFrameRate(12f); // Play frames 00-12

            final WeaponAPI finalHeadWeapon = headWeapon;
            final WeaponAPI finalHairWeapon = hairWeapon;
            Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                private boolean loopStarted = false;

                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    finalHairWeapon.setCurrAngle(finalHeadWeapon.getCurrAngle());
                    if (finalHairWeapon.getAnimation().getFrame() >= 12 && !loopStarted) {
                        finalHairWeapon.getAnimation().setFrame(6);
                        finalHairWeapon.getAnimation().setFrameRate(12f / (12 - 6)); // Loop frames 6-12
                        loopStarted = true;
                    }
                }
            });
        }
    }

    private void revertTransformation(ShipAPI ship) {
        if (ship == null) return;

        // Revert transformation effects
        ship.getMutableStats().getMaxSpeed().unmodify("super_saiyan");
        ship.getMutableStats().getBallisticWeaponDamageMult().unmodify("super_saiyan");
        ship.getMutableStats().getEnergyWeaponDamageMult().unmodify("super_saiyan");
        ship.getMutableStats().getMissileWeaponDamageMult().unmodify("super_saiyan");
        ship.getMutableStats().getShieldUpkeepMult().unmodify("super_saiyan");

        // Reset the power gauge
        powerGauge = 0f;

        // Fade out transformation particle effect
        particleEffect.fadeOutParticles(ship);

        // Revert hair deco weapon animation
        revertHairDecoWeapon(ship);
    }

    private void revertHairDecoWeapon(ShipAPI ship) {
        if (ship == null) return;

        for (final WeaponAPI weapon : ship.getAllWeapons()) {
            if ("WS0012".equals(weapon.getSlot().getId())) {
                weapon.getAnimation().setFrameRate(12f); // Play frames 12-00
                weapon.getAnimation().play();
                Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                    @Override
                    public void advance(float amount, List<InputEventAPI> events) {
                        if (weapon.getAnimation().getFrame() <= 0) {
                            weapon.getAnimation().pause();
                            Global.getCombatEngine().removePlugin(this);
                        }
                    }
                });
            }
        }
    }

    private void updateHUD(ShipAPI ship) {
        if (Global.getCombatEngine().getPlayerShip() == ship) {
            float progress = powerGauge / GAUGE_MAX;
            Color barColor = progress >= 1.0f ? Misc.getPositiveHighlightColor() : Misc.getNegativeHighlightColor();
            MagicUI.drawHUDStatusBar(
                    ship,
                    progress,
                    barColor,
                    barColor,
                    0,
                    "Transformation",
                    "",
                    false
            );

            // Draw ticks at 20%, 40%, 60%, 80%, and 100%
            for (int i = 1; i <= 5; i++) {
                float tickPosition = i * 0.2f;
                MagicUI.drawHUDStatusBar(
                        ship,
                        tickPosition,
                        Misc.getHighlightColor(),
                        Misc.getHighlightColor(),
                        0,
                        "",
                        "",
                        true
                );
            }
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "20% less flux cost for weapons";
        if (index == 1) return "30% shorter system cooldown";
        if (index == 2) return "temporary boost in speed, damage, and shield efficiency in critical situations";
        if (index == 3) return "Transformation when power gauge is full";
        if (index == 4) return "Doubles weapon damage and speed, halves shield upkeep";
        if (index == 5) return "Transformation lasts for 30 seconds";
        return null;
    }
}