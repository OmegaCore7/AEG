package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class AEG_ChestInfernoLimbController implements EveryFrameCombatPlugin {

    private CombatEngineAPI engine;

    private static final String MAIN_WEAPON_SLOT = "WS0002";  // AEG_ChestInferno
    private static final String ARM_L = "WS0006";
    private static final String ARM_R = "WS0005";
    private static final String SHOULDER_L = "WS0003";
    private static final String SHOULDER_R = "WS0004";

    private final float RECOVERY_DURATION = 0.5f;
    private float recoveryProgress = 0f;
    private float lastCharge = 0f;
    private boolean sparksTriggered = false;
    private final Random rand = new Random();

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

            // Define limb angles and positions
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
                float tChargePhase = mapCharge(charge, 0.0f, 0.2f);  // Slam in
                float tFirePhase = mapCharge(charge, 0.7f, 1.0f);    // Recoil to fire pose

                if (charge < 0.2f) {
                    // Phase 1: Slam
                    finalArmLX = lerp(armLStartX, armLSlamX, tChargePhase);
                    finalArmLY = lerp(armLStartY, armLSlamY, tChargePhase);
                    finalArmLAngle = lerp(armLStartAngle, armLSlamAngle, tChargePhase);

                    finalArmRX = lerp(armRStartX, armRSlamX, tChargePhase);
                    finalArmRY = lerp(armRStartY, armRSlamY, tChargePhase);
                    finalArmRAngle = lerp(armRStartAngle, armRSlamAngle, tChargePhase);

                    finalShoulderLAngle = lerp(shoulderLStartAngle, shoulderLSlamAngle, tChargePhase);
                    finalShoulderRAngle = lerp(shoulderRStartAngle, shoulderRSlamAngle, tChargePhase);

                    // Handle spark burst during slam phase
                    spawnSparksAt(ship, 0f, 75f); // World position relative to ship facing
                    sparksTriggered = true;
                } else if (charge < 0.7f) {
                    // Phase 2: Hold slam pose
                    finalArmLX = armLSlamX;
                    finalArmLY = armLSlamY;
                    finalArmLAngle = armLSlamAngle;

                    finalArmRX = armRSlamX;
                    finalArmRY = armRSlamY;
                    finalArmRAngle = armRSlamAngle;

                    finalShoulderLAngle = shoulderLSlamAngle;
                    finalShoulderRAngle = shoulderRSlamAngle;
                } else {
                    // Phase 3: Recoil back through start into firing pose
                    finalArmLX = lerp(armLSlamX, armLFireX, tFirePhase);
                    finalArmLY = lerp(armLSlamY, armLFireY, tFirePhase);
                    finalArmLAngle = lerp(armLSlamAngle, armLFireAngle, tFirePhase);

                    finalArmRX = lerp(armRSlamX, armRFireX, tFirePhase);
                    finalArmRY = lerp(armRSlamY, armRFireY, tFirePhase);
                    finalArmRAngle = lerp(armRSlamAngle, armRFireAngle, tFirePhase);

                    finalShoulderLAngle = lerp(shoulderLSlamAngle, shoulderLFireAngle, tFirePhase);
                    finalShoulderRAngle = lerp(shoulderRSlamAngle, shoulderRFireAngle, tFirePhase);
                }

                recoveryProgress = 0f; // Cancel recovery
            } else {
                // Recovery: return to idle
                recoveryProgress += amount;
                float t = Math.min(recoveryProgress / RECOVERY_DURATION, 1f);
                finalArmLX = lerp(armLFireX, armLStartX, t);
                finalArmLY = lerp(armLFireY, armLStartY, t);
                finalArmLAngle = lerp(armLFireAngle, armLStartAngle, t);

                finalArmRX = lerp(armRFireX, armRStartX, t);
                finalArmRY = lerp(armRFireY, armRStartY, t);
                finalArmRAngle = lerp(armRFireAngle, armRStartAngle, t);

                finalShoulderLAngle = lerp(shoulderLFireAngle, shoulderLStartAngle, t);
                finalShoulderRAngle = lerp(shoulderRFireAngle, shoulderRStartAngle, t);

                sparksTriggered = false; // Reset sparks for next activation
            }

            // Update weapon positions and rotations
            updateWeaponPosition(ship, ARM_L, finalArmLX, finalArmLY, facing + finalArmLAngle);
            updateWeaponPosition(ship, ARM_R, finalArmRX, finalArmRY, facing + finalArmRAngle);
            updateWeaponPosition(ship, SHOULDER_L, 0f, 0f, facing + finalShoulderLAngle); // Shoulders only rotate
            updateWeaponPosition(ship, SHOULDER_R, 0f, 0f, facing + finalShoulderRAngle);

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

    private void updateWeaponPosition(ShipAPI ship, String slotId, float offsetX, float offsetY, float angle) {
        WeaponAPI weapon = getWeaponBySlot(ship, slotId);
        if (weapon != null) {
            // Calculate new position based on ship location and offsets
            Vector2f shipLoc = ship.getLocation();
            float angleRad = (float) Math.toRadians(ship.getFacing());
            float x = shipLoc.x + (float) Math.cos(angleRad) * offsetY - (float) Math.sin(angleRad) * offsetX;
            float y = shipLoc.y + (float) Math.sin(angleRad) * offsetY + (float) Math.cos(angleRad) * offsetX;

            // Set weapon position
            weapon.getLocation().set(x, y);

            // Set weapon rotation
            weapon.setCurrAngle(angle);
        }
    }

    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    private float mapCharge(float charge, float min, float max) {
        if (charge < min) return 0f;
        if (charge > max) return 1f;
        return (charge - min) / (max - min);
    }
    private void spawnSparksAt(ShipAPI ship, float offsetX, float offsetY) {
        Vector2f shipLoc = ship.getLocation();
        float angleRad = (float) Math.toRadians(ship.getFacing());
        float x = shipLoc.x + (float) Math.cos(angleRad) * offsetY - (float) Math.sin(angleRad) * offsetX;
        float y = shipLoc.y + (float) Math.sin(angleRad) * offsetY + (float) Math.cos(angleRad) * offsetX;

        for (int i = 0; i < 6; i++) {
            float dx = rand.nextFloat() * 10f - 5f;
            float dy = rand.nextFloat() * 10f - 5f;
            Vector2f loc = new Vector2f(x + dx, y + dy);
            Color color = new Color(
                    255 - rand.nextInt(55),
                    180 - rand.nextInt(45),
                    100 - rand.nextInt(50),
                    255 - rand.nextInt(105)
            );
            engine.addSmoothParticle(loc, new Vector2f(), 10f + rand.nextFloat() * 5f, 1f, 0.2f, color);
        }
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {}

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {}

    @Override
    public void renderInUICoords(ViewportAPI viewport) {}
}
