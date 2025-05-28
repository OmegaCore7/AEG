package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
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
    private static boolean wasCharging = false;
    private static boolean endSoundPlayed = false;

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
    private static final Color FLASH_ARC_CORE = new Color(255, 255, 180, 200);
    private static final Color FLASH_ARC_FRINGE = new Color(255, 200, 180, 180);

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
        boolean isCharging = weapon.isFiring() && weapon.getChargeLevel() > 0f;

        // === Charging just started ===
        if (isCharging && !wasCharging) {
            Global.getSoundPlayer().playSound("system_phase_cloak_collision", 1f, 1f, weaponLoc, shipVel);
            Global.getSoundPlayer().playSound("terrain_hyperspace_lightning", 1f, 1f, weaponLoc, shipVel);
            Global.getSoundPlayer().playSound("system_nova_burst_fire", 1f, 1f, weaponLoc, shipVel);
            endSoundPlayed = false;
        }

        // === Interrupted by venting only ===
        if (!chargeComplete && wasCharging && weapon.getShip().getFluxTracker().isVenting() && !endSoundPlayed) {
            Global.getSoundPlayer().playSound("system_burn_drive_deactivate", 1f, 1f, weaponLoc, shipVel);
            endSoundPlayed = true;
        }

        wasCharging = isCharging;

        // Track charge progress (normalized)
        if (weapon.getChargeLevel() > 0f && !hasFired) {
            chargeTime += amount;
            float chargeProgress = Math.min(chargeTime / MAX_CHARGE_DURATION, 1f);

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
                if (Math.random() < 0.1) {
                    Global.getSoundPlayer().playSound("terrain_hyperspace_lightning", 1f, 1f, weaponLoc, shipVel);  // This block runs approximately 10% of the time
                }
            }

            // === Nebula wisps ===
            if (chargeProgress > 0.3f && Math.random() < NEBULA_CHANCE) {
                float angle = (float) Math.random() * 360f;
                Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);
                dir.scale(10f + chargeProgress * 40f);
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
                engine.addNebulaParticle(
                        smokeLoc,
                        new Vector2f(shipVel),
                        20 + chargeProgress * 40,
                        1.2f + chargeProgress,
                        0.2f + random.nextInt(1),
                        0.05f + random.nextFloat() * 1.5f,
                        NEBULA_BASE_DURATION + chargeProgress + random.nextInt(1),
                        new Color(255,50 + random.nextInt(180),50 + random.nextInt(150),255 - random.nextInt(105)),
                        false
                );
            }

            // === Nebula wisps ===
            if (chargeProgress > 0.3f && Math.random() < NEBULA_CHANCE) {
                float angle = (float) Math.random() * 360f;
                Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);
                dir.scale(10f + chargeProgress * 40f);
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
                chargeComplete = true;  // Set charge complete flag
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

        // === Reset when weapon stops firing ===
        if (!weapon.isFiring()) {
            hasFired = false;
            chargeTime = 0f;
            chargeComplete = false;  // Reset charge complete flag
            wasCharging = false;  // Reset this flag when firing stops
            endSoundPlayed = false;
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
}
