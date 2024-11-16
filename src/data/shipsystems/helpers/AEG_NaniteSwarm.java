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
    private static final float ORB_INTERVAL = 1f; // Interval for orb updates
    private static final Color ORB_COLOR = new Color(105, 255, 105, 255);
    private static final float DISSIPATION_TIME = 1f; // 1 second dissipation time
    private static final float DEBUFF_DURATION = 20f; // Duration of debuffs on ships

    private static boolean isActive = false;
    private static float activeTime = 0f;
    private static List<ShipAPI> affectedShips = new ArrayList<>();

    public static void execute(ShipAPI ship, String id) {
        isActive = true;
        activeTime = 0f;
        affectedShips.clear();

        CombatEngineAPI engine = Global.getCombatEngine();

        // Emit nanite swarm
        emitNaniteSwarm(ship, engine, id);
    }

    private static void emitNaniteSwarm(ShipAPI ship, CombatEngineAPI engine, String id) {
        List<CombatEntityAPI> targets = getTargetsInRange(engine, ship.getLocation(), EMP_RADIUS);

        // Apply debuffs to enemy ships only
        for (CombatEntityAPI target : targets) {
            if (target instanceof ShipAPI && target != ship) {
                applySpecialEffects((ShipAPI) target, id);
            }
        }

        // Create visual effects
        createVisualEffects(ship, targets);
    }

    private static List<CombatEntityAPI> getTargetsInRange(CombatEngineAPI engine, Vector2f point, float range) {
        List<CombatEntityAPI> result = new ArrayList<>();
        for (CombatEntityAPI entity : engine.getShips()) {
            if (MathUtils.getDistance(point, entity.getLocation()) <= range) {
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

        // Sensor Scramble
        target.getMutableStats().getSensorProfile().modifyMult(id, 2f);

        // Energy Drain
        target.getMutableStats().getEnergyWeaponDamageMult().modifyMult(id, 0.5f);

        // Schedule removal of debuffs after DEBUFF_DURATION
        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            private float elapsed = 0f;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (Global.getCombatEngine().isPaused()) {
                    return;
                }

                elapsed += amount;
                if (elapsed >= DEBUFF_DURATION) {
                    removeSpecialEffects(target, id);
                    Global.getCombatEngine().removePlugin(this);
                }
            }
        });
    }

    private static void removeSpecialEffects(final ShipAPI target, final String id) {
        target.getMutableStats().getSensorProfile().unmodify(id);
        target.getMutableStats().getEnergyWeaponDamageMult().unmodify(id);
        affectedShips.remove(target);
    }

    private static void createVisualEffects(final ShipAPI ship, final List<CombatEntityAPI> targets) {
        final CombatEngineAPI engine = Global.getCombatEngine();

        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            private float elapsed = 0f;
            private final Random random = new Random();

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (Global.getCombatEngine().isPaused() || !isActive) {
                    return;
                }

                elapsed += amount;
                if (elapsed >= ORB_INTERVAL) {
                    elapsed = 0f;

                    // Check for ships in the radius and target them
                    boolean targetFound = false;
                    for (CombatEntityAPI target : targets) {
                        if (target instanceof ShipAPI && !affectedShips.contains(target)) {
                            // Create particle effects on impact
                            for (int i = 0; i < 10; i++) {
                                Vector2f particlePos = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius());
                                engine.addHitParticle(particlePos, new Vector2f(), 5f + random.nextFloat() * 10f, 1f, 0.5f, ORB_COLOR);
                            }
                            applySpecialEffects((ShipAPI) target, "AEG_NaniteSwarm");
                            targetFound = true;
                            break; // Only strike one ship per interval
                        }
                    }

                    // If no targets found, dissipate particles
                    if (!targetFound) {
                        for (int i = 0; i < 10; i++) {
                            Vector2f particlePos = MathUtils.getRandomPointInCircle(ship.getLocation(), 200);
                            engine.addHitParticle(particlePos, new Vector2f(), 5f + random.nextFloat() * 10f, 1f, 0.5f, ORB_COLOR);
                        }
                    }
                }
            }
        });
    }

    public static void advance(float amount) {
        if (!isActive) {
            return;
        }

        activeTime += amount;
        if (activeTime >= DISSIPATION_TIME) {
            isActive = false;

            // Remove debuffs from all ships within the radius
            List<CombatEntityAPI> targets = getTargetsInRange(Global.getCombatEngine(), Global.getCombatEngine().getPlayerShip().getLocation(), EMP_RADIUS);
            for (CombatEntityAPI target : targets) {
                if (target instanceof ShipAPI) {
                    removeSpecialEffects((ShipAPI) target, "AEG_NaniteSwarm");
                }
            }
        }
    }
}