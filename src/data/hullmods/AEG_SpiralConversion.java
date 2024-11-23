package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AEG_SpiralConversion extends BaseHullMod {
    private static final float DEFLECT_RADIUS = 500f;
    private static final int MAX_CHARGES = 5;
    private static final float RECHARGE_TIME = 10f;

    private final Map<ShipAPI, Integer> charges = new HashMap<>();
    private final Map<ShipAPI, IntervalUtil> rechargeTimers = new HashMap<>();
    private final IntervalUtil inputCheckInterval = new IntervalUtil(0.1f, 0.1f); // Check input every 0.1 seconds

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCombatEngine().isPaused()) return;

        if (!charges.containsKey(ship)) {
            charges.put(ship, MAX_CHARGES);
            rechargeTimers.put(ship, new IntervalUtil(RECHARGE_TIME, RECHARGE_TIME));
        }

        IntervalUtil timer = rechargeTimers.get(ship);
        timer.advance(amount);

        if (timer.intervalElapsed() && charges.get(ship) < MAX_CHARGES) {
            charges.put(ship, charges.get(ship) + 1);
        }

        inputCheckInterval.advance(amount);
        if (inputCheckInterval.intervalElapsed() && charges.get(ship) > 0 && !ship.isPhased() && isActivationKeyPressed()) {
            List<MissileAPI> missilesInRange = CombatUtils.getMissilesWithinRange(ship.getLocation(), DEFLECT_RADIUS);
            for (MissileAPI missile : missilesInRange) {
                if (missile.getOwner() != ship.getOwner()) {
                    deflectMissile(ship, missile);
                    charges.put(ship, charges.get(ship) - 1);
                    break;
                }
            }
        }
    }

    private boolean isActivationKeyPressed() {
        return Keyboard.isKeyDown(Keyboard.KEY_W) && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
    }

    private void deflectMissile(ShipAPI ship, MissileAPI missile) {
        missile.setOwner(ship.getOwner());
        missile.setSource(ship);
        missile.setMissileAI(new AEG_SpiralEffectedMissile(missile, null));
        Global.getSoundPlayer().playSound("hit_shield_solid_gun", 1f, 1f, ship.getLocation(), ship.getVelocity());

        RippleDistortion ripple = new RippleDistortion(ship.getLocation(), ship.getVelocity());
        ripple.setSize(DEFLECT_RADIUS);
        ripple.setIntensity(DEFLECT_RADIUS / 2);
        ripple.setFrameRate(60f);
        ripple.fadeInSize(0.75f);
        ripple.fadeOutIntensity(0.5f);
        DistortionShader.addDistortion(ripple);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + DEFLECT_RADIUS;
        if (index == 1) return "" + MAX_CHARGES;
        if (index == 2) return "" + RECHARGE_TIME;
        return null;
    }
}