package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class AEG_EMPPulse {

    private static final float EMP_RADIUS = 1000f;
    private static final float EMP_DURATION = 5f;

    public static void execute(ShipAPI ship, String id) {
        CombatEngineAPI engine = Global.getCombatEngine();

        // Emit EMP pulse
        emitEMPPulse(ship, engine);
    }

    private static void emitEMPPulse(ShipAPI ship, CombatEngineAPI engine) {
        List<CombatEntityAPI> targets = getTargetsInRange(engine, ship.getLocation(), EMP_RADIUS);

        for (CombatEntityAPI target : targets) {
            if (target instanceof ShipAPI) {
                ShipAPI targetShip = (ShipAPI) target;
                disableSystems(targetShip);
            }
        }
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

    private static void disableSystems(final ShipAPI target) {
        target.getMutableStats().getShieldUpkeepMult().modifyMult("AEG_EMPPulse", 0f);
        target.getMutableStats().getWeaponMalfunctionChance().modifyFlat("AEG_EMPPulse", 1f);
        target.getMutableStats().getEngineMalfunctionChance().modifyFlat("AEG_EMPPulse", 1f);

        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            private float elapsed = 0f;

            @Override
            public void advance(float amount, List events) {
                if (Global.getCombatEngine().isPaused()) {
                    return;
                }

                elapsed += amount;
                if (elapsed >= EMP_DURATION) {
                    target.getMutableStats().getShieldUpkeepMult().unmodify("AEG_EMPPulse");
                    target.getMutableStats().getWeaponMalfunctionChance().unmodify("AEG_EMPPulse");
                    target.getMutableStats().getEngineMalfunctionChance().unmodify("AEG_EMPPulse");
                    Global.getCombatEngine().removePlugin(this);
                }
            }
        });
    }
}