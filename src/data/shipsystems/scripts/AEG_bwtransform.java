package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_bwtransform extends BaseShipSystemScript {
    private static final float PUSH_FORCE = 25f;
    private static final float DAMAGE_TAKEN_REDUCTION = 0.5f; // 50% damage taken

    private static final int TURN_ACC_BUFF = 500;
    private static final int TURN_RATE_BUFF = 500;
    private static final int ACCEL_BUFF = 500;
    private static final int DECCEL_BUFF = 500;
    private static final int SPEED_BUFF = 100;
    private static final int UNIVERSAL_SPEED_BUFF = 50; // Universal speed buff
    private static final float FORWARD_PUSH_DURATION = 1f; // 1 second forward push

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        // Apply universal speed buff when the system is not active
        stats.getMaxSpeed().modifyPercent(id, UNIVERSAL_SPEED_BUFF);

        if (state == State.IN) {
            // Apply visual effects for the maneuver
            applyVisualEffects(ship, effectLevel);

            // Apply maneuverability boost
            stats.getAcceleration().modifyPercent(id, ACCEL_BUFF * effectLevel);
            stats.getDeceleration().modifyPercent(id, DECCEL_BUFF * effectLevel);
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
            stats.getAcceleration().unmodify(id);
            stats.getDeceleration().unmodify(id);
            stats.getTurnAcceleration().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
            stats.getHullDamageTakenMult().unmodify(id);
            stats.getArmorDamageTakenMult().unmodify(id);
            stats.getShieldDamageTakenMult().unmodify(id);

            // Apply forward push force
            float facing = ship.getFacing();
            Vector2f forwardPush = new Vector2f((float) Math.cos(Math.toRadians(facing)), (float) Math.sin(Math.toRadians(facing)));
            forwardPush.scale(PUSH_FORCE * FORWARD_PUSH_DURATION);
            Vector2f.add(ship.getVelocity(), forwardPush, ship.getVelocity());
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