package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.magiclib.util.MagicUI;

import java.awt.Color;
import java.util.List;

public class AEG_transformation extends BaseHullMod {

    private static final float GAUGE_MAX = 2.0f; // Max gauge (200%)

    private float powerGauge = 0f; // Current gauge value
    private AEG_LSSJCriticalHitHelper critHelper = new AEG_LSSJCriticalHitHelper();
    private AEG_SpecialEffectsHelper effectsHelper;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null) return;

        if (effectsHelper == null) {
            effectsHelper = new AEG_SpecialEffectsHelper(Global.getCombatEngine());
        }

        // Increase the power gauge by 1% per second if not fully transformed
        if (powerGauge < GAUGE_MAX) {
            powerGauge = Math.min(powerGauge + (0.01f * amount), GAUGE_MAX);
        } else {
            // Reset the transformation state after reaching 200%
            powerGauge = 0f;
        }

        // Store the power gauge value in the ship's custom data
        ship.setCustomData("powerGauge", powerGauge);

        // Apply buffs based on charge level
        applyBuffs(ship, powerGauge);

        // Trigger the hair transformation effect at 100%
        if (powerGauge >= 1.0f) {
            createTransformationEffect(ship);
        }

        // Apply special effects based on power gauge thresholds
        effectsHelper.applyEffects(ship, powerGauge);

        // Update the HUD with the transformation bar
        updateHUD(ship);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        // Reset power gauge when the battle ends
        powerGauge = 0f;
    }

    private void applyBuffs(ShipAPI ship, float powerGauge) {
        if (ship == null) return;

        if (powerGauge >= 0.1f) {
            ship.getMutableStats().getShieldUnfoldRateMult().modifyMult("super_saiyan", 1.2f);
        }
        if (powerGauge >= 0.2f) {
            ship.getMutableStats().getAcceleration().modifyMult("super_saiyan", 1.2f);
            ship.getMutableStats().getDeceleration().modifyMult("super_saiyan", 1.2f);
            ship.getMutableStats().getTurnAcceleration().modifyMult("super_saiyan", 1.2f);
            ship.getMutableStats().getMaxTurnRate().modifyMult("super_saiyan", 1.2f);
        }
        if (powerGauge >= 0.4f) {
            ship.getMutableStats().getFluxDissipation().modifyMult("super_saiyan", 1.2f);
        }
        if (powerGauge >= 0.6f) {
            ship.getMutableStats().getArmorDamageTakenMult().modifyMult("super_saiyan", 0.8f); // Reduces armor damage taken by 20%
        }
        if (powerGauge >= 0.8f) {
            ship.getMutableStats().getBallisticRoFMult().modifyMult("super_saiyan", 1.2f);
            ship.getMutableStats().getEnergyRoFMult().modifyMult("super_saiyan", 1.2f);
            ship.getMutableStats().getMissileRoFMult().modifyMult("super_saiyan", 1.2f);
        }
        if (powerGauge >= 1.0f) {
            ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("super_saiyan", 1.5f);
            ship.getMutableStats().getEmpDamageTakenMult().modifyMult("super_saiyan", 0.5f); // Reduces EMP damage taken by 50%
        }
        if (powerGauge >= 1.2f) {
            critHelper.setCritChance(0.05f); // Increase critical hit chance at 120%
        }
        if (powerGauge >= 1.4f) {
            critHelper.setCritChance(0.1f); // Increase critical hit chance at 140%
        }
        if (powerGauge >= 1.6f) {
            critHelper.setCritChance(0.15f); // Increase critical hit chance at 160%
        }
        if (powerGauge >= 1.8f) {
            critHelper.setCritChance(0.2f); // Increase critical hit chance at 180%
        }
    }
    private void createTransformationEffect(ShipAPI ship) {
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
            hairWeapon.getAnimation().setFrameRate(24f); // Speed up the animation by 2x

            final WeaponAPI finalHairWeapon = hairWeapon;
            final WeaponAPI finalHeadWeapon = headWeapon;
            Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                private boolean loopStarted = false;

                @Override
                public void advance(float amount, List events) {
                    finalHairWeapon.setCurrAngle(finalHeadWeapon.getCurrAngle());
                    if (finalHairWeapon.getAnimation().getFrame() >= 12 && !loopStarted) {
                        finalHairWeapon.getAnimation().setFrame(6);
                        finalHairWeapon.getAnimation().setFrameRate(24f / (12 - 6)); // Loop frames 6-12 at 2x speed
                        loopStarted = true;
                    }
                }

                @Override
                public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
                    // No implementation needed for this method
                }

                @Override
                public void renderInWorldCoords(ViewportAPI viewport) {
                    // No implementation needed for this method
                }

                @Override
                public void renderInUICoords(ViewportAPI viewport) {
                    // No implementation needed for this method
                }
            });
        }
    }

    private void updateHUD(ShipAPI ship) {
        if (Global.getCombatEngine().getPlayerShip() == ship) {
            float progress = powerGauge / GAUGE_MAX;
            Color barColor = progress < 0.5f ? Color.YELLOW : new Color(217, 255, 161); // Yellow from 0 to 100, greenish-yellow from 100 to 200
            Color backgroundColor = Color.BLACK; // Black background

            // Draw the gauge with the label "Transformation" above it
            MagicUI.drawHUDStatusBar(
                    ship,
                    progress,
                    barColor,
                    backgroundColor,
                    0,
                    "Transformation",
                    "",
                    true
            );
        }
    }
}