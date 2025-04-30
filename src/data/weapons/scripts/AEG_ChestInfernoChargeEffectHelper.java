package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

public class AEG_ChestInfernoChargeEffectHelper {

    // ================================
    // 🔧 CONFIGURATION
    // ================================
    private static final float MAX_CHARGE_DURATION = 6f;

    // Plasma burst
    private static final float BASE_PARTICLE_SIZE = 30f;
    private static final float PARTICLE_SIZE_SCALE = 30f;
    private static final float BASE_PARTICLE_OPACITY = 0.2f;
    private static final float PARTICLE_OPACITY_SCALE = 0.3f;
    private static final float PARTICLE_DURATION = 0.5f;

    // EMP arcs
    private static final float ARC_CHANCE = 0.4f;
    private static final float ARC_BASE_RANGE = 20f;
    private static final float ARC_RANGE_SCALE = 40f;
    private static final Color ARC_CORE_COLOR = new Color(100, 180, 255, 160);
    private static final Color ARC_FRINGE_COLOR = new Color(180, 220, 255, 160);

    // Nebula
    private static final float NEBULA_CHANCE = 0.2f;
    private static final float NEBULA_BASE_SIZE = 15f;
    private static final float NEBULA_SIZE_SCALE = 15f;
    private static final float NEBULA_BASE_DURATION = 1f;
    private static final Color NEBULA_COLOR = new Color(50, 50, 80, 100);

    // Final flash
    private static final float EXPLOSION_RADIUS = 80f;
    private static final float EXPLOSION_DURATION = 1f;
    private static final Color FLASH_COLOR = new Color(255, 255, 255, 220);
    private static final Color FLASH_ARC_CORE = new Color(255, 255, 255, 200);
    private static final Color FLASH_ARC_FRINGE = new Color(255, 255, 200, 180);

    // ================================
    // ⚙️ LOGIC
    // ================================
    private static float chargeTime = 0f;
    private static boolean hasFired = false;
    private static AEG_ChestInfernoChargeCompleteListener listener;
    private static boolean chargeComplete = false;
    private static final Random random = new Random();

    public static void setChargeCompleteListener(AEG_ChestInfernoChargeCompleteListener newListener) {
        listener = newListener;
    }

