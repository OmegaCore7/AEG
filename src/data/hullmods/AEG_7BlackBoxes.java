package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AEG_7BlackBoxes extends BaseHullMod {
    private final IntervalUtil lastStandCooldownTimer = new IntervalUtil(60f, 60f); // 1-minute cooldown
    private static final float REGENERATION_RATE = 0.01f;
    private static final float ASSIMILATION_DAMAGE_CONVERSION = 0.05f;
    private static final float STRENGTHENING_MULT = 1.05f;
    private static final float PREDICTION_EVASION_CHANCE = 0.1f;
    private static final float SENSOR_RANGE_BOOST = 200f;
    private static final float ADAPTIVE_DEFENSE_DURATION = 10f; // Reduced from 20f
    private static final float ADAPTIVE_DEFENSE_REDUCTION = 0.1f; // Reduced from 0.75f
    private static final float ADAPTIVE_DEFENSE_COOLDOWN = 5f; // Added cooldown
    private static final float DAMAGE_REDUCTION_DURATION = 5f; // Duration for reduced damage effect

    private final IntervalUtil adaptiveDefenseTimer = new IntervalUtil(ADAPTIVE_DEFENSE_DURATION, ADAPTIVE_DEFENSE_DURATION + ADAPTIVE_DEFENSE_COOLDOWN);
    private final IntervalUtil damageReductionTimer = new IntervalUtil(DAMAGE_REDUCTION_DURATION, DAMAGE_REDUCTION_DURATION);
    private boolean lastStandTriggered = false; // Flag to track if Last Stand Protocol has been triggered
    private boolean damageReductionActive = false; // Flag to track if damage reduction is active


    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null) return;

        // Continuous Regeneration
        if (ship.getHullLevel() < 1.0f) {
            float newHitpoints = ship.getHitpoints() + (REGENERATION_RATE * ship.getMaxHitpoints() * amount);
            ship.setHitpoints(Math.min(newHitpoints, ship.getMaxHitpoints()));
        }

        // Assimilation
        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("AEG_7BlackBoxes", 1 + ASSIMILATION_DAMAGE_CONVERSION);
        ship.getMutableStats().getHardFluxDissipationFraction().modifyFlat("AEG_7BlackBoxes", ASSIMILATION_DAMAGE_CONVERSION);

        // Strengthening
        ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);
        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);
        ship.getMutableStats().getMissileWeaponDamageMult().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);
        ship.getMutableStats().getMaxSpeed().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);
        ship.getMutableStats().getAcceleration().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);
        ship.getMutableStats().getDeceleration().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);
        ship.getMutableStats().getTurnAcceleration().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);
        ship.getMutableStats().getMaxTurnRate().modifyMult("AEG_7BlackBoxes", STRENGTHENING_MULT);

        // Dimensional Prediction
        if (Math.random() < PREDICTION_EVASION_CHANCE) {
            ship.getMutableStats().getHullDamageTakenMult().modifyMult("AEG_7BlackBoxes", 0f);
            ship.getMutableStats().getArmorDamageTakenMult().modifyMult("AEG_7BlackBoxes", 0f);
        }
        ship.getMutableStats().getSensorStrength().modifyFlat("AEG_7BlackBoxes", SENSOR_RANGE_BOOST);

        // Adaptive Defense
        adaptiveDefenseTimer.advance(amount);
        if (adaptiveDefenseTimer.intervalElapsed()) {
            ship.getMutableStats().getHullDamageTakenMult().modifyMult("AEG_7BlackBoxes", ADAPTIVE_DEFENSE_REDUCTION);
            ship.getMutableStats().getArmorDamageTakenMult().modifyMult("AEG_7BlackBoxes", ADAPTIVE_DEFENSE_REDUCTION);
        } else {
            ship.getMutableStats().getHullDamageTakenMult().unmodify("AEG_7BlackBoxes");
            ship.getMutableStats().getArmorDamageTakenMult().unmodify("AEG_7BlackBoxes");
        }

        // Last Stand Protocol logic - trigger once every minute
        lastStandCooldownTimer.advance(amount); // Advance the cooldown timer

