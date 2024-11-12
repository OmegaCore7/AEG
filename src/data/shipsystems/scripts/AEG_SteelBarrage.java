package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;

public class AEG_SteelBarrage extends BaseShipSystemScript {

    private static final float ARM_MOVEMENT_DISTANCE = 10f;
    private static final float SHOULDER_MOVEMENT_DISTANCE = 10f;
    private static final float ANIMATION_SPEED = 0.2f; // Adjusted for 5-second animation
    private static final float TOTAL_ANIMATION_TIME = 6f; // Total time for the animation

    private float animationProgress = 0f;
    private final Map<String, WeaponAPI> weaponMap = new HashMap<>();
    private final Map<String, Vector2f> initialPositions = new HashMap<>();

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        if (state == State.ACTIVE) {
            handleWeaponAnimation(ship, effectLevel);
        } else {
            resetToInitialPositions(ship);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        resetToInitialPositions(ship);
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
        animationProgress += amount * (ANIMATION_SPEED / TOTAL_ANIMATION_TIME);

        if (animationProgress >= 1f) {
            animationProgress = 0f;
        }

        float armMovement = (float) Math.sin(animationProgress * Math.PI * 2) * ARM_MOVEMENT_DISTANCE;
        float shoulderMovement = (float) Math.sin(animationProgress * Math.PI * 2) * SHOULDER_MOVEMENT_DISTANCE;

        // Move arms and shoulders equally on both sides
        shoulderRight.getSlot().getLocation().set(new Vector2f(initialPositions.get("WS0002").x + shoulderMovement, initialPositions.get("WS0002").y));
        armRight.getSlot().getLocation().set(new Vector2f(initialPositions.get("WS0004").x + armMovement, initialPositions.get("WS0004").y));
        shoulderLeft.getSlot().getLocation().set(new Vector2f(initialPositions.get("WS0001").x - shoulderMovement, initialPositions.get("WS0001").y));
        armLeft.getSlot().getLocation().set(new Vector2f(initialPositions.get("WS0003").x - armMovement, initialPositions.get("WS0003").y));
    }

    private void resetToInitialPositions(ShipAPI ship) {
        initializeWeaponMap(ship);

        WeaponAPI shoulderLeft = findWeapon("WS0001");
        WeaponAPI armLeft = findWeapon("WS0003");
        WeaponAPI shoulderRight = findWeapon("WS0002");
        WeaponAPI armRight = findWeapon("WS0004");

        if (shoulderLeft != null && armLeft != null && shoulderRight != null && armRight != null) {
            // Reset to initial positions
            shoulderLeft.getSlot().getLocation().set(initialPositions.get("WS0001"));
            armLeft.getSlot().getLocation().set(initialPositions.get("WS0003"));
            shoulderRight.getSlot().getLocation().set(initialPositions.get("WS0002"));
            armRight.getSlot().getLocation().set(initialPositions.get("WS0004"));
        }
    }

    private void initializeWeaponMap(ShipAPI ship) {
        weaponMap.clear();
        initialPositions.clear();
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            weaponMap.put(weapon.getSlot().getId(), weapon);
            initialPositions.put(weapon.getSlot().getId(), new Vector2f(weapon.getSlot().getLocation()));
        }
    }

    private WeaponAPI findWeapon(String slotId) {
        return weaponMap.get(slotId);
    }
}