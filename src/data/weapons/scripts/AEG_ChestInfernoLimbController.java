package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicAnim;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class AEG_ChestInfernoLimbController implements EveryFrameCombatPlugin {

    private CombatEngineAPI engine;

    private static final String MAIN_WEAPON_SLOT = "WS0002";
    private static final String ARM_L = "WS0006";
    private static final String ARM_R = "WS0005";
    private static final String SHOULDER_L = "WS0003";
    private static final String SHOULDER_R = "WS0004";

    private final float RECOVERY_DURATION = 0.5f;
    private float recoveryProgress = 0f;
    private float lastCharge = 0f;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) return;

        for (ShipAPI ship : engine.getShips()) {
            if (ship == null || !ship.isAlive()) continue;

            WeaponAPI inferno = getWeaponBySlot(ship, MAIN_WEAPON_SLOT);
            if (inferno == null) continue;

            WeaponGroupAPI selected = ship.getSelectedGroupAPI();
            if (selected == null || !selected.getWeaponsCopy().contains(inferno)) continue;

            float charge = inferno.getChargeLevel();
            float facing = ship.getFacing();

            // Define limb poses
            float armLStartX = 78f, armLStartY = 8f, armLStartAngle = 0f;
            float armRStartX = -78f, armRStartY = 8f, armRStartAngle = 0f;
            float shoulderLStartAngle = 0f, shoulderRStartAngle = 0f;

            float armLSlamX = 60f, armLSlamY = 30f, armLSlamAngle = -60f;
            float armRSlamX = -60f, armRSlamY = 30f, armRSlamAngle = 60f;
            float shoulderLSlamAngle = -45f, shoulderRSlamAngle = 45f;

            float armLFireX = 82f, armLFireY = 2f, armLFireAngle = 45f;
            float armRFireX = -82f, armRFireY = 2f, armRFireAngle = -45f;
            float shoulderLFireAngle = 25f, shoulderRFireAngle = -25f;

            float finalArmLX, finalArmLY, finalArmLAngle;
            float finalArmRX, finalArmRY, finalArmRAngle;
            float finalShoulderLAngle, finalShoulderRAngle;

            if (charge > 0f) {
                float t = MagicAnim.smooth(charge);

                if (charge < 0.2f) {
                    // Phase 1: Slam in
                    float tSlam = charge / 0.2f;
                    finalArmLX = lerp(armLStartX, -armLSlamX, tSlam);
                    finalArmLY = lerp(armLStartY, -armLSlamY, tSlam);
                    finalArmLAngle = lerp(armLStartAngle, -armLSlamAngle, tSlam);

                    finalArmRX = lerp(armRStartX, -armRSlamX, tSlam);
                    finalArmRY = lerp(armRStartY, -armRSlamY, tSlam);
                    finalArmRAngle = lerp(armRStartAngle, -armRSlamAngle, tSlam);

                    finalShoulderLAngle = lerp(shoulderLStartAngle, shoulderLSlamAngle, tSlam);
                    finalShoulderRAngle = lerp(shoulderRStartAngle, shoulderRSlamAngle, tSlam);
                } else if (charge < 0.7f) {
                    // Phase 2: Hold slam
                    finalArmLX = -armLSlamX;
                    finalArmLY = -armLSlamY;
                    finalArmLAngle = -armLSlamAngle;

                    finalArmRX = -armRSlamX;
                    finalArmRY = -armRSlamY;
                    finalArmRAngle = -armRSlamAngle;

                    finalShoulderLAngle = shoulderLSlamAngle;
                    finalShoulderRAngle = shoulderRSlamAngle;
                } else {
                    // Phase 3: Recoil to fire
                    float tRecoil = (charge - 0.7f) / 0.3f;
                    finalArmLX = lerp(-armLSlamX, -armLFireX, tRecoil);
                    finalArmLY = lerp(-armLSlamY, -armLFireY, tRecoil);
                    finalArmLAngle = lerp(-armLSlamAngle, -armLFireAngle, tRecoil);

                    finalArmRX = lerp(-armRSlamX, -armRFireX, tRecoil);
                    finalArmRY = lerp(-armRSlamY, -armRFireY, tRecoil);
                    finalArmRAngle = lerp(-armRSlamAngle, -armRFireAngle, tRecoil);

                    finalShoulderLAngle = lerp(shoulderLSlamAngle, shoulderLFireAngle, tRecoil);
                    finalShoulderRAngle = lerp(shoulderRSlamAngle, shoulderRFireAngle, tRecoil);
                }

                recoveryProgress = 0f; // Cancel recovery
            } else {
                // Recovery to idle
                recoveryProgress += amount;
                float t = Math.min(recoveryProgress / RECOVERY_DURATION, 1f);
                finalArmLX = lerp(-armLFireX, -armLStartX, t);
                finalArmLY = lerp(-armLFireY, -armLStartY, t);
                finalArmLAngle = lerp(-armLFireAngle, -armLStartAngle, t);

                finalArmRX = lerp(-armRFireX, -armRStartX, t);
                finalArmRY = lerp(-armRFireY, -armRStartY, t);
                finalArmRAngle = lerp(-armRFireAngle, -armRStartAngle, t);

                finalShoulderLAngle = lerp(shoulderLFireAngle, shoulderLStartAngle, t);
                finalShoulderRAngle = lerp(shoulderRFireAngle, shoulderRStartAngle, t);
            }

            // Apply transforms
            updateWeaponTransform(ship, ARM_L, finalArmLX, finalArmLY, facing + finalArmLAngle);
            updateWeaponTransform(ship, ARM_R, finalArmRX, finalArmRY, facing + finalArmRAngle);
            updateWeaponTransform(ship, SHOULDER_L, 0f, 0f, facing + finalShoulderLAngle);
            updateWeaponTransform(ship, SHOULDER_R, 0f, 0f, facing + finalShoulderRAngle);

            lastCharge = charge;
        }
    }

    private WeaponAPI getWeaponBySlot(ShipAPI ship, String slotId) {
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot() != null && slotId.equals(w.getSlot().getId())) {
                return w;
            }
        }
        return null;
    }
    private void updateWeaponTransform(ShipAPI ship, String slotId, float offsetX, float offsetY, float angle) {
        WeaponAPI weapon = getWeaponBySlot(ship, slotId);
        if (weapon != null) {
            // Ship's rotation in radians
            float shipAngleRad = (float) Math.toRadians(ship.getFacing());

            // Rotate local offset by ship's facing
            float rotatedX = offsetX * (float) Math.cos(shipAngleRad) - offsetY * (float) Math.sin(shipAngleRad);
            float rotatedY = offsetX * (float) Math.sin(shipAngleRad) + offsetY * (float) Math.cos(shipAngleRad);

            // Add to ship's current position to get world coordinates
            Vector2f worldPosition = new Vector2f(ship.getLocation().x + rotatedX, ship.getLocation().y + rotatedY);

            // Apply position and rotation
            weapon.getLocation().set(worldPosition);
            weapon.setCurrAngle(angle);
        }
    }

    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {}

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {}

    @Override
    public void renderInUICoords(ViewportAPI viewport) {}
}
