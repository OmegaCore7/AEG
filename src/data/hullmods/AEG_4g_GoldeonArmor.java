package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicUI;

import java.awt.*;

public class AEG_4g_GoldeonArmor extends BaseHullMod {
    private final String id = "goldion_armor_boost";
    private static final String GOLDION_ACTIVE_KEY = "goldion_active";
    private static final String GAUGE_KEY = "goldion_gauge";
    private static final float MAX_GAUGE = 100f;
    private static final float GAIN_PER_SECOND = 10f;
    private static final float GOLDION_DURATION = 20f;
    private static final String GOLDION_TIMER_KEY = "goldion_timer";
    private final Color GOLD_MAIN = new Color(255, 215, 0, 255);
    private final Color GOLD_LIGHT = new Color(255, 230, 150, 180);
    private final Color GOLD_PARTICLE = new Color(255, 240, 130, 220);
    private static final float SPEED_BOOST = 100f;
    private static final float MANEUVER_BOOST = 100f;
    private static final String BOOST_APPLIED_KEY = "goldion_boost_applied";

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
                gauge = MAX_GAUGE; // Don't reset unless activated
            }
            ship.setCustomData(GAUGE_KEY, gauge);

            // Show gauge above the ship
            drawGaugeHUD(ship, gauge / MAX_GAUGE);
        }
        // Check if the golden effect is active
        boolean isActive = ship.getCustomData().get(GOLDION_ACTIVE_KEY) instanceof Boolean &&
                (Boolean) ship.getCustomData().get(GOLDION_ACTIVE_KEY);
        Float timer = (Float) ship.getCustomData().get(GOLDION_TIMER_KEY); // <- Make sure this is defined
        Float gauge = (Float) ship.getCustomData().get(GAUGE_KEY);
        if (gauge == null) gauge = 0f;

        // Only allow activation if gauge is full and system is not on cooldown
        boolean canActivate = gauge >= MAX_GAUGE && !Boolean.TRUE.equals(ship.getCustomData().get(GOLDION_ACTIVE_KEY));

        // Check for Shift + X key press
        if (canActivate && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
                && Keyboard.isKeyDown(Keyboard.KEY_X)) {
            ship.setCustomData(GOLDION_ACTIVE_KEY, true);
            ship.setCustomData(GOLDION_TIMER_KEY, GOLDION_DURATION);
            ship.setCustomData(GAUGE_KEY, 0f); // Reset the gauge
            for (int i = 0; i < 20; i++) {
                Vector2f burstVel = MathUtils.getPoint(new Vector2f(), MathUtils.getRandomNumberInRange(50f, 200f), (float)Math.random() * 360f);
                engine.addSmoothParticle(
                        ship.getLocation(),
                        burstVel,
                        MathUtils.getRandomNumberInRange(12f, 25f),
                        1.5f,
                        1.2f,
                        new Color(GOLD_PARTICLE.getRed(), GOLD_PARTICLE.getGreen(), GOLD_PARTICLE.getBlue(), 255)
                );
            }
        }
        //Logic for Activating Goldion Mode
        if (isActive) {
            if (timer == null) timer = GOLDION_DURATION;
            timer -= amount;

            boolean systemActive = ship.getSystem() != null && ship.getSystem().isActive();
            boolean boostApplied = Boolean.TRUE.equals(ship.getCustomData().get(BOOST_APPLIED_KEY));

            if (timer > 0f) {
                // Visual effects
                ship.getEngineController().fadeToOtherColor(this, GOLD_MAIN, GOLD_LIGHT, 1f, 1f);
                ship.getEngineController().extendFlame(this, 1.5f, 1f, 1.3f);
                ship.setJitter(this, GOLD_MAIN, 1.0f, 1 + MathUtils.getRandom().nextInt(4), 2f, 5f);
                ship.setJitterUnder(this, GOLD_LIGHT, 1f, 3, 0f, 20f);

                if (ship.getShield() != null) {
                    ship.getShield().setInnerColor(GOLD_LIGHT);
                    ship.getShield().setRingColor(GOLD_MAIN);
                }

                ship.setVentCoreColor(GOLD_MAIN);
                ship.setVentFringeColor(GOLD_LIGHT);
                spawnRadiantParticles(ship, engine);

                // Handle speed/maneuver boosts
                if (!systemActive && !boostApplied) {
                    ship.getMutableStats().getMaxSpeed().modifyFlat(id, SPEED_BOOST);
                    ship.getMutableStats().getAcceleration().modifyFlat(id, MANEUVER_BOOST);
                    ship.getMutableStats().getDeceleration().modifyFlat(id, MANEUVER_BOOST);
                    ship.getMutableStats().getTurnAcceleration().modifyFlat(id, MANEUVER_BOOST);
                    ship.getMutableStats().getMaxTurnRate().modifyFlat(id, MANEUVER_BOOST);
                    ship.setCustomData(BOOST_APPLIED_KEY, true);
                }

                // Temporarily remove boost if system is active
                if (systemActive && boostApplied) {
                    ship.getMutableStats().getMaxSpeed().unmodifyFlat(id);
                    ship.getMutableStats().getAcceleration().unmodifyFlat(id);
                    ship.getMutableStats().getDeceleration().unmodifyFlat(id);
                    ship.getMutableStats().getTurnAcceleration().unmodifyFlat(id);
                    ship.getMutableStats().getMaxTurnRate().unmodifyFlat(id);
                    ship.setCustomData(BOOST_APPLIED_KEY, false);
                }

            } else {
                // End of Goldion Mode: Remove boosts and flags
                ship.getMutableStats().getMaxSpeed().unmodifyFlat(id);
                ship.getMutableStats().getAcceleration().unmodifyFlat(id);
                ship.getMutableStats().getDeceleration().unmodifyFlat(id);
                ship.getMutableStats().getTurnAcceleration().unmodifyFlat(id);
                ship.getMutableStats().getMaxTurnRate().unmodifyFlat(id);
                ship.setCustomData(GOLDION_ACTIVE_KEY, false);
                ship.setCustomData(BOOST_APPLIED_KEY, false);
            }

            ship.setCustomData(GOLDION_TIMER_KEY, timer);
        }
    }
    private void spawnRadiantParticles(ShipAPI ship, CombatEngineAPI engine) {
        Vector2f loc = ship.getLocation();

        for (int i = 0; i < 8; i++) {
            float angle = (float) Math.random() * 360f;
            float speed = MathUtils.getRandomNumberInRange(40f, 120f);
            Vector2f vel = MathUtils.getPoint(new Vector2f(), speed, angle);

            float size = MathUtils.getRandomNumberInRange(8f, 18f);
            float alpha = MathUtils.getRandomNumberInRange(0.5f, 1f);
            float dur = MathUtils.getRandomNumberInRange(0.7f, 1.5f);

            Color particleColor = new Color(
                    GOLD_PARTICLE.getRed(),
                    GOLD_PARTICLE.getGreen(),
                    GOLD_PARTICLE.getBlue(),
                    (int)(alpha * 255f)
            );

            engine.addNebulaParticle(
                    loc,
                    vel,
                    size,
                    1.8f,   // intensity multiplier
                    0.1f,   // ramp-up time
                    0.3f,   // ramp-down time
                    dur,
                    particleColor
            );
        }
    }

    private void drawGaugeHUD(ShipAPI ship, float progress) {
        if (Global.getCombatEngine().getPlayerShip() == ship) {
            Color barColor = (progress < 1f) ? new Color(255, 150, 0) : new Color(255, 225, 50);
            Color bgColor = new Color(0, 0, 0, 180);

            MagicUI.drawHUDStatusBar(
                    ship,
                    progress,
                    barColor,
                    bgColor,
                    0,
                    "GOLDION ARMOR MODE",
                    "",
                    true
            );
        }
    }
}
