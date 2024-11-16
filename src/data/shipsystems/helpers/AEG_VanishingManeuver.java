package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class AEG_VanishingManeuver {
    private static final float PUSH_FORCE = 700f;
    private static final float TIME_DILATION_DURATION = 0.5f;
    private static final float TIME_MULT = 0.1f;
    private static final float DAMAGE_TAKEN_REDUCTION = 0.9f; // 10% damage taken

    private static final int TURN_ACC_BUFF = 1000;
    private static final int TURN_RATE_BUFF = 500;
    private static final int ACCEL_BUFF = 500;
    private static final int DECCEL_BUFF = 300;
    private static final int SPEED_BUFF = 200;

    public static void execute(ShipAPI ship, String id) {

        // Apply visual effects for the maneuver
        applyVisualEffects(ship);

        // Apply speed and maneuverability boost
        ship.getMutableStats().getMaxSpeed().modifyPercent(id, SPEED_BUFF);
        ship.getMutableStats().getAcceleration().modifyPercent(id, ACCEL_BUFF);
        ship.getMutableStats().getDeceleration().modifyPercent(id, DECCEL_BUFF);

        // Apply turn rate and acceleration buffs
        ship.getMutableStats().getTurnAcceleration().modifyPercent(id, TURN_ACC_BUFF);
        ship.getMutableStats().getMaxTurnRate().modifyPercent(id, TURN_RATE_BUFF);

        // Increase engine flame size
        ship.getEngineController().extendFlame(id, 1.5f, 1.5f, 1.5f);

        // Apply push force
        Vector2f pushVector = VectorUtils.getDirectionalVector(ship.getLocation(), ship.getMouseTarget());
        pushVector.scale(PUSH_FORCE);
        Vector2f.add(ship.getVelocity(), pushVector, ship.getVelocity());

        // Disable shields for balance
        ship.getShield().toggleOff();

        // Apply time dilation
        applyTimeDilation(ship, id);

        // Apply damage taken reduction
        ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, DAMAGE_TAKEN_REDUCTION);
        ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, DAMAGE_TAKEN_REDUCTION);
        ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, DAMAGE_TAKEN_REDUCTION);
    }

    private static void applyVisualEffects(ShipAPI ship) {
        float effect = 1.0f; // Full effect level for visual effects
        ship.setJitterUnder(
                ship,
                Color.CYAN,
                0.5f * effect,
                5,
                5 + 5f * effect,
                5 + 10f * effect
        );
        if (Math.random() > 0.9f) {
            ship.addAfterimage(new Color(0, 200, 255, 64), 0, 0, -ship.getVelocity().x, -ship.getVelocity().y, 5 + 50 * effect, 0, 0, 2 * effect, false, false, false);
        }
    }

    private static void applyTimeDilation(final ShipAPI ship, final String id) {
        final CombatEngineAPI engine = Global.getCombatEngine();
        engine.getTimeMult().modifyMult(id, TIME_MULT);
        ship.getMutableStats().getTimeMult().modifyMult(id, 1 / TIME_MULT);

        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            private float elapsed = 0f;

            @Override
            public void advance(float amount, List events) {
                if (Global.getCombatEngine().isPaused()) {
                    return;
                }

                elapsed += amount;
                if (elapsed >= TIME_DILATION_DURATION) {
                    engine.getTimeMult().unmodify(id);
                    ship.getMutableStats().getTimeMult().unmodify(id);
                    ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
                    ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
                    ship.getMutableStats().getShieldDamageTakenMult().unmodify(id);
                    Global.getCombatEngine().removePlugin(this);
                }
            }
        });
    }
}