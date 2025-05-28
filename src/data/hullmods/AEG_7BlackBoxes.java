package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.util.IntervalUtil;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import java.util.Map;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AEG_7BlackBoxes extends BaseHullMod {
    private final java.util.Map<ShipAPI, Float> evasionEffectCooldown = new java.util.WeakHashMap<>();
    private static final float PREDICTION_EFFECT_COOLDOWN = 5f; // Adjust as needed

    private final java.util.Map<ShipAPI, EvasionTracker> evasionMap = new java.util.WeakHashMap<>();
    private final IntervalUtil lastStandCooldownTimer = new IntervalUtil(60f, 60f); // 1-minute cooldown
    private static final float REGENERATION_RATE = 0.01f;
    private static final float ASSIMILATION_DAMAGE_CONVERSION = 0.05f;
    private static final float STRENGTHENING_MULT = 1.05f;
    private static final float PREDICTION_EVASION_CHANCE = 0.1f;
    private static final float ADAPTIVE_DEFENSE_DURATION = 10f; // Reduced from 20f
    private static final float ADAPTIVE_DEFENSE_REDUCTION = 0.1f; // Reduced from 0.75f
    private static final float ADAPTIVE_DEFENSE_COOLDOWN = 5f; // Added cooldown
    private static final float DAMAGE_REDUCTION_DURATION = 5f; // Duration for reduced damage effect

    private final IntervalUtil adaptiveDefenseTimer = new IntervalUtil(ADAPTIVE_DEFENSE_DURATION, ADAPTIVE_DEFENSE_DURATION + ADAPTIVE_DEFENSE_COOLDOWN);
    private final IntervalUtil damageReductionTimer = new IntervalUtil(DAMAGE_REDUCTION_DURATION, DAMAGE_REDUCTION_DURATION);
    private boolean lastStandTriggered = false; // Flag to track if Last Stand Protocol has been triggered
    private boolean damageReductionActive = false; // Flag to track if damage reduction is active
    // Declare jitter variables
    private boolean justEvadedDamage = false;
    @Override
    public void advanceInCombat(final ShipAPI ship, float amount) {

        updateCooldown(evasionEffectCooldown, ship, amount);

        if (ship == null || !ship.isAlive() || ship.isHulk() || ship.isPiece()) {
            unapplyStrengthening(ship.getMutableStats());
            unapplyAssimilation(ship.getMutableStats());
            unapplyAdaptiveDefense(ship.getMutableStats());
            // Cooldown timer update for evasion effect
            return;
        }

        // Black Box 1 Continuous Regeneration
        if (ship.getHullLevel() < 1.0f) {
            float newHitpoints = ship.getHitpoints() + (REGENERATION_RATE * ship.getMaxHitpoints() * amount);
            ship.setHitpoints(Math.min(newHitpoints, ship.getMaxHitpoints()));
        }

        // Black Box 2 Assimilation
        applyAssimilation(ship.getMutableStats());

        // Black Box 3 Strengthening
        applyStrengthening(ship.getMutableStats());


        // Black Box 4 Dimensional Prediction
        // Only add the damage listener once
        if (ship.getCustomData().get("AEG_7BlackBoxes_EvasionListener") == null) {
            ship.setCustomData("AEG_7BlackBoxes_EvasionListener", true);
            ship.addListener(new DamageTakenModifier() {
                @Override
                public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
                    if (target instanceof ShipAPI && target == ship && Math.random() < PREDICTION_EVASION_CHANCE) {
                        damage.getModifier().modifyMult("AEG_7BlackBoxes", 0f); // Negate the damage

                        // Always trigger jitter
                        triggerJitterOnly(ship);

                        // Only trigger sway+teleport if cooldown expired
                        if (!evasionEffectCooldown.containsKey(ship)) {
                            triggerSwayEffect(ship);
                            evasionEffectCooldown.put(ship, PREDICTION_EFFECT_COOLDOWN);
                        }

                        return "AEG_7BlackBoxes";
                    }
                    return null;
                }
            });
        }
        // Improved sway
        if (evasionMap.containsKey(ship)) {
            EvasionTracker tracker = evasionMap.get(ship);
            tracker.timeElapsed += amount;

            if (!tracker.returning && tracker.timeElapsed >= tracker.delay) {
                tracker.returning = true;
                tracker.timeElapsed = 0f; // Reset timer for return phase
            }

            if (tracker.returning) {
                float progress = tracker.timeElapsed / tracker.returnDuration;
                progress = Math.min(progress, 1f);

                // LERP back to original position
                Vector2f current = ship.getLocation();
                float x = current.x + (tracker.startPosition.x - current.x) * progress;
                float y = current.y + (tracker.startPosition.y - current.y) * progress;
                ship.getLocation().set(x, y);

                // Interpolate back to original facing
                float restoredAngle = currentAngleLerp(ship.getFacing(), ship.getFacing() - tracker.angularOffset, progress);
                ship.setFacing(restoredAngle);

                if (progress >= 1f) {
                    evasionMap.remove(ship);
                }
            }
        }


        // Black Box 5 Adaptive Defense
        if (adaptiveDefenseTimer.intervalElapsed()) {
            applyAdaptiveDefense(ship.getMutableStats());
        } else {
            unapplyAdaptiveDefense(ship.getMutableStats());
        }

        // Black Box 6 Causality Weapon (Black Box 7 is the System)
        // Last Stand Protocol logic - trigger once every minute
        lastStandCooldownTimer.advance(amount); // Advance the cooldown timer

        // Trigger Last Stand Protocol if health is below 10% and cooldown has passed, or trigger it immediately the first time
        if ((ship.getHullLevel() <= 0.10f || ship.getHitpoints() <= ship.getMaxHitpoints() * 0.10f)
                && (!lastStandTriggered || lastStandCooldownTimer.intervalElapsed())) {
            // Trigger Last Stand Protocol immediately if conditions are met
            Global.getLogger(this.getClass()).info("Last Stand Protocol triggered");

            // Heal the ship to 25% health (after last stand activation)
            ship.setHitpoints(ship.getMaxHitpoints() * 0.30f);

            // Begin AOE effect
            triggerLastStandAOE(ship);

            // ✅ Activate damage reduction
            damageReductionActive = true;
            damageReductionTimer.forceIntervalElapsed(); // Reset and start the timer

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
//Black Box Strengthening Helper
    private void applyStrengthening(MutableShipStatsAPI stats) {
        String id = "AEG_7BlackBoxes";

        if (!stats.getBallisticWeaponDamageMult().getMultMods().containsKey(id)) {
            stats.getBallisticWeaponDamageMult().modifyMult(id, STRENGTHENING_MULT);
            stats.getEnergyWeaponDamageMult().modifyMult(id, STRENGTHENING_MULT);
            stats.getMissileWeaponDamageMult().modifyMult(id, STRENGTHENING_MULT);

            stats.getMaxSpeed().modifyMult(id, STRENGTHENING_MULT);
            stats.getAcceleration().modifyMult(id, STRENGTHENING_MULT);
            stats.getDeceleration().modifyMult(id, STRENGTHENING_MULT);
            stats.getTurnAcceleration().modifyMult(id, STRENGTHENING_MULT);
            stats.getMaxTurnRate().modifyMult(id, STRENGTHENING_MULT);
        }
    }
    //Black Box Strengthening Remover Helper
    private void unapplyStrengthening(MutableShipStatsAPI stats) {
        String id = "AEG_7BlackBoxes";

        stats.getBallisticWeaponDamageMult().unmodify(id);
        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getMissileWeaponDamageMult().unmodify(id);

        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
    }
    //BlackBox Assimilation Apply Helper
    private void applyAssimilation(MutableShipStatsAPI stats) {
        String id = "AEG_7BlackBoxes_Assimilation";

        if (!stats.getEnergyWeaponDamageMult().getMultMods().containsKey(id)) {
            stats.getEnergyWeaponDamageMult().modifyMult(id, 1 + ASSIMILATION_DAMAGE_CONVERSION);
            stats.getHardFluxDissipationFraction().modifyFlat(id, ASSIMILATION_DAMAGE_CONVERSION);
        }
    }
    //BlackBox Assimilation Remover Helper
    private void unapplyAssimilation(MutableShipStatsAPI stats) {
        String id = "AEG_7BlackBoxes_Assimilation";
        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getHardFluxDissipationFraction().unmodify(id);
    }
    private void applyAdaptiveDefense(MutableShipStatsAPI stats) {
        String id = "AEG_7BlackBoxes_AdaptiveDefense";
        stats.getHullDamageTakenMult().modifyMult(id, ADAPTIVE_DEFENSE_REDUCTION);
        stats.getArmorDamageTakenMult().modifyMult(id, ADAPTIVE_DEFENSE_REDUCTION);
    }

    private void unapplyAdaptiveDefense(MutableShipStatsAPI stats) {
        String id = "AEG_7BlackBoxes_AdaptiveDefense";
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);
    }

    //Dimensional Prediction Evasion Helper
    private void triggerJitterOnly(ShipAPI ship) {
        ship.setJitter(
                ship,
                new Color(255, 200 - MathUtils.getRandom().nextInt(100), 0),
                0.5f + MathUtils.getRandom().nextFloat(),
                5 + MathUtils.getRandom().nextInt(2),
                0f,
                100f
        );
        Global.getCombatEngine().addFloatingText(ship.getLocation(), "Dimensional Prediction!", 16f, Color.YELLOW, ship, 0.5f, 1.0f);
    }
    private void triggerSwayEffect(final ShipAPI ship) {
        float swayAngle = ship.getFacing() + (Math.random() > 0.5 ? 90f : -90f);
        Vector2f offset = MathUtils.getPointOnCircumference(null, 40f, swayAngle);
        Vector2f newPos = Vector2f.add(ship.getLocation(), offset, new Vector2f());

        ship.getLocation().set(newPos.x, newPos.y);

        float angleOffset = (Math.random() > 0.5f ? 1 : -1) * 5f;
        ship.setFacing(ship.getFacing() + angleOffset);

        evasionMap.put(ship, new EvasionTracker(offset, ship.getLocation(), angleOffset));
    }

    //Evasion Tracker helper for Smooth Sway
    private static class EvasionTracker {
        Vector2f direction;         // Initial dodge direction
        Vector2f startPosition;     // Where the dodge began
        float angularOffset;        // Facing offset to return
        float timeElapsed = 0f;
        float delay = 0.3f;         // Time before reverse starts
        float returnDuration = 0.3f; // Time to return to position
        boolean returning = false;

        public EvasionTracker(Vector2f direction, Vector2f currentPos, float angularOffset) {
            this.direction = direction;
            this.startPosition = new Vector2f(currentPos);
            this.angularOffset = angularOffset;
        }
    }
    //Evasion Helper Method
    private float currentAngleLerp(float from, float to, float progress) {
        float shortest = ((((to - from) % 360) + 540) % 360) - 180;
        return from + shortest * progress;
    }
    //Evasion Cooldown for FLicker and sway
    private void updateCooldown(Map<ShipAPI, Float> map, ShipAPI ship, float amount) {
        if (map.containsKey(ship)) {
            float newTime = map.get(ship) - amount;
            if (newTime <= 0f) {
                map.remove(ship);
            } else {
                map.put(ship, newTime);
            }
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
        int index = Math.max(0, Math.min(energyColors.length - 1, (int)(progress * (energyColors.length - 1))));
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
