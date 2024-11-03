package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import data.shipsystems.helpers.JitterEffectManager;
import data.shipsystems.helpers.AEG_SB_Animation;
import data.shipsystems.helpers.AEG_SteelBarrageHelper;
import data.shipsystems.helpers.AEG_SteelBarrageEffects;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class AEG_SteelBarrage extends BaseShipSystemScript {

    private static final float RAM_RADIUS = 1000f;
    private static final int RAM_COUNT = 5;
    private static final float PAUSE_DURATION = 0.4f;

    private int ramCounter = 0;
    private float pauseTimer = 0f;
    private ShipAPI targetShip = null;
    private int maneuverStep = 0;
    private AEG_SB_Animation animation = new AEG_SB_Animation();
    private Map<String, WeaponAPI> weaponMap;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        if (state == State.ACTIVE) {
            handleActiveState(ship, id, effectLevel);
        }

        AEG_SteelBarrageEffects.addJitterCopies(ship);
    }

    private void handleActiveState(ShipAPI ship, String id, float effectLevel) {
        if (ramCounter == 0) {
            targetShip = findClosestTarget(ship);
            if (targetShip != null) {
                ramCounter = RAM_COUNT;
            }
        }

        if (targetShip != null && ramCounter > 0) {
            if (pauseTimer <= 0f) {
                if (maneuverStep == 0) {
                    AEG_SteelBarrageEffects.createHitspark(ship, targetShip);
                }

                AEG_SteelBarrageHelper.performManeuvers(ship, targetShip, maneuverStep);

                if (maneuverStep >= AEG_SteelBarrageHelper.MANEUVER_STEPS.length) {
                    AEG_SteelBarrageHelper.applyRammingForceAndDamage(ship, targetShip, id, effectLevel);
                    AEG_SteelBarrageEffects.createExplosionOrShieldHit(ship, targetShip);

                    resetManeuver();
                } else {
                    incrementManeuverStep();
                }
            } else {
                pauseTimer -= Global.getCombatEngine().getElapsedInLastFrame();
            }
        }

        handleWeaponAnimation(ship, effectLevel);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        resetWeaponAnimation(ship);
    }

    private void handleWeaponAnimation(ShipAPI ship, float effectLevel) {
        initializeWeaponMap(ship);

        WeaponAPI shoulderLeft = findWeapon("AEG_broly_shoulder_l");
        WeaponAPI armLeft = findWeapon("AEG_broly_arm_l");
        WeaponAPI shoulderRight = findWeapon("AEG_broly_shoulder_r");
        WeaponAPI armRight = findWeapon("AEG_broly_arm_r");

        if (shoulderLeft != null && armLeft != null && shoulderRight != null && armRight != null) {
            animation.advance(shoulderLeft, armLeft, shoulderRight, armRight, effectLevel);
        }
    }

    private void resetWeaponAnimation(ShipAPI ship) {
        initializeWeaponMap(ship);

        WeaponAPI shoulderLeft = findWeapon("AEG_broly_shoulder_l");
        WeaponAPI armLeft = findWeapon("AEG_broly_arm_l");
        WeaponAPI shoulderRight = findWeapon("AEG_broly_shoulder_r");
        WeaponAPI armRight = findWeapon("AEG_broly_arm_r");

        if (shoulderLeft != null && armLeft != null && shoulderRight != null && armRight != null) {
            animation.reset(shoulderLeft, armLeft, shoulderRight, armRight);
        }
    }

    private void initializeWeaponMap(ShipAPI ship) {
        weaponMap = new HashMap<>();
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            weaponMap.put(weapon.getSlot().getId(), weapon);
        }
    }

    private WeaponAPI findWeapon(String slotId) {
        return weaponMap.get(slotId);
    }

    private ShipAPI findClosestTarget(ShipAPI ship) {
        ShipAPI closestTarget = null;
        float closestDistance = Float.MAX_VALUE;
        for (ShipAPI target : Global.getCombatEngine().getShips()) {
            if (target.getOwner() != ship.getOwner() && !target.isHulk() && target.isAlive()) {
                float distance = Misc.getDistance(ship.getLocation(), target.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestTarget = target;
                }
            }
        }
        return closestTarget;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("True Destruction Steel Fist Barrage!", false);
        }
        return null;
    }

    private void resetManeuver() {
        maneuverStep = 0;
        pauseTimer = PAUSE_DURATION;
        ramCounter--;
    }

    private void incrementManeuverStep() {
        maneuverStep++;
        pauseTimer = PAUSE_DURATION / 2f;
    }
}
