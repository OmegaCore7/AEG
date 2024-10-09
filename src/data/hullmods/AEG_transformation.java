package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

public class AEG_transformation extends BaseHullMod {

    private static final float GAUGE_MAX = 1.0f; // Max gauge (100%)
    private static final float[] BOOST_THRESHOLDS = {0.5f, 0.75f, 1.0f};
    private static final float TRANSFORMATION_DURATION = 30f; // Duration of the transformation in seconds
    private static final Color AURA_COLOR = new Color(255, 223, 0, 150); // Golden yellow color with some transparency

    private float powerGauge = 0f; // Current gauge value

    public void applyEffectsBeforeShipCreation(HullModSpecAPI spec, ShipAPI ship, List<ShipAPI> ships) {
        if (ship == null) return;
        powerGauge = 0f; // Initialize gauge when applied
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null) return;

        // Increase the power gauge by 1% per second
        powerGauge = Math.min(powerGauge + (0.01f * amount), GAUGE_MAX);
        applyBuffs(ship);

        // Check if the power gauge is at maximum
        if (powerGauge >= GAUGE_MAX) {
            // Trigger the transformation
            triggerTransformation(ship);
        }
    }

    public void render(ShipAPI ship, float alpha) {
        if (ship == null) return;

        // Get the position to draw the gauge
        float x = ship.getLocation().x + 20; // Offset from the ship's position
        float y = ship.getLocation().y + 40; // Offset from the ship's position
        float width = 100; // Width of the gauge
        float height = 10; // Height of the gauge

        // Draw the background
        GL11.glColor4f(0, 0, 0, 0.5f); // Semi-transparent black
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();

        // Draw the filled gauge
        float filledWidth = (powerGauge / GAUGE_MAX) * width;
        GL11.glColor4f(0, 1, 0, 1); // Green for the filled gauge
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + filledWidth, y);
        GL11.glVertex2f(x + filledWidth, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
    }

    private void applyBuffs(ShipAPI ship) {
        if (ship == null) return;

        for (float threshold : BOOST_THRESHOLDS) {
            if (powerGauge >= threshold) {
                increaseArmorEffectiveness(ship);
                increaseWeaponStats(ship);
                increaseEnergyDamage(ship);
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

    private void triggerTransformation(final ShipAPI ship) {
        if (ship == null) return;

        // Apply transformation effects
        ship.getMutableStats().getMaxSpeed().modifyFlat("super_saiyan", 100f);
        ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult("super_saiyan", 2f);
        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("super_saiyan", 2f);
        ship.getMutableStats().getMissileWeaponDamageMult().modifyMult("super_saiyan", 2f);
        ship.getMutableStats().getShieldUpkeepMult().modifyMult("super_saiyan", 0.5f); // Reduce shield upkeep by 50%

        // Add visual and audio effects
        Global.getCombatEngine().addFloatingText(ship.getLocation(), "Transformation Activated!", 30, Color.GREEN, ship, 1f, 2f);
        Global.getSoundPlayer().playSound("super_saiyan_transformation", 1f, 1f, ship.getLocation(), ship.getVelocity());

        // Add golden yellow aura effect
        addAuraEffect(ship);

        // Set a timer to revert the transformation after the duration
        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            private float timer = TRANSFORMATION_DURATION;

            public void advance(float amount, List<InputEventAPI> events) {
                timer -= amount;
                if (timer <= 0) {
                    revertTransformation(ship);
                    Global.getCombatEngine().removePlugin(this);
                }
            }
        });
    }

    private void addAuraEffect(ShipAPI ship) {
        if (ship == null) return;

        for (int i = 0; i < 360; i += 10) {
            float angle = (float) Math.toRadians(i);
            Vector2f offset = new Vector2f((float) Math.cos(angle) * ship.getCollisionRadius(), (float) Math.sin(angle) * ship.getCollisionRadius());
            Vector2f location = Vector2f.add(ship.getLocation(), offset, new Vector2f());
            Global.getCombatEngine().addNebulaParticle(location, ship.getVelocity(), 50f, 1.5f, 0.1f, 0.3f, TRANSFORMATION_DURATION, AURA_COLOR);
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
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "20% less energy consumption for Plasma Jets and Blaster Shell";
        if (index == 1) return "30% longer system cooldown";
        if (index == 2) return "25% more flux generated by weapons";
        if (index == 3) return "50% increased shield upkeep";
        if (index == 4) return "10% reduced top speed when Plasma Jets are active";
        if (index == 5) return "temporary boost in speed, damage, and shield efficiency in critical situations";
        if (index == 6) return "Transformation when power gauge is full";
        if (index == 7) return "Doubles weapon damage and speed, halves shield upkeep";
        if (index == 8) return "Transformation lasts for 30 seconds";
        return null;
    }
}
