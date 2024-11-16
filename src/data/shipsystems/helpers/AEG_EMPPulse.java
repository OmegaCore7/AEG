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

public class AEG_EMPPulse {

    private static final float EMP_RADIUS = 1000f;
    private static final float ARC_INTERVAL = 3f; // Interval for EMP arcs
    private static final Color EMP_CORE_COLOR = new Color(105, 255, 105, 255);
    private static final Color EMP_FRINGE_COLOR = new Color(0, 100, 0, 255);
    private static final float DISSIPATION_TIME = 1f; // 1 second dissipation time

    private static boolean isActive = false;
    private static float activeTime = 0f;

    public static void execute(ShipAPI ship, String id) {
        isActive = true;
        activeTime = 0f;

        CombatEngineAPI engine = Global.getCombatEngine();

        // Emit EMP pulse
        emitEMPPulse(ship, engine, id);
    }

    private static void emitEMPPulse(ShipAPI ship, CombatEngineAPI engine, String id) {
        List<CombatEntityAPI> targets = getTargetsInRange(engine, ship.getLocation(), EMP_RADIUS);

        for (CombatEntityAPI target : targets) {
            if (target instanceof ShipAPI && target != ship) { // Ensure player ship is unaffected
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
        // Flux Overload
        target.getFluxTracker().increaseFlux(target.getFluxTracker().getMaxFlux() * 0.25f, true);

        // Shield Disruption
        if (target.getShield() != null) {
            target.getShield().toggleOff();
        }

        // Sensor Scramble
        target.getMutableStats().getSensorProfile().modifyMult(id, 2f);

        // Energy Drain
        target.getMutableStats().getEnergyWeaponDamageMult().modifyMult(id, 0.5f);
    }

    private static void removeSpecialEffects(final ShipAPI target, final String id) {
        target.getMutableStats().getSensorProfile().unmodify(id);
        target.getMutableStats().getEnergyWeaponDamageMult().unmodify(id);
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
                if (elapsed >= ARC_INTERVAL) {
                    elapsed = 0f;
                    int alpha = random.nextInt(156) + 100; // Random transparency between 100 and 255
                    Color coreColor = new Color(EMP_CORE_COLOR.getRed(), EMP_CORE_COLOR.getGreen(), EMP_CORE_COLOR.getBlue(), alpha);
                    Color fringeColor = new Color(EMP_FRINGE_COLOR.getRed(), EMP_FRINGE_COLOR.getGreen(), EMP_FRINGE_COLOR.getBlue(), alpha);
                    float thickness = 2f + random.nextFloat() * 18f; // Random thickness between 2 and 20

                    // Visual effects on the target ships
                    for (CombatEntityAPI target : targets) {
                        if (target instanceof ShipAPI) {
                            Vector2f point = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius());
                            engine.spawnEmpArcVisual(target.getLocation(), target, point, target, thickness, coreColor, fringeColor);
                        }
                    }

                    // Visual effects on the player ship
                    Vector2f point = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius());
                    engine.spawnEmpArcVisual(ship.getLocation(), ship, point, ship, thickness, coreColor, fringeColor);

                    // Create green lightning arcs from the ship to the outer ring at cardinal directions
                    for (int i = 0; i < 4; i++) {
                        float angle = (float) (Math.PI / 2 * i);
                        Vector2f ringPoint = new Vector2f(
                                (float) (ship.getLocation().x + EMP_RADIUS * Math.cos(angle)),
                                (float) (ship.getLocation().y + EMP_RADIUS * Math.sin(angle))
                        );
                        engine.spawnEmpArcVisual(ship.getLocation(), ship, ringPoint, ship, thickness, Color.GREEN, Color.GREEN);
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
                    removeSpecialEffects((ShipAPI) target, "AEG_EMPPulse");
                }
            }
        }
    }
}