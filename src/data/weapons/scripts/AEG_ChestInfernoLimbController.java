package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicAnim;
import sound.Sound;

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

            Boolean sparkTriggered = sparkTriggeredMap.containsKey(ship) ? sparkTriggeredMap.get(ship) : false;

            if (charge > 0f) {
                if (charge < 0.1f) {
                    // Phase 1: Slam inward (fast)
                    float t = charge / 0.1f;
                    t = (float) Math.pow(t, 6.0f);
                    finalArmLAngle = lerp(armLStartAngle, armLSlamAngle, t);
                    finalArmRAngle = lerp(armRStartAngle, armRSlamAngle, t);
                    finalShoulderLAngle = lerp(shoulderLStartAngle, shoulderLSlamAngle, t);
                    finalShoulderRAngle = lerp(shoulderRStartAngle, shoulderRSlamAngle, t);
                    sparkTriggeredMap.put(ship, false); // Reset
                } else if (charge < 0.2f) {
                    // Phase 2: Quick hold slam pose
                    finalArmLAngle = armLSlamAngle;
                    finalArmRAngle = armRSlamAngle;
                    finalShoulderLAngle = shoulderLSlamAngle;
                    finalShoulderRAngle = shoulderRSlamAngle;

                    if (!sparkTriggered) {
                        Vector2f sparkLoc = MathUtils.getPointOnCircumference(ship.getLocation(), 63f, ship.getFacing());

                        Global.getSoundPlayer().playSound("hellbore_fire", 1f, 1f, sparkLoc, new Vector2f());

                        // Fast directional welding-style sparks
                        for (int i = 0; i < 10; i++) {
                            float angle = ship.getFacing() + MathUtils.getRandomNumberInRange(-40f, 40f);
                            Vector2f velocity = MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(150f, 250f), angle);
                            engine.addHitParticle(
                                    sparkLoc,
                                    velocity,
                                    MathUtils.getRandomNumberInRange(3f, 5f),
                                    1.2f,
                                    0.2f,
                                    new Color(255, 180, 80)
                            );
                        }

                        // 🔄 ShaderLib wave distortion
                        WaveDistortion wave = new WaveDistortion(sparkLoc, new Vector2f());
                        wave.setIntensity(30f);      // shock strength
                        wave.setSize(200f);          // radius of ripple
                        wave.setLifetime(0.4f);      // duration
                        wave.setArc(0f, 360f);       // full circle
                        wave.fadeOutIntensity(0.5f); // smooth fade
                        DistortionShader.addDistortion(wave);

                        sparkTriggeredMap.put(ship, true);
                    }
                } else if (charge < 0.3f) {
                    // Phase 3: Snap outward (fast)
                    float t = (charge - 0.2f) / 0.1f;
                    t = (float) Math.pow(t, 6.0f);
                    finalArmLAngle = lerp(armLSlamAngle, armLFireAngle, t);
                    finalArmRAngle = lerp(armRSlamAngle, armRFireAngle, t);
                    finalShoulderLAngle = lerp(shoulderLSlamAngle, shoulderLFireAngle, t);
                    finalShoulderRAngle = lerp(shoulderRSlamAngle, shoulderRFireAngle, t);
                } else {
                    // Phase 4: Hold arms open
                    finalArmLAngle = armLFireAngle;
                    finalArmRAngle = armRFireAngle;
                    finalShoulderLAngle = shoulderLFireAngle;
                    finalShoulderRAngle = shoulderRFireAngle;
                }

                recoveryProgress = 0f;
            } else {
                // Recovery
                recoveryProgress += amount;
                float t = Math.min(recoveryProgress / RECOVERY_DURATION, 1f);
                finalArmLAngle = lerp(armLFireAngle, armLStartAngle, t);
                finalArmRAngle = lerp(armRFireAngle, armRStartAngle, t);
                finalShoulderLAngle = lerp(shoulderLFireAngle, shoulderLStartAngle, t);
                finalShoulderRAngle = lerp(shoulderRFireAngle, shoulderRStartAngle, t);
                sparkTriggeredMap.put(ship, false);
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
