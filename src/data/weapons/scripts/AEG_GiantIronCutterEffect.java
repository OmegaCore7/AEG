package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_GiantIronCutterEffect implements EveryFrameWeaponEffectPlugin {
    private boolean charging = false;
    private boolean fired = false;
    private float chargeTimer = 0f;

    private static final float TRANSFORM_DURATION = 3f;
    private static final float TOTAL_CHARGE_TIME = 6f;
    private static final int TRANSFORM_FRAMES = 16;

    private float particleTimer = 0f;
    private static final float PARTICLE_INTERVAL = 0.05f;
    private static final int PARTICLE_COUNT = 3;

    private int lastFrame = -1;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon == null) return;

        float cooldown = weapon.getCooldownRemaining();
        int ammo = weapon.getAmmo();

        // Begin charge when weapon enters cooldown
        if (cooldown > 0 && !charging && !fired) {
            charging = true;
            fired = false;
            chargeTimer = 0f;
            lastFrame = -1;
        }

        // CHARGING: 0s–3s → animate frames 0–15
        if (charging && chargeTimer < TRANSFORM_DURATION) {
            chargeTimer += amount;

            int frame = Math.min((int)((chargeTimer / TRANSFORM_DURATION) * TRANSFORM_FRAMES), TRANSFORM_FRAMES - 1);
            if (frame != lastFrame) {
                weapon.getAnimation().setFrame(frame);
                lastFrame = frame;
            }

            handleParticles(amount, engine, weapon);
        }

        // CHARGE COMPLETE: 3s–6s → hold frame 15
        if (charging && chargeTimer >= TRANSFORM_DURATION && chargeTimer < TOTAL_CHARGE_TIME && !fired) {
            if (lastFrame != 15) {
                weapon.getAnimation().setFrame(15);
                lastFrame = 15;
            }
            handleParticles(amount, engine, weapon);
        }

        // FIRING: at 6s mark → switch to frame 16
        if (charging && chargeTimer >= TOTAL_CHARGE_TIME && !fired) {
            weapon.getAnimation().setFrame(16);
            lastFrame = 16;
            fired = true;
        }
        // POST-FIRE COOLDOWN & AMMO SPENT: hold frame 17
        if (fired || cooldown > 0 || weapon.getAmmo() <= 0) {
            if (lastFrame != 17) {
                weapon.getAnimation().setFrame(17);
                lastFrame = 17;
            }
            handleParticles(amount, engine, weapon);
        }


        // RESET: once reloaded & ammo restored → frame 0
        if (fired && cooldown <= 0 && ammo > 0) {
            weapon.getAnimation().setFrame(0);
            lastFrame = 0;
            fired = false;
            charging = false;
            chargeTimer = 0f;
        }
    }

    private void handleParticles(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        particleTimer += amount;
        if (particleTimer >= PARTICLE_INTERVAL) {
            particleTimer -= PARTICLE_INTERVAL;
            spawnPhotonParticles(engine, weapon);
        }
    }

    private void spawnPhotonParticles(CombatEngineAPI engine, WeaponAPI weapon) {
        Vector2f loc = weapon.getLocation();
        Vector2f shipVel = weapon.getShip().getVelocity();

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float angle = (float)(Math.random() * 360f);
            float distance = 4f + (float)(Math.random() * 12f);
            float speed = 10f + (float)Math.random() * 20f;

            Vector2f offset = Misc.getUnitVectorAtDegreeAngle(angle);
            offset.scale(distance);
            Vector2f particleLoc = Vector2f.add(loc, offset, null);

            Vector2f vel = Misc.getUnitVectorAtDegreeAngle(angle + 180f);
            vel.scale(speed);
            Vector2f.add(vel, shipVel, vel);

            float size = 10f + (float)Math.random() * 20f;
            float endSize = size * (1.5f + (float)Math.random());
            float duration = 1.5f + (float)Math.random() * 1f;

            Color color = pickEnergyColor();

            engine.addNebulaParticle(
                    particleLoc,
                    vel,
                    size,
                    endSize / size,
                    0.3f,
                    0.6f,
                    duration,
                    color
            );
        }
    }

    private Color pickEnergyColor() {
        if (Math.random() < 0.5) {
            return new Color(255, 220 + (int)(Math.random() * 30), 130 + (int)(Math.random() * 50), 180);
        } else {
            return new Color(255, 160 + (int)(Math.random() * 30), 80 + (int)(Math.random() * 50), 170);
        }
    }
}
