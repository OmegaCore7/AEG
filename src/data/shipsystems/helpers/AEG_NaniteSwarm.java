package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class AEG_NaniteSwarm {

    private static final float EMP_RADIUS = 1000f;
    private static final float DISSIPATION_TIME = 1f; // 1 second dissipation time
    private static final float DEBUFF_DURATION = 20f; // Duration of debuffs on ships

    private static boolean isActive = false;
    private static float activeTime = 0f;
    private static final List<ShipAPI> affectedShips = new ArrayList<>();

    public static void execute(ShipAPI ship, String id) {
        isActive = true;
        activeTime = 0f;
        affectedShips.clear();

        CombatEngineAPI engine = Global.getCombatEngine();

        // Emit nanite swarm
        emitNaniteSwarm(ship, engine, id);
    }

    private static void emitNaniteSwarm(ShipAPI ship, CombatEngineAPI engine, String id) {
        List<CombatEntityAPI> targets = getTargetsInRange(engine, ship.getLocation());

        // Apply debuffs to enemy ships only
        for (CombatEntityAPI target : targets) {
            if (target instanceof ShipAPI && target != ship) {
                applySpecialEffects((ShipAPI) target, id);
            }
        }
    }

    private static List<CombatEntityAPI> getTargetsInRange(CombatEngineAPI engine, Vector2f point) {
        List<CombatEntityAPI> result = new ArrayList<>();
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

    public static void advance(float amount) {
        if (!isActive) {
            return;
        }

        activeTime += amount;
        if (activeTime >= DISSIPATION_TIME) {
            isActive = false;

            // Remove debuffs from all ships within the radius
            List<CombatEntityAPI> targets = getTargetsInRange(Global.getCombatEngine(), Global.getCombatEngine().getPlayerShip().getLocation());
            for (CombatEntityAPI target : targets) {
                if (target instanceof ShipAPI) {
                    removeSpecialEffects((ShipAPI) target, "AEG_NaniteSwarm");
                }
            }
        }
    }
}