    public static void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip() == null || !weapon.getShip().isAlive()) return;

        Vector2f weaponLoc = weapon.getLocation();
        Vector2f shipVel = weapon.getShip().getVelocity();

        // Track charge progress (normalized)
        if (weapon.getChargeLevel() > 0f && !hasFired) {
            chargeTime += amount;
            float chargeProgress = Math.min(chargeTime / MAX_CHARGE_DURATION, 1f);

            // === Animating the arms ===
            updateAnimationPose(chargeTime / MAX_CHARGE_DURATION, weapon);

            // === Plasma flicker (timed to keep particle count sane) ===
            if (engine.getTotalElapsedTime(false) % 0.2f < 0.016f) {
                engine.addSmoothParticle(
                        weaponLoc,
                        shipVel,
                        BASE_PARTICLE_SIZE + chargeProgress * PARTICLE_SIZE_SCALE,
                        0.75f + chargeProgress,
                        BASE_PARTICLE_OPACITY + chargeProgress * PARTICLE_OPACITY_SCALE,
                        new Color(255, 100, 60, 140 + random.nextInt(115))
                );
            }

            // === EMP arcs ===
            if (chargeProgress > 0.6f && Math.random() < ARC_CHANCE) {
                float angle = (float) Math.random() * 360f;
                Vector2f offset = Misc.getUnitVectorAtDegreeAngle(angle);
                offset.scale(ARC_BASE_RANGE + chargeProgress * ARC_RANGE_SCALE);
                Vector2f targetLoc = Vector2f.add(weaponLoc, offset, null);

                engine.spawnEmpArcVisual(
                        weaponLoc,
                        weapon.getShip(),
                        targetLoc,
                        null,
                        3f + random.nextInt(27),
                        ARC_CORE_COLOR,
                        ARC_FRINGE_COLOR
                );
            }

            // === Nebula wisps ===
            if (chargeProgress > 0.3f && Math.random() < NEBULA_CHANCE) {
                float angle = (float) Math.random() * 360f;
                Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);
                dir.scale(10f + chargeProgress * 20f);
                Vector2f smokeLoc = Vector2f.add(weaponLoc, dir, null);

                engine.addNebulaParticle(
                        smokeLoc,
                        new Vector2f(shipVel),
                        NEBULA_BASE_SIZE + chargeProgress * NEBULA_SIZE_SCALE,
                        1.2f + chargeProgress,
                        0.2f,
                        0.05f + random.nextFloat() * 0.5f,
                        NEBULA_BASE_DURATION + chargeProgress,
                        NEBULA_COLOR,
                        false
                );
            }

            // === Final flash ===
            if (chargeTime >= MAX_CHARGE_DURATION && !hasFired) {
                hasFired = true;
                chargeComplete = true;

                engine.spawnExplosion(weaponLoc, shipVel, FLASH_COLOR, EXPLOSION_RADIUS, EXPLOSION_DURATION);
                engine.spawnEmpArcVisual(
                        weaponLoc,
                        weapon.getShip(),
                        weaponLoc,
                        null,
                        45f,
                        FLASH_ARC_CORE,
                        FLASH_ARC_FRINGE
                );
                engine.spawnEmpArcVisual(
                        weaponLoc,
                        weapon.getShip(),
                        weaponLoc,
                        null,
                        50f,
                        new Color(255, 200, 100),
                        new Color(255, 150, 50)
                );
                if (listener != null) {
                    listener.onChargeComplete();
                }
            }
        }

        if (!weapon.isFiring()) {
            hasFired = false;
            chargeTime = 0f;
            chargeComplete = false;

            // Reset arms and shoulders
            setWeaponRotation("AEG_zero_arm_r", 78f, weapon);
            setWeaponRotation("AEG_ironcutter_l", -78f, weapon);
            setWeaponRotation("AEG_zero_shoulder_r", 0f, weapon);
            setWeaponRotation("AEG_zero_shoulder_l", 0f, weapon);
        }
    }

    // Function to get the weapon by ID
    private static WeaponAPI getWeaponById(String weaponId, WeaponAPI weapon) {
        for (WeaponAPI w : weapon.getShip().getAllWeapons()) {
            if (w.getSlot().getId().equals(weaponId)) {
                return w;
            }
        }
        return null;
    }

    public interface AEG_ChestInfernoChargeCompleteListener {
        void onChargeComplete();
    }
    private static void updateAnimationPose(float progress, WeaponAPI weapon) {
        // Start: Arms wide
        Vector2f armStart = new Vector2f(78f, 8f);       // (angle, offset)
        Vector2f shoulderStart = new Vector2f(0f, 0f);   // angle only

        // Mid (slam) pose at ~0.5
        Vector2f armMid = new Vector2f(60f, 30f);
        Vector2f shoulderMid = new Vector2f(45f, 0f); // 45° inward

        // Final (pullback) pose at 1.0
        Vector2f armEnd = new Vector2f(82f, 2f);
        Vector2f shoulderEnd = new Vector2f(25f, 0f); // slight outward

        float armAngle;
        float shoulderAngle;

        if (progress < 0.5f) {
            float t = progress / 0.5f;
            armAngle = lerp(armStart.x, armMid.x, t);
            shoulderAngle = lerp(shoulderStart.x, shoulderMid.x, t);
        } else {
            float t = (progress - 0.5f) / 0.5f;
            armAngle = lerp(armMid.x, armEnd.x, t);
            shoulderAngle = lerp(shoulderMid.x, shoulderEnd.x, t);
        }

        // Set arm rotations
        setWeaponRotation("AEG_zero_arm_r", armAngle, weapon);
        setWeaponRotation("AEG_ironcutter_l", -armAngle, weapon);

        // Set shoulder rotations
        setWeaponRotation("AEG_zero_shoulder_r", shoulderAngle, weapon);
        setWeaponRotation("AEG_zero_shoulder_l", -shoulderAngle, weapon);
    }

    // Lerp utility
    private static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    // Rotation setter (safe)
    private static void setWeaponRotation(String id, float angle, WeaponAPI base) {
        WeaponAPI w = getWeaponById(id, base);
        if (w != null) {
            w.setCurrAngle(angle);
        }
    }
}
