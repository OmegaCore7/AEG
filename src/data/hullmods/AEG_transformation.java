package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.magiclib.util.MagicUI;

import java.awt.Color;
import java.util.List;

public class AEG_transformation extends BaseHullMod {

    private static final float GAUGE_MAX = 1.0f; // Max gauge (100%)
    private static final float TRANSFORMATION_DURATION = 60f; // Duration of the transformation in seconds

    private float powerGauge = 0f; // Current gauge value
    private float transformationTime = 0f; // Time spent in the transformed state

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null) return;

        // Increase the power gauge by 1% per second if not fully transformed
        if (powerGauge < GAUGE_MAX) {
            powerGauge = Math.min(powerGauge + (0.01f * amount), GAUGE_MAX);
        } else {
            // Track the time spent in the transformed state
            transformationTime += amount;
            if (transformationTime >= TRANSFORMATION_DURATION) {
                // Reset the transformation state after the duration
                powerGauge = 0f;
                transformationTime = 0f;
            }
        }

        // Apply buffs based on charge level
        applyBuffs(ship, powerGauge);

        // Trigger the hair transformation effect at 99%
        if (powerGauge >= 0.99f) {
            createTransformationEffect(ship);
        }

        // Update the HUD with the transformation bar
        updateHUD(ship);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        // Reset power gauge when the battle ends
        powerGauge = 0f;
        transformationTime = 0f;
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
            });
        }
    }

    private void updateHUD(ShipAPI ship) {
        if (Global.getCombatEngine().getPlayerShip() == ship) {
            float progress = powerGauge / GAUGE_MAX;
            Color barColor = new Color(217, 255, 161); // Greenish-yellow fill color
            Color backgroundColor = Color.BLACK; // Black background

            // Draw the gauge with the label "Transformation" above it
            MagicUI.drawHUDStatusBar(
                    ship,
                    progress,
                    barColor,
                    backgroundColor,
                    0,
                    "SSJ",
                    "",
                    true
            );
        }
    }
}