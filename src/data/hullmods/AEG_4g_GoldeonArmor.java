package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_4g_GoldeonArmor extends BaseHullMod {

    private static final String GAUGE_KEY = "goldion_gauge";
    private static final float MAX_GAUGE = 100f;
    private static final float GAIN_PER_SECOND = 10f;

    private final Color GOLD_MAIN = new Color(255, 215, 0, 255);
    private final Color GOLD_LIGHT = new Color(255, 230, 150, 180);
    private final Color GOLD_PARTICLE = new Color(255, 240, 130, 220);

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) return;

        CombatEngineAPI engine = Global.getCombatEngine();

        // Gauge logic
        if (!ship.getSystem().isActive()) {
            Float gauge = (Float) ship.getCustomData().get(GAUGE_KEY);
            if (gauge == null) gauge = 0f;

            gauge += GAIN_PER_SECOND * amount;
            if (gauge >= MAX_GAUGE) {
                gauge = 0f;
                if (ship.getSystem().getCooldownRemaining() <= 0f && !ship.getSystem().isActive()) {
                    ship.useSystem();
                }
            }
            ship.setCustomData(GAUGE_KEY, gauge);

            // Show gauge above the ship
            drawGaugeBar(ship, gauge / MAX_GAUGE);
        }

        // Engine color override
        ship.getEngineController().fadeToOtherColor(this, GOLD_MAIN, GOLD_LIGHT, 1f, 1f);

        // Jitter Over — glowing gold shimmer on top
        ship.setJitter(this, GOLD_MAIN, 1.0f, 5, 10f, 20f);

        // Jitter Under — glowing echo afterimages
        ship.setJitterUnder(this, GOLD_LIGHT, 0.9f, 7, 12f, 24f);

        // Shield visuals override
        if (ship.getShield() != null) {
            ship.getShield().setInnerColor(GOLD_LIGHT);
            ship.getShield().setRingColor(GOLD_MAIN);
        }

        // Vent color override
        ship.setVentCoreColor(GOLD_MAIN);
        ship.setVentFringeColor(GOLD_LIGHT);

        // Golden radiant particles (constant visual motion)
        spawnRadiantParticles(ship, engine);
    }

    private void spawnRadiantParticles(ShipAPI ship, CombatEngineAPI engine) {
        Vector2f loc = ship.getLocation();

        for (int i = 0; i < 4; i++) {
            Vector2f vel = MathUtils.getPoint(new Vector2f(), MathUtils.getRandomNumberInRange(20f, 80f), (float) Math.random() * 360f);
            float size = MathUtils.getRandomNumberInRange(6f, 16f);
            float alpha = MathUtils.getRandomNumberInRange(0.4f, 1f);
            float dur = MathUtils.getRandomNumberInRange(0.6f, 1.4f);

            engine.addNebulaParticle(
                    loc,
                    vel,
                    size,
                    1.5f,
                    0.1f,
                    0.3f,
                    dur,
                    new Color(255, 240, 150, (int)(alpha * 255f))
            );
        }
    }

    private void drawGaugeBar(ShipAPI ship, float progress) {
        Vector2f loc = ship.getLocation();
        float width = 50f;
        float height = 6f;

        Vector2f barStart = new Vector2f(loc.x - width / 2f, loc.y + ship.getCollisionRadius() + 10f);

        Color bg = new Color(30, 30, 30, 150);
        Color fg = new Color(255, 215, 0, 220);

        Global.getCombatEngine().addSmoothParticle(barStart, new Vector2f(), width, 1f, 0.05f, bg);
        Global.getCombatEngine().addSmoothParticle(barStart, new Vector2f(), width * progress, 1.2f, 0.1f, fg);
    }
}
