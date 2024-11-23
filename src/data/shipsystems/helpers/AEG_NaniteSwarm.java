package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AEG_NaniteSwarm {

    private static final float EMP_RADIUS = 1000f;
    private static final float DEBUFF_DURATION = 10f; // Duration of debuffs on ships
    private static final float TARGET_RANGE = 100f; // Target weapon range
    private static final int MAX_PARTICLES = 20; // Maximum number of particles
    private static final float MAX_PARTICLE_SIZE = 5f; // Maximum particle size

    private static boolean isActive = false;
    private static final List<ShipAPI> affectedShips = new ArrayList<ShipAPI>();
    private static final Random random = new Random();

    public static void execute(ShipAPI ship, String id) {
        isActive = true;
        affectedShips.clear();

        CombatEngineAPI engine = Global.getCombatEngine();

        // Emit nanite swarm
        emitNaniteSwarm(ship, engine, id);
    }

    private static void emitNaniteSwarm(ShipAPI ship, CombatEngineAPI engine, String id) {
        List<CombatEntityAPI> targets = getTargetsInRange(engine, ship.getLocation());

        // Apply debuffs to enemy ships only
        for (CombatEntityAPI target : targets) {
            if (target instanceof ShipAPI && target != ship && !affectedShips.contains(target)) {
                applySpecialEffects((ShipAPI) target, id);
            }
        }
    }

    private static List<CombatEntityAPI> getTargetsInRange(CombatEngineAPI engine, Vector2f point) {
        List<CombatEntityAPI> result = new ArrayList<CombatEntityAPI>();
        for (CombatEntityAPI entity : engine.getShips()) {
            if (MathUtils.getDistance(point, entity.getLocation()) <= AEG_NaniteSwarm.EMP_RADIUS) {
                result.add(entity);
            }
        }
        return result;
    }

    private static void applySpecialEffects(final ShipAPI target, final String id) {
        if (affectedShips.contains(target)) {
            return; // Do not apply effects if already affected
        }

        affectedShips.add(target);

        // Shield Disruption
        if (target.getShield() != null) {
            target.getShield().toggleOff();
        }

        // Set weapon range to 100 units
        float currentRange = target.getMutableStats().getWeaponRangeThreshold().getBaseValue();
        float reductionAmount = currentRange - TARGET_RANGE;
        target.getMutableStats().getWeaponRangeThreshold().modifyFlat(id, -reductionAmount);

        // Slow weapon rate of fire
        target.getMutableStats().getBallisticRoFMult().modifyMult(id, 0.5f);
        target.getMutableStats().getEnergyRoFMult().modifyMult(id, 0.5f);
        target.getMutableStats().getMissileRoFMult().modifyMult(id, 0.5f);

        // Schedule critical malfunctions and removal of debuffs
        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            private float elapsed = 0f;
            private float malfunctionTimer = 0f;
            private int particleCount = 0;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (Global.getCombatEngine().isPaused()) {
                    return;
                }

                elapsed += amount;
                malfunctionTimer += amount;

                // Create visual effect
                if (particleCount < MAX_PARTICLES) {
                    createParticleEffect(target);
                    particleCount++;
                }

                if (malfunctionTimer >= 2f) {
                    malfunctionTimer = 0f;
                    target.getEngineController().forceFlameout();
                    for (WeaponAPI weapon : target.getAllWeapons()) {
                        weapon.disable(true);
                    }
                }

                if (elapsed >= DEBUFF_DURATION) {
                    removeSpecialEffects(target, id);
                    Global.getCombatEngine().removePlugin(this);
                }
            }
        });
    }

    private static void createParticleEffect(ShipAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f location = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius());
        float size = random.nextFloat() * MAX_PARTICLE_SIZE;
        float lifetime = 0.5f + random.nextFloat() * 1.5f;
        float transparency = 0.5f + random.nextFloat() * 0.5f;
        Color color = new Color(0, random.nextInt(256), random.nextInt(256), (int) (transparency * 255));

        engine.addHitParticle(location, new Vector2f(), size, transparency, lifetime, color);
    }

    private static void removeSpecialEffects(final ShipAPI target, final String id) {
        target.getMutableStats().getWeaponRangeThreshold().unmodify(id);
        target.getMutableStats().getBallisticRoFMult().unmodify(id);
        target.getMutableStats().getEnergyRoFMult().unmodify(id);
        target.getMutableStats().getMissileRoFMult().unmodify(id);
        affectedShips.remove(target);
    }

    public static void advance(float amount) {
        if (!isActive) {
            return;
        }

        // Remove debuffs from all ships within the radius
        List<CombatEntityAPI> targets = getTargetsInRange(Global.getCombatEngine(), Global.getCombatEngine().getPlayerShip().getLocation());
        for (CombatEntityAPI target : targets) {
            if (target instanceof ShipAPI) {
                removeSpecialEffects((ShipAPI) target, "AEG_NaniteSwarm");
            }
        }
    }
}