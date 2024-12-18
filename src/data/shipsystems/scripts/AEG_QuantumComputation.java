package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_QuantumComputation extends BaseShipSystemScript {
    private static final float PUSH_FORCE = 10f;
    private static final float DAMAGE_TAKEN_REDUCTION = 0.9f; // 10% damage taken

    private static final int TURN_ACC_BUFF = 1000;
    private static final int TURN_RATE_BUFF = 500;
    private static final int ACCEL_BUFF = 500;
    private static final int DECCEL_BUFF = 300;
    private static final int SPEED_BUFF = 50;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        if (state == State.IN) {
            // Apply visual effects for the maneuver
            applyVisualEffects(ship, effectLevel);

            // Apply speed and maneuverability boost
            stats.getMaxSpeed().modifyPercent(id, SPEED_BUFF * effectLevel);
            stats.getAcceleration().modifyPercent(id, ACCEL_BUFF * effectLevel);
            stats.getDeceleration().modifyPercent(id, DECCEL_BUFF * effectLevel);

            // Apply turn rate and acceleration buffs
            stats.getTurnAcceleration().modifyPercent(id, TURN_ACC_BUFF * effectLevel);
            stats.getMaxTurnRate().modifyPercent(id, TURN_RATE_BUFF * effectLevel);

            // Increase engine flame size
            ship.getEngineController().extendFlame(id, 1.5f * effectLevel, 1.5f * effectLevel, 1.5f * effectLevel);

            // Apply push force
            Vector2f pushVector = VectorUtils.getDirectionalVector(ship.getLocation(), ship.getMouseTarget());
            pushVector.scale(PUSH_FORCE * effectLevel);
            Vector2f.add(ship.getVelocity(), pushVector, ship.getVelocity());

            // Disable shields for balance
            ship.getShield().toggleOff();

            // Apply damage taken reduction
            stats.getHullDamageTakenMult().modifyMult(id, DAMAGE_TAKEN_REDUCTION);
            stats.getArmorDamageTakenMult().modifyMult(id, DAMAGE_TAKEN_REDUCTION);
            stats.getShieldDamageTakenMult().modifyMult(id, DAMAGE_TAKEN_REDUCTION);
        } else if (state == State.OUT) {
            // Reset stats after the system is deactivated
            stats.getMaxSpeed().unmodify(id);
            stats.getAcceleration().unmodify(id);
            stats.getDeceleration().unmodify(id);
            stats.getTurnAcceleration().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
            stats.getHullDamageTakenMult().unmodify(id);
            stats.getArmorDamageTakenMult().unmodify(id);
            stats.getShieldDamageTakenMult().unmodify(id);
        }
    }

    private void applyVisualEffects(ShipAPI ship, float effectLevel) {
        ship.setJitterUnder(
                ship,
                Color.CYAN,
                0.5f * effectLevel,
                5,
                5 + 5f * effectLevel,
                5 + 10f * effectLevel
        );
        if (Math.random() > 0.9f) {
            ship.addAfterimage(new Color(0, 200, 255, 64), 0, 0, -ship.getVelocity().x, -ship.getVelocity().y, 5 + 50 * effectLevel, 0, 0, 2 * effectLevel, false, false, false);
        }
    }
}