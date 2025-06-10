package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicUI;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AEG_4g_GoldeonArmor extends BaseHullMod {

    private static final String DISSOLVE_CD_KEY = "goldion_dissolve_cd";  //Performance boost for dissolve mechanic
    private static final float DISSOLVE_INTERVAL = 0.5f;  //Performance boost for dissolve mechanic
    private final String id = "goldion_armor_boost";
    private static final String GOLDION_ACTIVE_KEY = "goldion_active";
    private static final String GAUGE_KEY = "goldion_gauge";
    private static final float MAX_GAUGE = 100f;
    private static final float GAIN_PER_SECOND = 20f;
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
            float gauge = getGauge(ship);

            // Calculate the player's health percentage (0 to 1)
            float healthPercentage = ship.getHitpoints() / ship.getMaxHitpoints();

            // Calculate the dynamic fill rate based on health percentage
            // The closer the health is to 0, the faster the gauge fills.
            float healthFactor = (1f - healthPercentage) / (1f - 0.2f);  // The 0.2f corresponds to 20% health
            float dynamicGainRate = GAIN_PER_SECOND + healthFactor * (GAIN_PER_SECOND * 9);  // Fill faster at lower health

            gauge += dynamicGainRate * amount;

            // Ensure the gauge doesn't exceed the max
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
        float gauge = getGauge(ship);

        // Only allow activation if gauge is full and system is not on cooldown
        boolean canActivate = gauge >= MAX_GAUGE && !Boolean.TRUE.equals(ship.getCustomData().get(GOLDION_ACTIVE_KEY));

        // Check for Shift + X key press
        if (canActivate && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
                && Keyboard.isKeyDown(Keyboard.KEY_X)) {
            ship.setCustomData(GOLDION_ACTIVE_KEY, true);
            ship.setCustomData(GOLDION_TIMER_KEY, GOLDION_DURATION);
            ship.setCustomData(GAUGE_KEY, 0f); // Reset the gauge
            for (int i = 0; i < 20; i++) {
                Vector2f burstVel = MathUtils.getPoint(new Vector2f(), MathUtils.getRandomNumberInRange(50f, 200f), (float) MathUtils.getRandom().nextFloat() * 360f);
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

        // Logic for Activating Goldion Mode
        if (isActive) {
            if (timer == null) timer = GOLDION_DURATION;
            timer -= amount;

            boolean systemActive = ship.getSystem() != null && ship.getSystem().isActive();
            boolean boostApplied = Boolean.TRUE.equals(ship.getCustomData().get(BOOST_APPLIED_KEY));

            if (timer > 0f) {
                // Call the dissolve function to remove nearby projectiles and missiles (Cooldown for performance)
                Float cd = (Float) ship.getCustomData().get(DISSOLVE_CD_KEY);
                if (cd == null) cd = 0f;
                cd -= amount;
                if (cd <= 0f) {
                    dissolveProjectilesAndMissiles(ship, engine);
                    cd = DISSOLVE_INTERVAL;
                }
                ship.setCustomData(DISSOLVE_CD_KEY, cd);

                // Visual effects
                ship.getEngineController().fadeToOtherColor(this, GOLD_MAIN, GOLD_LIGHT, 1f, 1f);
                ship.getEngineController().extendFlame(this, 1.5f, 1f, 1.3f);
                ship.setJitter(this, GOLD_MAIN, 1.0f, 1 + MathUtils.getRandom().nextInt(4), 2f, 5f);
                ship.setJitterUnder(this, GOLD_LIGHT, 0.5f, 10, 10f, 25f);
                // Apply custom shield settings during Goldion Armor mode
                if (ship.getShield() != null) {
                    // Set the shield's absorption rate to 100% for energy damage
                    ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, 0f);  // 100% absorption
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
                if (ship.getShield() != null) {
                    // Reset shield colors to default
                    ship.getShield().setInnerColor(new Color(255, 175, 100, 255));  // Default shield inner color (for example)
                    ship.getShield().setRingColor(new Color(255, 125, 50, 255));  // Default ring color (for example)
                    ship.getMutableStats().getShieldDamageTakenMult().unmodify(id); // Reset absorption back to normal
                }
                ship.setCustomData(GOLDION_ACTIVE_KEY, false);
                ship.setCustomData(BOOST_APPLIED_KEY, false);

            }
            //Cleanup
            ship.setCustomData(GAUGE_KEY, 0f);
            ship.setCustomData(DISSOLVE_CD_KEY, 0f);
            ship.setCustomData(GOLDION_TIMER_KEY, timer);
        }
    }

    private void spawnRadiantParticles(ShipAPI ship, CombatEngineAPI engine) {
        Vector2f loc = ship.getLocation();

        for (int i = 0; i < 8; i++) {
            float angle = (float) MathUtils.getRandom().nextFloat() * 360f;
            float speed = MathUtils.getRandomNumberInRange(40f, 120f);
            Vector2f vel = MathUtils.getPoint(new Vector2f(), speed, angle);

            float size = MathUtils.getRandomNumberInRange(8f, 18f);
            float alpha = MathUtils.getRandomNumberInRange(0.5f, 1f);
            float dur = MathUtils.getRandomNumberInRange(0.7f, 1.5f);

            Color particleColor = new Color(
                    GOLD_PARTICLE.getRed(),
                    GOLD_PARTICLE.getGreen(),
                    GOLD_PARTICLE.getBlue(),
                    (int) (alpha * 255f)
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
    private void dissolveProjectilesAndMissiles(ShipAPI ship, CombatEngineAPI engine) {
        // Get all projectiles and missiles within a certain range of the ship (250f)
        float dissolveRadius = 250f;

        // Separate lists for projectiles and missiles
        List<DamagingProjectileAPI> projectilesInRange = new ArrayList<>();
        List<MissileAPI> missilesInRange = new ArrayList<>();

        // Loop through all projectiles
        for (DamagingProjectileAPI projectile : engine.getProjectiles()) {
            if (MathUtils.getDistance(projectile.getLocation(), ship.getLocation()) <= dissolveRadius) {
                projectilesInRange.add(projectile);
            }
        }

        // Loop through all missiles
        for (MissileAPI missile : engine.getMissiles()) {
            if (MathUtils.getDistance(missile.getLocation(), ship.getLocation()) <= dissolveRadius) {
                missilesInRange.add(missile);
            }
        }

        // Dissolve projectiles
        for (DamagingProjectileAPI projectile : projectilesInRange) {
            dissolveProjectile(ship, projectile, engine);
        }

        // Dissolve missiles
        for (MissileAPI missile : missilesInRange) {
            dissolveMissile(ship, missile, engine);
        }
    }

    // Function to dissolve a projectile
    private void dissolveProjectile(ShipAPI ship, DamagingProjectileAPI projectile, CombatEngineAPI engine) {
        // Randomize when the projectile gets dissolved (before or after the 250f mark)
        float distanceToShip = MathUtils.getDistance(projectile.getLocation(), ship.getLocation());
        if (MathUtils.getRandom().nextFloat() < (distanceToShip / 250f)) {
            engine.removeEntity(projectile);  // Remove the projectile
            generateDissolvingParticles(projectile.getLocation(), engine);  // Create particle effects
        }
    }

    // Function to dissolve a missile
    private void dissolveMissile(ShipAPI ship, MissileAPI missile, CombatEngineAPI engine) {
        // Randomize when the missile gets dissolved (before or after the 250f mark)
        float distanceToShip = MathUtils.getDistance(missile.getLocation(), ship.getLocation());
        if (MathUtils.getRandom().nextFloat() < (distanceToShip / 250f)) {
            engine.removeEntity(missile);  // Remove the missile
            generateDissolvingParticles(missile.getLocation(), engine);  // Create particle effects
        }
    }


    private void generateDissolvingParticles(Vector2f location, CombatEngineAPI engine) {
        // Flash effect (optional, but adds punch)
        Color flashColor = new Color(255, 220, 100, 255);
        engine.spawnExplosion(location, new Vector2f(), flashColor, 20f, 0.2f);
        int particleCount = MathUtils.getRandom().nextInt(4, 10); // Fewer particles for less clutter
        for (int i = 0; i < particleCount; i++) {
            float angle = MathUtils.getRandom().nextFloat() * 360f;
            float speed = MathUtils.getRandomNumberInRange(20f, 60f); // Quick zip
            Vector2f velocity = MathUtils.getPoint(new Vector2f(), speed, angle);

            float size = MathUtils.getRandomNumberInRange(5f, 10f); // Compact
            float duration = MathUtils.getRandomNumberInRange(0.25f, 0.5f); // Short-lived
            float intensity = 1.8f;

            Color color = new Color(
                    255,
                    MathUtils.getRandomNumberInRange(200, 230),
                    100 + MathUtils.getRandom().nextInt(50),
                    200 + MathUtils.getRandom().nextInt(55)
            );

            engine.addSmoothParticle(
                    location,
                    velocity,
                    size,
                    intensity,
                    duration,
                    color
            );
        }
    }
    private float getGauge(ShipAPI ship) {
        Object data = ship.getCustomData().get(GAUGE_KEY);
        return (data instanceof Float) ? (Float) data : 0f;
    }
}
