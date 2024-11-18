package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicUI;

import java.awt.*;

public class AEG_transformation extends BaseHullMod {

    private static final float GAUGE_MAX = 2.0f; // Max gauge (200%)
    private static final float EMP_INTERVAL = 2.0f; // EMP strike interval in seconds
    private static final float EMP_RADIUS = 100f; // EMP strike radius
    private static final Color EMP_COLOR = new Color(0, 255, 0, 255); // Green color for EMP

    private float powerGauge = 0f; // Current gauge value
    private float empTimer = 0f; // Timer for EMP strikes
    private AEG_LSSJCriticalHitHelper critHelper = new AEG_LSSJCriticalHitHelper();

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null) return;

        // Adjust gauge rate based on current power level
        float gaugeRate = 0.01f;
        if (powerGauge >= 1.0f && powerGauge < 1.4f) {
            gaugeRate = 0.005f; // Slow down between 100-140%
        } else if (powerGauge >= 1.4f) {
            gaugeRate = 0.0025f; // Slow down further between 140-200%
        }

        // Increase the power gauge if not fully transformed
        if (powerGauge < GAUGE_MAX) {
            powerGauge = Math.min(powerGauge + (gaugeRate * amount), GAUGE_MAX);
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

        // Handle EMP strikes
        empTimer += amount;
        if (empTimer >= EMP_INTERVAL) {
            empTimer = 0f;
            triggerEMP(ship);
        }
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
            triggerVisualEffect(ship, Color.CYAN);
        }
        if (powerGauge >= 0.2f) {
            ship.getMutableStats().getAcceleration().modifyMult("super_saiyan", 1.2f);
            ship.getMutableStats().getDeceleration().modifyMult("super_saiyan", 1.2f);
            ship.getMutableStats().getTurnAcceleration().modifyMult("super_saiyan", 1.2f);
            ship.getMutableStats().getMaxTurnRate().modifyMult("super_saiyan", 1.2f);
            triggerVisualEffect(ship, Color.YELLOW);
        }
        if (powerGauge >= 0.4f) {
            ship.getMutableStats().getFluxDissipation().modifyMult("super_saiyan", 1.2f);
            triggerVisualEffect(ship, new Color(0, 255, 255)); // Light teal green
        }
        if (powerGauge >= 0.6f) {
            ship.getMutableStats().getArmorDamageTakenMult().modifyMult("super_saiyan", 0.8f); // Reduces armor damage taken by 20%
            triggerVisualEffect(ship, Color.GREEN);
        }
        if (powerGauge >= 0.8f) {
            ship.getMutableStats().getBallisticRoFMult().modifyMult("super_saiyan", 1.2f);
            ship.getMutableStats().getEnergyRoFMult().modifyMult("super_saiyan", 1.2f);
            ship.getMutableStats().getMissileRoFMult().modifyMult("super_saiyan", 1.2f);
            triggerVisualEffect(ship, new Color(173, 255, 47)); // Light yellowish green
        }
        if (powerGauge >= 1.0f) {
            ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("super_saiyan", 1.5f);
            ship.getMutableStats().getEmpDamageTakenMult().modifyMult("super_saiyan", 0.5f); // Reduces EMP damage taken by 50%
            triggerVisualEffect(ship, Color.RED);
        }
        if (powerGauge >= 1.2f) {
            critHelper.setCritChance(0.05f); // Increase critical hit chance at 120%
            triggerVisualEffect(ship, Color.MAGENTA);
        }
        if (powerGauge >= 1.4f) {
            critHelper.setCritChance(0.1f); // Increase critical hit chance at 140%
            triggerVisualEffect(ship, Color.CYAN);
        }
        if (powerGauge >= 1.6f) {
            critHelper.setCritChance(0.15f); // Increase critical hit chance at 160%
            triggerVisualEffect(ship, Color.ORANGE);
        }
        if (powerGauge >= 1.8f) {
            critHelper.setCritChance(0.2f); // Increase critical hit chance at 180%
            triggerVisualEffect(ship, Color.white);
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
        Color particleColor = new Color(0.5f * charge, 0.5f * charge, 1f, MathUtils.getRandomNumberInRange(0.5f, 1f));
        float particleSize = MathUtils.getRandomNumberInRange(10f, 20f) * charge;

        if (shoulderL != null) {
            Vector2f locationL = shoulderL.getLocation();
            Vector2f velocityL = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(50f, 100f), ship.getFacing() + 180f);
            if (locationL == null || velocityL == null) return;
            Global.getCombatEngine().addHitParticle(locationL, velocityL, particleSize, 1f, 1f, particleColor);
        }

        if (shoulderR != null) {
            Vector2f locationR = shoulderR.getLocation();
            Vector2f velocityR = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(50f, 100f), ship.getFacing() + 180f);
            if (locationR == null || velocityR == null) return;
            Global.getCombatEngine().addHitParticle(locationR, velocityR, particleSize, 1f, 1f, particleColor);
        }
    }

    private void playEnhancedEffects(ShipAPI ship, WeaponAPI shoulderL, WeaponAPI shoulderR, float powerGauge) {
        float charge = (powerGauge - 0.99f) / (1.4f - 0.99f);
        Color particleColor = new Color(1f, 1f * charge, 0.5f * charge, MathUtils.getRandomNumberInRange(0.5f, 1f));
        float particleSize = MathUtils.getRandomNumberInRange(20f, 40f) * charge;

        for (ShipEngineControllerAPI.ShipEngineAPI eng : ship.getEngineController().getShipEngines()) {
            Vector2f locationEng = eng.getLocation();
            Vector2f velocityEng = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(100f, 200f), ship.getFacing() + 180f);
            if (locationEng == null || velocityEng == null) continue;
            Global.getCombatEngine().addNebulaParticle(locationEng, velocityEng, particleSize, 1f, 0.5f, 1f, 1f, particleColor);
        }

        if (shoulderL != null) {
            Vector2f locationL = shoulderL.getLocation();
            Vector2f velocityL = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(100f, 200f), ship.getFacing() + 180f);
            if (locationL == null || velocityL == null) return;
            Global.getCombatEngine().addNebulaParticle(locationL, velocityL, particleSize, 1f, 0.5f, 1f, 1f, particleColor);
        }

        if (shoulderR != null) {
            Vector2f locationR = shoulderR.getLocation();
            Vector2f velocityR = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(100f, 200f), ship.getFacing() + 180f);
            if (locationR == null || velocityR == null) return;
            Global.getCombatEngine().addNebulaParticle(locationR, velocityR, particleSize, 1f, 0.5f, 1f, 1f, particleColor);
        }
    }

    private void playUltimateEffects(ShipAPI ship, WeaponAPI shoulderL, WeaponAPI shoulderR, float powerGauge) {
        float charge = (powerGauge - 1.4f) / (2.0f - 1.4f);
        Color particleColor = new Color(0.5f * charge, 1f, 0.5f * charge, MathUtils.getRandomNumberInRange(0.5f, 1f));
        float particleSize = MathUtils.getRandomNumberInRange(40f, 80f) * charge;

        for (ShipEngineControllerAPI.ShipEngineAPI eng : ship.getEngineController().getShipEngines()) {
            Vector2f locationEng = eng.getLocation();
            Vector2f velocityEng = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(200f, 400f), ship.getFacing() + 180f);
            if (locationEng == null || velocityEng == null) continue;
            Global.getCombatEngine().addNebulaParticle(locationEng, velocityEng, particleSize, 1f, 0.5f, 1f, 1f, particleColor);
        }

        if (shoulderL != null) {
            Vector2f locationL = shoulderL.getLocation();
            Vector2f velocityL = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(200f, 400f), ship.getFacing() + 180f);
            if (locationL == null || velocityL == null) return;
            Global.getCombatEngine().addNebulaParticle(locationL, velocityL, particleSize, 1f, 0.5f, 1f, 1f, particleColor);
        }

        if (shoulderR != null) {
            Vector2f locationR = shoulderR.getLocation();
            Vector2f velocityR = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(200f, 400f), ship.getFacing() + 180f);
            if (locationR == null || velocityR == null) return;
            Global.getCombatEngine().addNebulaParticle(locationR, velocityR, particleSize, 1f, 0.5f, 1f, 1f, particleColor);
        }

        // Big explosion at 100
        if (powerGauge >= 100f) {
            Vector2f location = ship.getLocation();
            Vector2f velocity = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(300f, 600f), ship.getFacing() + 180f);
            if (location == null || velocity == null) return;
            Global.getCombatEngine().spawnExplosion(location, velocity, Color.RED, 300f, 2f);
            Global.getCombatEngine().spawnExplosion(location, velocity, Color.MAGENTA, 400f, 2.5f);
        }

        // Smoke/burnout effect at 200
        if (powerGauge >= 200f) {
            for (ShipEngineControllerAPI.ShipEngineAPI eng : ship.getEngineController().getShipEngines()) {
                Vector2f locationEng = eng.getLocation();
                Vector2f velocityEng = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(100f, 200f), ship.getFacing() + 180f);
                if (locationEng == null || velocityEng == null) continue;
                float particleSizeEng = MathUtils.getRandomNumberInRange(20f, 40f);
                Global.getCombatEngine().addNebulaParticle(locationEng, velocityEng, particleSizeEng, 1f, 0.5f, 1f, 1f, Color.GRAY);
            }
        }
    }

    private void triggerEMP(ShipAPI ship) {
        WeaponAPI shoulderL = getWeaponBySlot(ship, "WS0001");
        WeaponAPI shoulderR = getWeaponBySlot(ship, "WS0002");
        Vector2f newPoint1 = new Vector2f(shoulderL.getLocation().x - 22f, shoulderL.getLocation().y);
        Vector2f newPoint2 = new Vector2f(shoulderR.getLocation().x - 22f, shoulderR.getLocation().y);

        Vector2f[] points = {shoulderL.getLocation(), shoulderR.getLocation(), newPoint1, newPoint2};

        for (Vector2f point : points) {
            Vector2f randomPoint = MathUtils.getRandomPointInCircle(point, EMP_RADIUS);
            Global.getCombatEngine().spawnEmpArcPierceShields(
                    ship, randomPoint, null, ship,
                    DamageType.ENERGY, 0f, 0f, 100000f, null, 10f, EMP_COLOR, EMP_COLOR
            );
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
                barColor = Color.GREEN; // First half of the gauge
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
                    "LSSJ",
                    "",
                    true
            );
        }
    }

    private void triggerVisualEffect(ShipAPI ship, Color color) {
        for (ShipEngineControllerAPI.ShipEngineAPI eng : ship.getEngineController().getShipEngines()) {
            Vector2f locationEng = eng.getLocation();
            Vector2f velocityEng = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(50f, 100f), ship.getFacing() + 180f);
            if (locationEng == null || velocityEng == null) continue;
            float particleSizeEng = MathUtils.getRandomNumberInRange(30f, 60f);
            Global.getCombatEngine().addHitParticle(locationEng, velocityEng, particleSizeEng, 1f, 0.5f, color);
        }
    }
}