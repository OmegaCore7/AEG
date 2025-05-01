package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;

import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicAnim;
import java.awt.Color;
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
    private final java.util.Map<ShipAPI, Boolean> sparkTriggeredMap = new java.util.WeakHashMap<>();
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

            float armLStartAngle = 0f;
            float armRStartAngle = 0f;
            float shoulderLStartAngle = 0f;
            float shoulderRStartAngle = 0f;

            float armLSlamAngle = 60f;
            float armRSlamAngle = -60f;
            float shoulderLSlamAngle = -25f;
            float shoulderRSlamAngle = 25f;

            float armLFireAngle = -45f;
            float armRFireAngle = 45f;
            float shoulderLFireAngle = 25f;
            float shoulderRFireAngle = -25f;

            float finalArmLAngle, finalArmRAngle;
            float finalShoulderLAngle, finalShoulderRAngle;

            boolean sparkTriggered = sparkTriggeredMap.containsKey(ship) ? sparkTriggeredMap.get(ship) : false;

            if (charge > 0f) {
                // Phase 1: Slam inward
                if (charge < 0.15f) {
                    float t = charge / 0.15f;
                    t = (float) Math.pow(t, 6.0f); // jerk slam
                    finalArmLAngle = lerp(armLStartAngle, armLSlamAngle, t);
                    finalArmRAngle = lerp(armRStartAngle, armRSlamAngle, t);
                    finalShoulderLAngle = lerp(shoulderLStartAngle, shoulderLSlamAngle, t);
                    finalShoulderRAngle = lerp(shoulderRStartAngle, shoulderRSlamAngle, t);
                    sparkTriggeredMap.put(ship, false); // reset spark state
                }
                // Phase 2: Hold
                else if (charge < 0.65f) {
                    finalArmLAngle = armLSlamAngle;
                    finalArmRAngle = armRSlamAngle;
                    finalShoulderLAngle = shoulderLSlamAngle;
                    finalShoulderRAngle = shoulderRSlamAngle;

                    if (!sparkTriggered) {
                        Vector2f sparkLoc = MathUtils.getPointOnCircumference(ship.getLocation(), 63f, ship.getFacing());
                        engine.addHitParticle(sparkLoc, new Vector2f(), 20f, 1f, 0.2f, new Color(255, 220, 100));
                        engine.addHitParticle(sparkLoc, new Vector2f(), 30f, 0.7f, 0.4f, new Color(255, 150, 50));
                        engine.spawnEmpArcVisual(
                                sparkLoc, ship,
                                MathUtils.getPointOnCircumference(ship.getLocation(), 10f, ship.getFacing() + MathUtils.getRandomNumberInRange(-30f, 30f)),
                                null,
                                6f,
                                new Color(255, 200, 180),
                                new Color(255, 120, 60)
                        );
                        sparkTriggeredMap.put(ship, true);
                    }
                }
                // Phase 3: Snap outward (fire)
                else {
                    float t = (charge - 0.65f) / 0.35f;
                    t = (float) Math.pow(t, 0.3f); // snappy open
                    finalArmLAngle = lerp(armLSlamAngle, armLFireAngle, t);
                    finalArmRAngle = lerp(armRSlamAngle, armRFireAngle, t);
                    finalShoulderLAngle = lerp(shoulderLSlamAngle, shoulderLFireAngle, t);
                    finalShoulderRAngle = lerp(shoulderRSlamAngle, shoulderRFireAngle, t);
                }

                recoveryProgress = 0f;
            } else {
                // Recovery to idle
                recoveryProgress += amount;
                float t = Math.min(recoveryProgress / RECOVERY_DURATION, 1f);
                finalArmLAngle = lerp(armLFireAngle, armLStartAngle, t);
                finalArmRAngle = lerp(armRFireAngle, armRStartAngle, t);
                finalShoulderLAngle = lerp(shoulderLFireAngle, shoulderLStartAngle, t);
                finalShoulderRAngle = lerp(shoulderRFireAngle, shoulderRStartAngle, t);
                sparkTriggeredMap.put(ship, false); // reset for next charge
            }

            // Apply rotation
            rotateWeapon(ship, ARM_L, facing + finalArmLAngle);
            rotateWeapon(ship, ARM_R, facing + finalArmRAngle);
            rotateWeapon(ship, SHOULDER_L, facing + finalShoulderLAngle);
            rotateWeapon(ship, SHOULDER_R, facing + finalShoulderRAngle);
        }
    }

    private void rotateWeapon(ShipAPI ship, String slotId, float angle) {
        WeaponAPI weapon = getWeaponBySlot(ship, slotId);
        if (weapon != null) {
            weapon.setCurrAngle(angle);
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
