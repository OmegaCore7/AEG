package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;

public class AEG_SteelBarrage extends BaseShipSystemScript {

    private static final float ARM_MOVEMENT_DISTANCE = 6f;
    private static final float SHOULDER_MOVEMENT_DISTANCE = 6f;
    private static final float ANIMATION_SPEED = 0.2f; // Adjusted for 5-second animation
    private static final float RETURN_SPEED = 0.05f; // Speed for returning to base position
    private static final float RESET_TIME = 1f; // Time to reset before system ends
    private static final float TOTAL_ANIMATION_TIME = 6f; // Total time for the animation

    private float animationProgress = 0f;
    private boolean isPunchingRight = true;
    private boolean isResetting = false;
    private float resetTimer = 0f;
    private final Map<String, WeaponAPI> weaponMap = new HashMap<>();
    private final Map<String, Vector2f> initialPositions = new HashMap<>();

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        if (state == State.ACTIVE) {
            handleWeaponAnimation(ship, effectLevel);
        } else if (state == State.OUT) {
            if (!isResetting) {
                isResetting = true;
                resetTimer = RESET_TIME;
            }
            resetTimer -= Global.getCombatEngine().getElapsedInLastFrame();
            if (resetTimer <= 0f) {
                returnToBasePosition(ship);
                isResetting = false;
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        returnToBasePosition(ship);
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
            isPunchingRight = !isPunchingRight;
        }

        float shoulderProgress = animationProgress + (ANIMATION_SPEED / TOTAL_ANIMATION_TIME); // Shoulders move slightly ahead
        if (shoulderProgress > 1f) shoulderProgress = 1f;

        float armMovement = (float) Math.sin(animationProgress * Math.PI * 2) * ARM_MOVEMENT_DISTANCE;
        float shoulderMovement = (float) Math.sin(shoulderProgress * Math.PI * 2) * SHOULDER_MOVEMENT_DISTANCE;

        // Move arms and shoulders independently based on their initial positions
        shoulderRight.getSlot().getLocation().set(new Vector2f(initialPositions.get("WS0002").x + shoulderMovement, initialPositions.get("WS0002").y));
        armRight.getSlot().getLocation().set(new Vector2f(initialPositions.get("WS0004").x + armMovement, initialPositions.get("WS0004").y));
        shoulderLeft.getSlot().getLocation().set(new Vector2f(initialPositions.get("WS0001").x - shoulderMovement, initialPositions.get("WS0001").y));
        armLeft.getSlot().getLocation().set(new Vector2f(initialPositions.get("WS0003").x - armMovement, initialPositions.get("WS0003").y));
    }

    private void returnToBasePosition(ShipAPI ship) {
        initializeWeaponMap(ship);

        WeaponAPI shoulderLeft = findWeapon("WS0001");
        WeaponAPI armLeft = findWeapon("WS0003");
        WeaponAPI shoulderRight = findWeapon("WS0002");
        WeaponAPI armRight = findWeapon("WS0004");

        if (shoulderLeft != null && armLeft != null && shoulderRight != null && armRight != null) {
            // Gradually return to base position
            armLeft.getSlot().getLocation().set(Vector2f.add(initialPositions.get("WS0003"), new Vector2f((armLeft.getSlot().getLocation().x - initialPositions.get("WS0003").x) * (1 - RETURN_SPEED), (armLeft.getSlot().getLocation().y - initialPositions.get("WS0003").y) * (1 - RETURN_SPEED)), null));
            armRight.getSlot().getLocation().set(Vector2f.add(initialPositions.get("WS0004"), new Vector2f((armRight.getSlot().getLocation().x - initialPositions.get("WS0004").x) * (1 - RETURN_SPEED), (armRight.getSlot().getLocation().y - initialPositions.get("WS0004").y) * (1 - RETURN_SPEED)), null));
            shoulderLeft.getSlot().getLocation().set(Vector2f.add(initialPositions.get("WS0001"), new Vector2f((shoulderLeft.getSlot().getLocation().x - initialPositions.get("WS0001").x) * (1 - RETURN_SPEED), (shoulderLeft.getSlot().getLocation().y - initialPositions.get("WS0001").y) * (1 - RETURN_SPEED)), null));
            shoulderRight.getSlot().getLocation().set(Vector2f.add(initialPositions.get("WS0002"), new Vector2f((shoulderRight.getSlot().getLocation().x - initialPositions.get("WS0002").x) * (1 - RETURN_SPEED), (shoulderRight.getSlot().getLocation().y - initialPositions.get("WS0002").y) * (1 - RETURN_SPEED)), null));
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