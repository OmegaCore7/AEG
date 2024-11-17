package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicUI;

import java.awt.Color;
import java.util.List;

public class AEG_transformation extends BaseHullMod {

    private static final float GAUGE_MAX = 1.0f; // Max gauge (100%)

    private float powerGauge = 0f; // Current gauge value
    private AEG_transformationFX particleEffect;

    public void applyEffectsBeforeShipCreation(HullModSpecAPI spec, ShipAPI ship, List<ShipAPI> ships) {
        if (ship == null) return;
        powerGauge = 0f; // Initialize gauge when applied
        particleEffect = new AEG_transformationFX();
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null) return;

        // Ensure particleEffect is initialized
        if (particleEffect == null) {
            particleEffect = new AEG_transformationFX();
        }

        // Increase the power gauge by 1% per second
        powerGauge = Math.min(powerGauge + (0.01f * amount), GAUGE_MAX);
        AEG_transformationBuffs.applyBuffs(ship, powerGauge);

        // Update particles based on charge level
        particleEffect.updateParticles(ship, powerGauge);

        // Check if the power gauge is at maximum
        if (powerGauge >= GAUGE_MAX) {
            // Trigger the transformation
            triggerTransformation(ship);
        }

        // Apply damage reduction if hull is above 50%
        if (ship.getHullLevel() > 0.5f) {
            applyDamageReduction(ship);
        }

        // Update the HUD with the transformation bar
        updateHUD(ship);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        // Reset power gauge and buffs when the battle ends
        powerGauge = 0f;
        if (particleEffect != null) {
            particleEffect.reset();
        }
    }

    private void triggerTransformation(final ShipAPI ship) {
        if (ship == null) return;

        // Ensure hair animation plays before any boosts
        particleEffect.createTransformationEffect(ship);

        // Apply transformation effects
        ship.getMutableStats().getMaxSpeed().modifyFlat("super_saiyan", 100f);
        ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult("super_saiyan", 2f);
        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("super_saiyan", 2f);
        ship.getMutableStats().getMissileWeaponDamageMult().modifyMult("super_saiyan", 2f);
        ship.getMutableStats().getShieldUpkeepMult().modifyMult("super_saiyan", 0.5f); // Reduce shield upkeep by 50%
    }

    private void applyDamageReduction(ShipAPI ship) {
        ship.getMutableStats().getHullDamageTakenMult().modifyFlat("super_saiyan", 0.5f);
        ship.getMutableStats().getArmorDamageTakenMult().modifyFlat("super_saiyan", 0.5f);
    }

    private void updateHUD(ShipAPI ship) {
        if (Global.getCombatEngine().getPlayerShip() == ship) {
            float progress = powerGauge / GAUGE_MAX;
            Color barColor = new Color(217, 255, 161); // Greenish-yellow fill color
            Color backgroundColor = Color.BLACK; // Black background

            // Example coordinates for positioning the HUD
            float xPosition = 100f; // Adjust this value to move left/right
            float yPosition = 60f;  // Adjust this value to move up/down (10f below the current position)

            // Draw the gauge with the label "Transformation" above it
            MagicUI.drawHUDStatusBar(
                    ship,
                    progress,
                    barColor,
                    backgroundColor,
                    0,
                    "",
                    "LSSJ",
                    true
            );
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "20% less flux cost for weapons";
        if (index == 1) return "30% shorter system cooldown";
        if (index == 2) return "temporary boost in speed, damage, and shield efficiency in critical situations";
        if (index == 3) return "Transformation when power gauge is full";
        if (index == 4) return "Doubles weapon damage and speed, halves shield upkeep";
        if (index == 5) return "Transformation lasts indefinitely once triggered";
        if (index == 6) return "Damage under 500 reduced to 1, damage above 500 reduced by 50% if hull is above 50%";
        return null;
    }
}