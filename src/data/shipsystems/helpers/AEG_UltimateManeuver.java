package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class AEG_UltimateManeuver {

    private static final float DURATION = 10f;
    private static final float DAMAGE_REDUCTION = 0.01f; // 99% damage reduction
    private static final float BLACK_HOLE_RADIUS = 2500f;
    private static final float RING_RADIUS = 205f;
    private static final float INNER_RING_RADIUS = 200f;
    private static final float PULL_STRENGTH = 300f;
    private static final Color RING_COLOR = new Color(185, 0, 255); // Bright magenta
    private static final Color INNER_RING_COLOR = new Color(255, 140, 0); // Gradient orange
    private static final Color EXPLOSION_COLOR = new Color(105, 255, 105); // White with green fringe
    private static final Color EXPLOSION_FRINGE_COLOR = new Color(25, 153, 25);
    private static final Random RANDOM = new Random();

    private static Vector2f blackHolePosition;
    private static boolean isActive = false;
    private static boolean explosionOccurred = false;
    private static float elapsedTime = 0f;
    private static WeaponRepairHelper repairHelper;

    public static void execute(final ShipAPI ship, final String id) {
        if (isActive) {
            return; // Prevent reactivation while already active
        }

        isActive = true;
        elapsedTime = 0f;
        explosionOccurred = false;

        // Lock arms and shoulders
        AEG_MeteorSmash.initializePositions(ship);
        setWeaponAngles(ship);

        // Determine black hole position
        blackHolePosition = MathUtils.getPointOnCircumference(ship.getLocation(), 2000f, ship.getFacing());

        // Apply visual effects
        applyVisualEffects(ship);

        // Reduce incoming damage
        ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, DAMAGE_REDUCTION);
        ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, DAMAGE_REDUCTION);
        ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, DAMAGE_REDUCTION);

        // Lock ship in place
        ship.getMutableStats().getMaxSpeed().modifyMult(id, 0f);
        ship.getMutableStats().getAcceleration().modifyMult(id, 0f);
        ship.getMutableStats().getDeceleration().modifyMult(id, 0f);

        // Add plugin to handle the effect over time
        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            @Override
            public void advance(float amount, List events) {
                if (Global.getCombatEngine().isPaused() || !isActive) {
                    return;
                }

                elapsedTime += amount;
                if (elapsedTime >= DURATION + 1f) { // Reset timer at the 11th second
                    if (!explosionOccurred) {
                        endEffect(ship, id);
                        explosionOccurred = true;
                    }
                    Global.getCombatEngine().removePlugin(this);
                    return;
                }

                // Apply black hole pull effect
                applyBlackHolePull();

                // Create particles moving towards the black hole
                createBlackHoleParticles();
            }
        });

        // Remove the repair helper if it exists
        if (repairHelper != null) {
            Global.getCombatEngine().removePlugin(repairHelper);
        }
    }

    private static void setWeaponAngles(ShipAPI ship) {
        float shipFacing = ship.getFacing();
        for (WeaponAPI w : ship.getAllWeapons()) {
            switch (w.getSlot().getId()) {
                case "WS0003":
                    w.setCurrAngle(shipFacing + AEG_MeteorSmash.TARGET_LEFT_ARM_ANGLE);
                    break;
                case "WS0004":
                    w.setCurrAngle(shipFacing + AEG_MeteorSmash.TARGET_RIGHT_ARM_ANGLE);
                    break;
                case "WS0001":
                    w.setCurrAngle(shipFacing + AEG_MeteorSmash.TARGET_LEFT_SHOULDER_ANGLE);
                    break;
                case "WS0002":
                    w.setCurrAngle(shipFacing + AEG_MeteorSmash.TARGET_RIGHT_SHOULDER_ANGLE);
                    break;
            }
        }
    }

    private static void applyVisualEffects(ShipAPI ship) {
        if (blackHolePosition == null) {
            return; // Ensure blackHolePosition is not null
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null) {
            // Add central ring effect
            engine.addHitParticle(blackHolePosition, new Vector2f(), RING_RADIUS, 1f, DURATION, RING_COLOR);
            // Add inner ring effect
            engine.addHitParticle(blackHolePosition, new Vector2f(), INNER_RING_RADIUS, 1f, DURATION, INNER_RING_COLOR);

            // Create the wavy magenta ring
            createWavyParticleRing(engine, blackHolePosition, RING_RADIUS, 258, RING_COLOR);

            // Create the thicker gradient orange ring
            createGradientParticleRing(engine, blackHolePosition, INNER_RING_RADIUS, 251);
        }
    }

    private static void createWavyParticleRing(CombatEngineAPI engine, Vector2f center, float radius, int particleCount, Color color) {
        for (int i = 0; i < particleCount; i++) {
            float angle = (float) (i * 2 * Math.PI / particleCount);
            float noise = (RANDOM.nextFloat() - 0.5f) * 10f; // Add some noise for wavy effect
            float x = center.x + (radius + noise) * (float) Math.cos(angle);
            float y = center.y + (radius + noise) * (float) Math.sin(angle);
            engine.addHitParticle(new Vector2f(x, y), new Vector2f(0, 0), 5f, 1f, DURATION, color);
        }
    }

    private static void createGradientParticleRing(CombatEngineAPI engine, Vector2f center, float radius, int particleCount) {
        for (int i = 0; i < particleCount; i++) {
            float angle = (float) (i * 2 * Math.PI / particleCount);
            float x = center.x + radius * (float) Math.cos(angle);
            float y = center.y + radius * (float) Math.sin(angle);
            Color color = getGradientColor(i, particleCount);
            engine.addHitParticle(new Vector2f(x, y), new Vector2f(0, 0), 10f, 1f, DURATION, color); // Increased size to 10f
        }
    }

    private static Color getGradientColor(int index, int total) {
        float ratio = (float) index / total;
        int red = (int) (255 * ratio + 255 * (1 - ratio));
        int green = (int) (165 * ratio + 69 * (1 - ratio));
        int blue = (int) (0 * ratio + 0 * (1 - ratio));
        return new Color(red, green, blue);
    }

    private static void applyBlackHolePull() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || blackHolePosition == null) {
            return; // Ensure engine and blackHolePosition are not null
        }

        for (CombatEntityAPI entity : engine.getShips()) {
            if (entity instanceof ShipAPI && entity != engine.getPlayerShip()) {
                float distance = MathUtils.getDistance(entity, blackHolePosition);
                if (distance <= BLACK_HOLE_RADIUS) {
                    Vector2f pullVector = VectorUtils.getDirectionalVector(entity.getLocation(), blackHolePosition);
                    float strength = (1f - distance / BLACK_HOLE_RADIUS) * PULL_STRENGTH;
                    pullVector.scale(strength);
                    Vector2f.add(entity.getVelocity(), pullVector, entity.getVelocity());
                }
            }
        }
    }
    private static void createBlackHoleParticles() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || blackHolePosition == null) {
            return; // Ensure engine and blackHolePosition are not null
        }

        for (int i = 0; i < 10; i++) { // Increase particle frequency
            Vector2f particlePos = MathUtils.getRandomPointInCircle(blackHolePosition, BLACK_HOLE_RADIUS);
            Vector2f particleVel = Vector2f.sub(blackHolePosition, particlePos, null);
            particleVel.scale(0.05f); // Slow down the particles
            float size = 20f + (float) Math.random() * 10f; // Increase particle size
            float transparency = 0.5f + (float) Math.random() * 0.5f; // Random transparency
            engine.addHitParticle(particlePos, particleVel, size, transparency, 1f, RING_COLOR);

            // Add smoke effect with variance
            if (Math.random() < 0.5) {
                Vector2f smokePos = MathUtils.getRandomPointInCircle(particlePos, 10f); // Add variance to smoke position
                Color smokeColor = new Color(50, 50, 50, (int) (transparency * 255));
                engine.addSmokeParticle(smokePos, particleVel, size * 1.5f, transparency, 1f, smokeColor);
            }

            // Ensure particles stop at the ring
            if (MathUtils.getDistance(particlePos, blackHolePosition) <= RING_RADIUS) {
                particleVel.set(0, 0);
            }
        }
    }

    private static void endEffect(ShipAPI ship, String id) {
        isActive = false;

        // Create final explosion
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null && blackHolePosition != null) {
            engine.spawnExplosion(blackHolePosition, new Vector2f(), EXPLOSION_COLOR, 2500f, 1f);
            engine.addHitParticle(blackHolePosition, new Vector2f(), 3000, 1f, 1f, EXPLOSION_FRINGE_COLOR);

            // Add shockwave effect
            engine.addNebulaParticle(blackHolePosition, new Vector2f(), 200f, 1.5f, 0.1f, 0.3f, 1f, EXPLOSION_COLOR);

            // Deal 10,000 high explosive damage to all entities except the player ship
            for (CombatEntityAPI entity : engine.getShips()) {
                if (entity != ship) {
                    engine.applyDamage(entity, blackHolePosition, 10000f, DamageType.HIGH_EXPLOSIVE, 5000f, true, false, ship);
                }
            }

            // Apply fatal damage to all weapons
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                weapon.disable(true);
                engine.applyDamage(ship, weapon.getLocation(), weapon.getCurrHealth() + 1, DamageType.FRAGMENTATION, 0f, false, false, ship);
            }
        }
        // Reset damage reduction
        ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
        ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
        ship.getMutableStats().getShieldDamageTakenMult().unmodify(id);

        // Reset speed and maneuverability
        ship.getMutableStats().getMaxSpeed().unmodify(id);
        ship.getMutableStats().getAcceleration().unmodify(id);
        ship.getMutableStats().getDeceleration().unmodify(id);

        // Reset arm and shoulder positions
        AEG_MeteorSmash.resetPositions(ship);

        // Add the repair helper to repair weapons over time
        repairHelper = new WeaponRepairHelper(ship);
        Global.getCombatEngine().addPlugin(repairHelper);

        // Reset the 11-second timer
        elapsedTime = 0f;

        // Deactivate the system
        ship.getSystem().deactivate();
    }

    private static class WeaponRepairHelper extends BaseEveryFrameCombatPlugin {

        private final ShipAPI ship;
        private static final float REPAIR_INTERVAL = 1f; // Repair interval in seconds
        private float elapsed = 0f;

        public WeaponRepairHelper(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount, List events) {
            if (Global.getCombatEngine().isPaused()) {
                return;
            }

            elapsed += amount;
            if (elapsed >= REPAIR_INTERVAL) {
                repairWeapons();
                elapsed = 0f;
            }
        }

        private void repairWeapons() {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.isDisabled()) {
                    weapon.repair();
                }
            }
        }
    }
}