package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
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
                applySpecialEffects(targetShip);
                disableSystems(targetShip);
                createVisualEffects(ship, targetShip);
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
        target.getMutableStats().getWeaponMalfunctionChance().modifyPercent("AEG_EMPPulse", 100f);
        target.getMutableStats().getEngineMalfunctionChance().modifyPercent("AEG_EMPPulse", 100f);

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

    private static void applySpecialEffects(ShipAPI target) {
        // Chain Reaction
        List<CombatEntityAPI> nearbyTargets = getTargetsInRange(Global.getCombatEngine(), target.getLocation(), EMP_RADIUS / 2);
        for (CombatEntityAPI nearbyTarget : nearbyTargets) {
            if (nearbyTarget != target && nearbyTarget instanceof ShipAPI) {
                disableSystems((ShipAPI) nearbyTarget);
            }
        }

        // Flux Overload
        target.getFluxTracker().increaseFlux(target.getFluxTracker().getMaxFlux() * 0.25f, true);

        // Shield Disruption
        if (target.getShield() != null) {
            target.getShield().toggleOff();
        }

        // Weapon Jam
        target.getMutableStats().getWeaponMalfunctionChance().modifyPercent("AEG_EMPPulse", 100f);

        // Engine Shutdown
        target.getMutableStats().getEngineMalfunctionChance().modifyPercent("AEG_EMPPulse", 100f);

        // Sensor Scramble
        target.getMutableStats().getSensorProfile().modifyMult("AEG_EMPPulse", 2f);

        // Energy Drain
        target.getMutableStats().getEnergyWeaponDamageMult().modifyMult("AEG_EMPPulse", 0.5f);

        // Hull Breach (apply direct hull damage)
        target.setHitpoints(target.getHitpoints() - target.getMaxHitpoints() * 0.1f);
    }

    private static void createVisualEffects(ShipAPI source, ShipAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();

        // Visual effects on the player ship
        for (int i = 0; i < 10; i++) {
            Vector2f point = MathUtils.getRandomPointInCircle(source.getLocation(), source.getCollisionRadius());
            engine.spawnEmpArcVisual(source.getLocation(), source, point, target, 10f, new Color(0, 255, 0, 255), new Color(0, 100, 0, 255));
        }

        // Visual effects on the target ships
        for (int i = 0; i < 10; i++) {
            Vector2f point = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius());
            engine.spawnEmpArcVisual(target.getLocation(), target, point, target, 10f, new Color(255, 255, 255, 255), new Color(100, 100, 255, 255));
        }
    }
}
