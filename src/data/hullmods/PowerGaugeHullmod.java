package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.List;

public class PowerGaugeHullmod extends BaseHullMod {

    private static final float GAUGE_MAX = 1.0f; // Max gauge (100%)
    private static final float[] BOOST_THRESHOLDS = {0.5f, 0.75f, 1.0f};

    private float powerGauge = 0f; // Current gauge value


    public void applyEffectsBeforeShipCreation(HullModSpecAPI spec, ShipAPI ship, List<ShipAPI> ships) {
        powerGauge = 0f; // Initialize gauge when applied
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        // Increase the power gauge by 1% per second
        powerGauge = Math.min(powerGauge + (0.01f * amount), GAUGE_MAX);
        applyBuffs(ship);
    }


    public void render(ShipAPI ship, float alpha) {
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
        if (powerGauge >= BOOST_THRESHOLDS[0]) increaseArmorEffectiveness(ship);
        if (powerGauge >= BOOST_THRESHOLDS[1]) increaseWeaponStats(ship);
        if (powerGauge >= BOOST_THRESHOLDS[2]) increaseEnergyDamage(ship);
    }

    private void increaseArmorEffectiveness(ShipAPI ship) {
        // Logic to increase effective armor reduction
    }

    private void increaseWeaponStats(ShipAPI ship) {
        // Logic to increase weapon reload, cooldown, and rate of fire
    }

    private void increaseEnergyDamage(ShipAPI ship) {
        // Logic to increase energy damage
    }
}
