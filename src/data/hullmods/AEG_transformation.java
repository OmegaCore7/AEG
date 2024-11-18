package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicUI;

import java.awt.Color;
import java.util.List;

public class AEG_transformation extends BaseHullMod {

    private static final float GAUGE_MAX = 2.0f; // Max gauge (200%)

    private float powerGauge = 0f; // Current gauge value
    private AEG_LSSJCriticalHitHelper critHelper = new AEG_LSSJCriticalHitHelper();
    private boolean hairAnimationStarted = false;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null) return;

        // Start hair animation at the beginning of combat
        if (!hairAnimationStarted) {
            startHairAnimation(ship);
            hairAnimationStarted = true;
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

        // Apply special effects based on power gauge thresholds
        applyEffects(ship, powerGauge);

        // Update the HUD with the transformation bar
        updateHUD(ship);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        // Reset power gauge when the battle ends
        powerGauge = 0f;
        hairAnimationStarted = false;
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

    private void startHairAnimation(ShipAPI ship) {
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
                    if (powerGauge == 0f) {
                        finalHairWeapon.getAnimation().setFrame(0);
                        finalHairWeapon.getAnimation().setFrameRate(24f); // Reset animation
                        loopStarted = false;
                    }
                }

                @Override
                public void processInputPreCoreControls(float amount, List events) {
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

    private void applyEffects(ShipAPI ship, float powerGauge) {
        if (ship == null || !ship.isAlive()) return;

        WeaponAPI shoulderL = getWeaponBySlot(ship, "WS0001");
        WeaponAPI shoulderR = getWeaponBySlot(ship, "WS0002");

        if (powerGauge <= 0.99f) {
            playChargingEffects(ship, shoulderL, shoulderR, powerGauge);
        } else if (powerGauge <= 1.4f) {
            playEnhancedEffects(ship, shoulderL, shoulderR, powerGauge);
        } else if (powerGauge <= 2.0f) {
            playUltimateEffects(ship, shoulderL, shoulderR, powerGauge);
        }
    }
    private void playChargingEffects(ShipAPI ship, WeaponAPI shoulderL, WeaponAPI shoulderR, float powerGauge) {
        float charge = powerGauge / 0.99f;
        Color particleColor = new Color(1f, 0.5f * charge, 0.5f * charge, MathUtils.getRandomNumberInRange(0.5f, 1f));

        for (ShipEngineControllerAPI.ShipEngineAPI eng : ship.getEngineController().getShipEngines()) {
            Vector2f location = eng.getLocation();
            Vector2f velocity = ship.getVelocity();
            if (location == null || velocity == null) continue;
            float size = MathUtils.getRandomNumberInRange(10f, 20f) * charge;
            Global.getCombatEngine().addHitParticle(location, velocity, size, 1f, 1f, particleColor);
        }

        if (shoulderL != null) {
            Vector2f location = shoulderL.getLocation();
            Vector2f velocity = ship.getVelocity();
            if (location == null || velocity == null) return;
            float size = MathUtils.getRandomNumberInRange(10f, 20f) * charge;
            Global.getCombatEngine().addHitParticle(location, velocity, size, 1f, 1f, particleColor);
        }

        if (shoulderR != null) {
            Vector2f location = shoulderR.getLocation();
            Vector2f velocity = ship.getVelocity();
            if (location == null || velocity == null) return;
            float size = MathUtils.getRandomNumberInRange(10f, 20f) * charge;
            Global.getCombatEngine().addHitParticle(location, velocity, size, 1f, 1f, particleColor);
        }
    }

    private void playEnhancedEffects(ShipAPI ship, WeaponAPI shoulderL, WeaponAPI shoulderR, float powerGauge) {
        float charge = (powerGauge - 0.99f) / (1.4f - 0.99f);
        Color particleColor = new Color(0.5f * charge, 0.5f, 1f * charge, MathUtils.getRandomNumberInRange(0.5f, 1f));

        for (ShipEngineControllerAPI.ShipEngineAPI eng : ship.getEngineController().getShipEngines()) {
            Vector2f location = eng.getLocation();
            Vector2f velocity = ship.getVelocity();
            if (location == null || velocity == null) continue;
            float size = MathUtils.getRandomNumberInRange(20f, 40f) * charge;
            Global.getCombatEngine().addNebulaParticle(location, velocity, size, 1f, 0.5f, 1f, 1f, particleColor);
        }

        if (shoulderL != null) {
            Vector2f location = shoulderL.getLocation();
            Vector2f velocity = ship.getVelocity();
            if (location == null || velocity == null) return;
            float size = MathUtils.getRandomNumberInRange(20f, 40f) * charge;
            Global.getCombatEngine().addNebulaParticle(location, velocity, size, 1f, 0.5f, 1f, 1f, particleColor);
        }

        if (shoulderR != null) {
            Vector2f location = shoulderR.getLocation();
            Vector2f velocity = ship.getVelocity();
            if (location == null || velocity == null) return;
            float size = MathUtils.getRandomNumberInRange(20f, 40f) * charge;
            Global.getCombatEngine().addNebulaParticle(location, velocity, size, 1f, 0.5f, 1f, 1f, particleColor);
        }
    }

    private void playUltimateEffects(ShipAPI ship, WeaponAPI shoulderL, WeaponAPI shoulderR, float powerGauge) {
        float charge = (powerGauge - 1.4f) / (2.0f - 1.4f);
        Color particleColor = new Color(1f, 0.5f * charge, 0.5f * charge, MathUtils.getRandomNumberInRange(0.5f, 1f));

        for (ShipEngineControllerAPI.ShipEngineAPI eng : ship.getEngineController().getShipEngines()) {
            Vector2f location = eng.getLocation();
            Vector2f velocity = ship.getVelocity();
            if (location == null || velocity == null) continue;
            float size = MathUtils.getRandomNumberInRange(40f, 80f) * charge;
            Global.getCombatEngine().addNebulaParticle(location, velocity, size, 1f, 0.5f, 1f, 1f, particleColor);
        }

        if (shoulderL != null) {
            Vector2f location = shoulderL.getLocation();
            Vector2f velocity = ship.getVelocity();
            if (location == null || velocity == null) return;
            float size = MathUtils.getRandomNumberInRange(40f, 80f) * charge;
            Global.getCombatEngine().addNebulaParticle(location, velocity, size, 1f, 0.5f, 1f, 1f, particleColor);
        }

        if (shoulderR != null) {
            Vector2f location = shoulderR.getLocation();
            Vector2f velocity = ship.getVelocity();
            if (location == null || velocity == null) return;
            float size = MathUtils.getRandomNumberInRange(40f, 80f) * charge;
            Global.getCombatEngine().addNebulaParticle(location, velocity, size, 1f, 0.5f, 1f, 1f, particleColor);
        }

        // Big explosion at 100
        if (powerGauge >= 100f) {
            Vector2f location = ship.getLocation();
            Vector2f velocity = ship.getVelocity();
            if (location == null || velocity == null) return;
            Global.getCombatEngine().spawnExplosion(location, velocity, Color.RED, 300f, 2f);
            Global.getCombatEngine().spawnExplosion(location, velocity, Color.MAGENTA, 400f, 2.5f);
        }

        // Smoke/burnout effect at 200
        if (powerGauge >= 200f) {
            for (ShipEngineControllerAPI.ShipEngineAPI eng : ship.getEngineController().getShipEngines()) {
                Vector2f location = eng.getLocation();
                Vector2f velocity = ship.getVelocity();
                if (location == null || velocity == null) continue;
                float size = MathUtils.getRandomNumberInRange(20f, 40f);
                Global.getCombatEngine().addNebulaParticle(location, velocity, size, 1f, 0.5f, 1f, 1f, Color.GRAY);
            }
        }
    }

    private WeaponAPI getWeaponBySlot(ShipAPI ship, String slotId) {
        if (ship == null) return null;
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSlot().getId().equals(slotId)) {
                return weapon;
            }
        }
        return null;
    }

    private void updateHUD(ShipAPI ship) {
        if (Global.getCombatEngine().getPlayerShip() == ship) {
            float progress = powerGauge / GAUGE_MAX;
            Color barColor;
            if (progress < 0.5f) {
                barColor = Color.YELLOW; // First half of the gauge
            } else {
                barColor = new Color(217, 255, 161); // Second half of the gauge
            }
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