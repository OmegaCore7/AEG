package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_4g_left_boltdriver implements EveryFrameWeaponEffectPlugin {
    private float chargeupTimer = 0f;
    private final float chargeupDuration = 1.2f; // Duration of chargeup before burst starts
    private boolean isCharging = false;
    private boolean isBurstActive = false;
    private int shotsRemaining = 0;

    private float fireTimer = 0f;
    private float fireInterval = 0.25f;
    private final float rampupRate = 0.015f;
    private final float minInterval = 0.05f;

    private float heatLevel = 0f;
    private final float heatIncreasePerShot = 0.08f;
    private final float heatDecayRate = 0.5f;

    private float postBurstCooldown = 0f;
    private final float postBurstDuration = 5f;

    private final int BURST_SHOT_COUNT = 50;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (engine.isPaused() || ship == null || !ship.isAlive()) return;

        // === FRAME ANIMATION ===
        if (ship.getSelectedGroupAPI() != null && ship.getSelectedGroupAPI().getActiveWeapon() == weapon) {
            weapon.getAnimation().setFrame(1);
        } else {
            weapon.getAnimation().setFrame(0);
        }

        // === GOLDION MODE CHECK ===
        if (!Boolean.TRUE.equals(ship.getCustomData().get("goldion_active"))) {
            resetState();
            return;
        }


        // === INITIATE CHARGEUP WHEN FIRED ===
        if (weapon.getChargeLevel() > 0f && !isBurstActive && !isCharging && postBurstCooldown <= 0f) {
            isCharging = true;
            chargeupTimer = chargeupDuration;
        }

// === HANDLE CHARGEUP EFFECT ===
        if (isCharging) {
            chargeupTimer -= amount;

            spawnChargeupFX(engine, weapon); // Visual or sound FX

            if (chargeupTimer <= 0f) {
                isCharging = false;
                isBurstActive = true;
                shotsRemaining = BURST_SHOT_COUNT;
                fireTimer = 0f;
                fireInterval = 0.25f;
            }
        }

        // === HANDLE BURST LOGIC ===
        if (isBurstActive && shotsRemaining > 0) {
            fireTimer -= amount;

            if (fireTimer <= 0f) {
                fireShot(engine, weapon, ship);
                fireTimer = fireInterval;
                fireInterval = Math.max(minInterval, fireInterval - rampupRate);
                shotsRemaining--;
                heatLevel = Math.min(1f, heatLevel + heatIncreasePerShot);
            }

            if (shotsRemaining <= 0) {
                isBurstActive = false;
                postBurstCooldown = postBurstDuration;
                if (postBurstCooldown > 0f) {
                    postBurstCooldown -= amount;

                    // Spawn some cooldown particles every frame (or every few frames)
                    if (Math.random() < 0.1) { // 10% chance each frame to spawn some particles
                        spawnCooldownFX(engine, weapon);
                    }
                }
            }
        }

        // === HANDLE COOLDOWN ===
        if (postBurstCooldown > 0f) {
            postBurstCooldown -= amount;
        }

        // === HEAT GLOW ===
        if (heatLevel > 0f) {
            Vector2f heatLoc = weapon.getFirePoint(0);
            float glowSize = 30f + 60f * heatLevel;
            float glowAlpha = 0.5f + 0.5f * heatLevel;

            engine.addSmoothParticle(
                    heatLoc,
                    ship.getVelocity(),
                    glowSize,
                    1.5f + 0.5f * heatLevel,
                    0.1f,
                    new Color(
                            255,
                            (int) (100 + 120 * heatLevel),
                            (int) (50 + 50 * heatLevel),
                            (int) (200 * glowAlpha)
                    )
            );
            heatLevel = Math.max(0f, heatLevel - heatDecayRate * amount);
        }
    }

    private void fireShot(CombatEngineAPI engine, WeaponAPI weapon, ShipAPI ship) {
        Vector2f loc = weapon.getFirePoint(0); // uses proper offset
        float angle = weapon.getCurrAngle();
        int owner = ship.getOwner();

        engine.addPlugin(new AEG_4g_helixSpear(ship, loc, angle, engine, owner));
        Global.getSoundPlayer().playSound("chaingun_fire", 1f, 1f, loc, ship.getVelocity());

        // FX
        engine.addSmoothParticle(loc, ship.getVelocity(), 40f, 1.5f, 0.1f, new Color(255, 240, 100, 200));
        engine.spawnExplosion(loc, ship.getVelocity(), new Color(255, 220, 80), 60f, 0.15f);
        engine.addHitParticle(loc, ship.getVelocity(), 30f, 1.2f, 0.2f, new Color(255, 150, 60));

        for (int i = 0; i < 4; i++) {
            Vector2f sparkVel = new Vector2f((float) (Math.random() - 0.5f) * 200f, (float) (Math.random() - 0.5f) * 200f);
            engine.addHitParticle(loc, sparkVel, 8f + (float) Math.random() * 12f, 0.8f, 0.3f, new Color(255, 200, 100));
        }
    }

    private void spawnCooldownFX(CombatEngineAPI engine, WeaponAPI weapon) {
        Vector2f loc = weapon.getFirePoint(0);
        if (loc == null) loc = weapon.getLocation();
        for (int i = 0; i < 6; i++) {
            Vector2f vel = new Vector2f((float) (Math.random() - 0.5f) * 50f, (float) (Math.random() - 0.5f) * 50f);
            engine.addNebulaParticle(loc, vel, 40f + (float) Math.random() * 40f, 1.5f, 0.3f, 0.4f, 1.5f, new Color(255, 255, 200, 180));
        }
    }
    private void resetState() {
        isBurstActive = false;
        isCharging = false;
        chargeupTimer = 0f;
        shotsRemaining = 0;
        fireTimer = 0f;
        fireInterval = 0.25f;
        heatLevel = 0f;
        postBurstCooldown = 0f;
    }

    private void spawnChargeupFX(CombatEngineAPI engine, WeaponAPI weapon) {
        Vector2f loc = weapon.getFirePoint(0);
        if (loc == null) loc = weapon.getLocation();

        float progress = 1f - (chargeupTimer / chargeupDuration); // 0 → 1 as it charges
        float size = 40f + 80f * progress;
        float alpha = 0.3f + 0.7f * progress;
        float intensity = 1.2f + 1.0f * progress;

        // Glow color shifts from blue to gold
        Color color = new Color(
                clamp((int)(100 + 155 * progress), 0, 255),
                clamp((int)(150 + 100 * progress), 0, 255),
                clamp((int)(255 - 155 * progress), 0, 255),
                clamp((int)(200 * alpha), 0, 255)
        );


        engine.addSmoothParticle(loc, weapon.getShip().getVelocity(), size, intensity, 0.1f, color);

        // Add some electric arc sparks at full charge
        if (progress > 0.7f && Math.random() < 0.15f) {
            Vector2f sparkVel = new Vector2f((float)(Math.random() - 0.5f) * 150f, (float)(Math.random() - 0.5f) * 150f);
            engine.addHitParticle(loc, sparkVel, 20f + (float)Math.random() * 10f, 1.5f, 0.2f,
                    new Color(255, 220, 150, 255));
        }

        // Play a rising pitch sound (optional, needs a loop or variant)
        if (Math.random() < 0.1) {
            float pitch = 0.8f + 0.4f * progress;
            Global.getSoundPlayer().playSound("system_nova_burst_fire", 1f, pitch, loc, weapon.getShip().getVelocity());
        }
    }
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

}

