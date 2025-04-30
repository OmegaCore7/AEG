package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

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
    private static final Color FLASH_ARC_FRINGE = new Color(255, 255, 255, 180);

    // ================================
    // ⚙️ LOGIC
    // ================================
    private static float chargeTime = 0f;
    private static boolean hasFired = false;
    private static AEG_ChestInfernoChargeCompleteListener listener;
    private static boolean chargeComplete = false;

    public static void setChargeCompleteListener(AEG_ChestInfernoChargeCompleteListener newListener) {
        listener = newListener;
    }

    public static void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip() == null || !weapon.getShip().isAlive()) return;

        Vector2f weaponLoc = weapon.getLocation();
        Vector2f shipVel = weapon.getShip().getVelocity();

        if (weapon.getChargeLevel() > 0f && !hasFired) {
            chargeTime += amount;
            float chargeProgress = Math.min(chargeTime / MAX_CHARGE_DURATION, 1f);

            // Plasma flicker (timed to keep particle count sane)
            if (engine.getTotalElapsedTime(false) % 0.2f < 0.016f) {
                engine.addSmoothParticle(
                        weaponLoc,
                        shipVel,
                        BASE_PARTICLE_SIZE + chargeProgress * PARTICLE_SIZE_SCALE,
                        0.75f + chargeProgress * 0.5f,
                        BASE_PARTICLE_OPACITY + chargeProgress * PARTICLE_OPACITY_SCALE,
                        new Color(255, 100, 60, 140)
                );
            }

            // EMP arcs (only appear in final 20%)
            if (chargeProgress > 0.8f && Math.random() < ARC_CHANCE) {
                float angle = (float) Math.random() * 360f;
                Vector2f offset = Misc.getUnitVectorAtDegreeAngle(angle);
                offset.scale(ARC_BASE_RANGE + chargeProgress * ARC_RANGE_SCALE);
                Vector2f targetLoc = Vector2f.add(weaponLoc, offset, null);

                engine.spawnEmpArcVisual(
                        weaponLoc,
                        weapon.getShip(),
                        targetLoc,
                        null,
                        3f,
                        ARC_CORE_COLOR,
                        ARC_FRINGE_COLOR
                );
            }

            // Nebula wisps
            if (chargeProgress > 0.3f && Math.random() < NEBULA_CHANCE) {
                float angle = (float)Math.random() * 360f;
                Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);
                dir.scale(10f + chargeProgress * 20f);
                Vector2f smokeLoc = Vector2f.add(weaponLoc, dir, null);

                engine.addNebulaParticle(
                        smokeLoc,
                        new Vector2f(shipVel),
                        NEBULA_BASE_SIZE + chargeProgress * NEBULA_SIZE_SCALE,
                        1.2f + chargeProgress,
                        0.2f,
                        0.05f,
                        NEBULA_BASE_DURATION + chargeProgress,
                        NEBULA_COLOR,
                        false
                );
            }

            // Final flash
            if (chargeTime >= MAX_CHARGE_DURATION && !hasFired) {
                hasFired = true;
                chargeComplete = true;

                engine.spawnExplosion(weaponLoc, shipVel, FLASH_COLOR, EXPLOSION_RADIUS, EXPLOSION_DURATION);
                engine.spawnEmpArcVisual(
                        weaponLoc,
                        weapon.getShip(),
                        weaponLoc,
                        null,
                        8f,
                        FLASH_ARC_CORE,
                        FLASH_ARC_FRINGE
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
        }
    }

    public interface AEG_ChestInfernoChargeCompleteListener {
        void onChargeComplete();
    }
}