// Trigger Last Stand Protocol if health is below 10% and cooldown has passed, or trigger it immediately the first time
        if ((ship.getHullLevel() <= 0.10f || ship.getHitpoints() <= ship.getMaxHitpoints() * 0.05f)
                && (!lastStandTriggered || lastStandCooldownTimer.intervalElapsed())) {
            // Trigger Last Stand Protocol immediately if conditions are met
            Global.getLogger(this.getClass()).info("Last Stand Protocol triggered");

            // Heal the ship to 25% health (after last stand activation)
            ship.setHitpoints(ship.getMaxHitpoints() * 0.30f);

            // Begin AOE effect
            triggerLastStandAOE(ship);

            // Reset the cooldown timer to prevent immediate reactivation
            lastStandCooldownTimer.advance(0); // Reset timer
            lastStandTriggered = true;  // Set the flag to true after the first activation
        }

        // Damage Reduction
        if (damageReductionActive) {
            damageReductionTimer.advance(amount);
            if (damageReductionTimer.intervalElapsed()) {
                damageReductionActive = false; // Deactivate damage reduction after the duration
            } else {
                ship.getMutableStats().getHullDamageTakenMult().modifyFlat("AEG_7BlackBoxes_DamageReduction", 0.01f);
                ship.getMutableStats().getArmorDamageTakenMult().modifyFlat("AEG_7BlackBoxes_DamageReduction", 0.01f);
            }
        } else {
            ship.getMutableStats().getHullDamageTakenMult().unmodify("AEG_7BlackBoxes_DamageReduction");
            ship.getMutableStats().getArmorDamageTakenMult().unmodify("AEG_7BlackBoxes_DamageReduction");
        }
    }
    private void triggerLastStandAOE(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        float AOE_RADIUS = 2500f;
        float DAMAGE_PERCENT = 0.90f;
        int MAX_TARGETS = 20;
// Define the radius and number of particles
        float radius = 2500f;
        int numParticles = 50;
// Spawn particles
        // Spawn particles
        for (int i = 0; i < numParticles; i++) {
            // Random angle and distance
            float angle = (float) Math.random() * 360f;
            float distance = (float) Math.random() * radius;

            // Calculate position
            float x = ship.getLocation().x + distance * (float) Math.cos(Math.toRadians(angle));
            float y = ship.getLocation().y + distance * (float) Math.sin(Math.toRadians(angle));

            // Velocity vector pointing outward
            Vector2f velocity = new Vector2f((float) Math.cos(Math.toRadians(angle)), (float) Math.sin(Math.toRadians(angle)));
            velocity.scale(50f); // Adjust speed as necessary

            // Particle size and color
            float size = 100f + (float) Math.random() * 100f; // Random size between 100 and 200
            Color color = getRandomEnergyColor(); // Get a random energy color

            // Add swirl particle
            engine.addSwirlyNebulaParticle(new Vector2f(x, y), velocity, size, 0.5f, 0.2f, 0.8f, 1.5f, color, true);
        }
        final Vector2f center = ship.getLocation();
        List<ShipAPI> potentialTargets = new ArrayList<>();

        for (ShipAPI enemy : engine.getShips()) {
            if (enemy.getOwner() == ship.getOwner() || !enemy.isAlive() || enemy.isFighter()) continue;

            float dist = MathUtils.getDistance(enemy, center);
            if (dist <= AOE_RADIUS) {
                potentialTargets.add(enemy);
            }
        }

        // Sort by priority and distance
        java.util.Collections.sort(potentialTargets, new java.util.Comparator<ShipAPI>() {
            @Override
            public int compare(ShipAPI a, ShipAPI b) {
                int priorityA = getHullSizePriority(a.getHullSize());
                int priorityB = getHullSizePriority(b.getHullSize());
                if (priorityA != priorityB) {
                    return Integer.valueOf(priorityA).compareTo(priorityB);
                }
                float distA = MathUtils.getDistance(a, center);
                float distB = MathUtils.getDistance(b, center);
                return Float.compare(distA, distB);
            }
        });

        for (int i = 0; i < Math.min(MAX_TARGETS, potentialTargets.size()); i++) {
            ShipAPI target = potentialTargets.get(i);
            Vector2f loc = target.getLocation();

            if (i == 0) {
                // First target: lethal damage
                engine.applyDamage(target, loc, target.getMaxHitpoints() * 2f, DamageType.HIGH_EXPLOSIVE, 0f, true, false, ship);
                engine.addSwirlyNebulaParticle(loc, new Vector2f(), 150f, 0.5f, 0.2f, 0.8f, 1.5f, new Color(255, 50, 50, 255), true);

                // Create WaveDistortion at target location
                WaveDistortion wave = new WaveDistortion(loc, new Vector2f());
                wave.setIntensity(30f);        // Shock strength
                wave.setSize(200f);            // Radius of ripple
                wave.setLifetime(0.4f);        // Duration of the distortion effect
                wave.setArc(0f, 360f);         // Full circle wave
                wave.fadeOutIntensity(0.5f);   // Smooth fade-out

                // Apply distortion effect
                DistortionShader.addDistortion(wave);

                engine.addFloatingText(loc, "TARGET TERMINATED", 24f, Color.RED, target, 1f, 2f);
            } else {
                // Others: crippling damage
                float damageAmount = target.getMaxHitpoints() * DAMAGE_PERCENT;

                engine.applyDamage(target, loc, damageAmount / 2, DamageType.HIGH_EXPLOSIVE, 0f, true, false, ship);
                engine.applyDamage(target, loc, damageAmount / 2, DamageType.KINETIC, 0f, false, false, ship);
                //Add Debilitating Slow
                target.getMutableStats().getMaxSpeed().modifyMult("AEG_LS_Slow", 0.1f);
                target.getMutableStats().getAcceleration().modifyMult("AEG_LS_Slow", 0.05f);
                //Jack target ship flux to 99 percent it's Max
                target.getFluxTracker().setCurrFlux(target.getFluxTracker().getMaxFlux() * 0.99f);
                // Force an overload for 3 seconds
                target.getFluxTracker().forceOverload(3f);

                engine.addSwirlyNebulaParticle(loc, new Vector2f(), 100f, 0.5f, 0.2f, 0.8f, 1.5f, new Color(255, 150, 50, 200), true);
            }
        }

        // Center explosion FX
        engine.addSwirlyNebulaParticle(center, new Vector2f(), 400f, 0.5f, 0.2f, 0.8f, 1.5f, new Color(255, 200, 100, 255), true);

        // Create WaveDistortion at the center location
        WaveDistortion centerWave = new WaveDistortion(center, new Vector2f());
        centerWave.setIntensity(50f);        // Shock strength
        centerWave.setSize(300f);            // Radius of ripple
        centerWave.setLifetime(0.6f);        // Duration of the distortion effect
        centerWave.setArc(0f, 360f);         // Full circle wave
        centerWave.fadeOutIntensity(0.5f);   // Smooth fade-out

        // Apply distortion effect at the center
        DistortionShader.addDistortion(centerWave);

        engine.addFloatingText(center, "CAUSALITY WEAPON ENGAGED", 32f, Color.RED, ship, 2f, 2f);
    }


    private int getHullSizePriority(ShipAPI.HullSize size) {
        switch (size) {
            case CAPITAL_SHIP: return 0;
            case CRUISER: return 1;
            case DESTROYER: return 2;
            case FRIGATE: return 3;
            default: return 4;
        }
    }
    // Define a color palette inspired by Mazinger Z's energy effects
    Color[] energyColors = {
            new Color(255, 50, 50), // Red
            new Color(255, 100, 50), // Orange
            new Color(255, 150, 50), // Yellow
            new Color(255, 200, 50), // Orange Yellow
            new Color(255, 255, 50), // Light Yellow
            new Color(200, 255, 50)  // Light Green
    };

    // Function to get a random energy color
    public Color getRandomEnergyColor() {
        return energyColors[(int) (Math.random() * energyColors.length)];
    }
    // Function to generate a gradient color effect
    public Color getGradientColor(float progress) {
        int index = (int) (progress * (energyColors.length - 1));
        return energyColors[index];
    }

    //Make sure Last stand cool-down resets each combat so it won't fail to trigger next combat.
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // Ensure Last Stand is ready at the start of each combat session
        lastStandCooldownTimer.forceIntervalElapsed(); // Reset the cooldown timer
        lastStandTriggered = false;  // Reset the flag, allowing Last Stand to trigger again
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        switch (index) {
            case 0:
                return "Black Box 1: Continuous hull regeneration.";
            case 1:
                return "Black Box 2: Converts incoming damage into a damage boost and converts hard flux to soft flux.";
            case 2:
                return "Black Box 3: Increases all ship attributes.";
            case 3:
                return "Black Box 4: Chance to evade incoming attacks and increases sensor range.";
            case 4:
                return "Black Box 5: Boosts damage reduction against a specific weapon type for 10 seconds after being hit by it, with a 5-second cooldown.";
            case 5:
                return "Black Box 6: One-time ability to destroy the attacking ship and recover 25% hull when the ship would be destroyed.";
            default:
                return null;
        }
    }
}
