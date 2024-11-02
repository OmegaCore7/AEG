package data.shipsystems.helpers;

import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;

public class AEG_SB_Animation {
    private static final float SHOULDER_ROTATION_ANGLE = 15f;
    private static final float ARM_MOVEMENT_DISTANCE = 5f;
    private static final float ANIMATION_SPEED = 0.1f; // Adjust as needed

    private float animationProgress = 0f;
    private boolean isPunchingRight = true;

    public void advance(WeaponAPI shoulderLeft, WeaponAPI armLeft, WeaponAPI shoulderRight, WeaponAPI armRight, float amount) {
        animationProgress += amount * ANIMATION_SPEED;

        if (animationProgress >= 1f) {
            animationProgress = 0f;
            isPunchingRight = !isPunchingRight;
        }

        if (isPunchingRight) {
            // Right punch
            shoulderRight.setCurrAngle((float) Math.toRadians(-SHOULDER_ROTATION_ANGLE * animationProgress));
            armRight.getLocation().set(new Vector2f(armRight.getLocation().x + ARM_MOVEMENT_DISTANCE * animationProgress, armRight.getLocation().y));
            shoulderLeft.setCurrAngle((float) Math.toRadians(SHOULDER_ROTATION_ANGLE * animationProgress));
            armLeft.getLocation().set(new Vector2f(armLeft.getLocation().x - ARM_MOVEMENT_DISTANCE * animationProgress, armLeft.getLocation().y));
        } else {
            // Left punch
            shoulderRight.setCurrAngle((float) Math.toRadians(SHOULDER_ROTATION_ANGLE * animationProgress));
            armRight.getLocation().set(new Vector2f(armRight.getLocation().x - ARM_MOVEMENT_DISTANCE * animationProgress, armRight.getLocation().y));
            shoulderLeft.setCurrAngle((float) Math.toRadians(-SHOULDER_ROTATION_ANGLE * animationProgress));
            armLeft.getLocation().set(new Vector2f(armLeft.getLocation().x + ARM_MOVEMENT_DISTANCE * animationProgress, armLeft.getLocation().y));
        }
    }

    public void reset(WeaponAPI shoulderLeft, WeaponAPI armLeft, WeaponAPI shoulderRight, WeaponAPI armRight) {
        shoulderLeft.setCurrAngle(0);
        armLeft.getLocation().set(new Vector2f(0, 0));
        shoulderRight.setCurrAngle(0);
        armRight.getLocation().set(new Vector2f(0, 0));
    }
}
