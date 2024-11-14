package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class AEG_GiganticCatastrophe {

    private static final float SPEED_BOOST = 1.5f;
    private static final float MANEUVERABILITY_BOOST = 1.5f;
    private static final float PUSH_FORCE = 700f;
    private static final float TIME_DILATION_DURATION = 0.5f;
    private static final float TIME_MULT = 0.1f;

    private static final int TURN_ACC_BUFF = 1000;
    private static final int TURN_RATE_BUFF = 500;
    private static final int ACCEL_BUFF = 500;
    private static final int DECCEL_BUFF = 300;
    private static final int SPEED_BUFF = 200;
    private static final int TIME_BUFF = 1000;

    private static final int MAX_CHARGES = 6;
    private static int currentCharges = MAX_CHARGES;

    public static void execute(ShipAPI ship, String id) {
        CombatEngineAPI engine = Global.getCombatEngine();

        // Apply time dilation for the transition
        AEG_TimeDilationHelper.applyTimeDilation(ship, TIME_DILATION_DURATION, TIME_MULT);

        // Apply visual effects for the maneuver
        applyVisualEffects(ship, engine);

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
    }

    public static void handleCollision(ShipAPI ship, CombatEntityAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();

        // Create collision effects
        AEG_SB_Effect.createCollisionEffects(ship, target, engine);

        // Use a charge when taking hits
        if (currentCharges > 0) {
            currentCharges--;
        }
    }

    private static void applyVisualEffects(ShipAPI ship, CombatEngineAPI engine) {
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

    public static void resetCharges() {
        currentCharges = MAX_CHARGES;
    }
}