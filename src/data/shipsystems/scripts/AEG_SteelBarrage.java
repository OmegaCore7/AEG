package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;

public class AEG_SteelBarrage extends BaseShipSystemScript {

    private static final float SHOULDER_ROTATION_ANGLE = 15f;
    private static final float ARM_MOVEMENT_DISTANCE = 5f;
    private static final float ANIMATION_SPEED = 0.1f; // Adjust as needed

    private float animationProgress = 0f;
    private boolean isPunchingRight = true;
    private final Map<String, WeaponAPI> weaponMap = new HashMap<>();

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

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

        WeaponAPI shoulderLeft = findWeapon("WS0001");
        WeaponAPI armLeft = findWeapon("WS0003");
        WeaponAPI shoulderRight = findWeapon("WS0002");
        WeaponAPI armRight = findWeapon("WS0004");

        if (shoulderLeft != null && armLeft != null && shoulderRight != null && armRight != null) {
            advance(shoulderLeft, armLeft, shoulderRight, armRight, effectLevel);
        }
    }

    private void advance(WeaponAPI shoulderLeft, WeaponAPI armLeft, WeaponAPI shoulderRight, WeaponAPI armRight, float amount) {
        animationProgress += amount * ANIMATION_SPEED;

        if (animationProgress >= 1f) {
            animationProgress = 0f;
            isPunchingRight = !isPunchingRight;
        }

        if (isPunchingRight) {
            // Right punch
            shoulderRight.setCurrAngle((float) Math.toRadians(-SHOULDER_ROTATION_ANGLE * animationProgress));
            armRight.getSlot().getLocation().set(new Vector2f(armRight.getSlot().getLocation().x + ARM_MOVEMENT_DISTANCE * animationProgress, armRight.getSlot().getLocation().y));
            shoulderLeft.setCurrAngle((float) Math.toRadians(SHOULDER_ROTATION_ANGLE * animationProgress));
            armLeft.getSlot().getLocation().set(new Vector2f(armLeft.getSlot().getLocation().x - ARM_MOVEMENT_DISTANCE * animationProgress, armLeft.getSlot().getLocation().y));
        } else {
            // Left punch
            shoulderRight.setCurrAngle((float) Math.toRadians(SHOULDER_ROTATION_ANGLE * animationProgress));
            armRight.getSlot().getLocation().set(new Vector2f(armRight.getSlot().getLocation().x - ARM_MOVEMENT_DISTANCE * animationProgress, armRight.getSlot().getLocation().y));
            shoulderLeft.setCurrAngle((float) Math.toRadians(-SHOULDER_ROTATION_ANGLE * animationProgress));
            armLeft.getSlot().getLocation().set(new Vector2f(armLeft.getSlot().getLocation().x + ARM_MOVEMENT_DISTANCE * animationProgress, armLeft.getSlot().getLocation().y));
        }
    }

    private void resetWeaponAnimation(ShipAPI ship) {
        initializeWeaponMap(ship);

        WeaponAPI shoulderLeft = findWeapon("WS0001");
        WeaponAPI armLeft = findWeapon("WS0003");
        WeaponAPI shoulderRight = findWeapon("WS0002");
        WeaponAPI armRight = findWeapon("WS0004");

        if (shoulderLeft != null && armLeft != null && shoulderRight != null && armRight != null) {
            shoulderLeft.setCurrAngle(0);
            armLeft.getSlot().getLocation().set(new Vector2f(0, 0));
            shoulderRight.setCurrAngle(0);
            armRight.getSlot().getLocation().set(new Vector2f(0, 0));
        }
    }

    private void initializeWeaponMap(ShipAPI ship) {
        weaponMap.clear();
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            weaponMap.put(weapon.getSlot().getId(), weapon);
        }
    }

    private WeaponAPI findWeapon(String slotId) {
        return weaponMap.get(slotId);
    }
}
