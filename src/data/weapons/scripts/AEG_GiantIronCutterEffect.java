package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class AEG_GiantIronCutterEffect implements EveryFrameWeaponEffectPlugin {

    private int frameIndex = 0;
    private float firingTimer = 0f;
    private boolean isFiring = false;

    private static final float FIRE_DURATION = 10f;   // Hold frame 16 for 10 seconds
    private static final int PARTICLE_COUNT = 5;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;

        float chargeLevel = weapon.getChargeLevel();

        // If we're in the firing phase (after full charge), hold frame 16
        if (isFiring) {
            firingTimer += amount;

            // Stay on frame 16 during the 10s firing/regeneration
            weapon.getAnimation().setFrame(16);

            // You can optionally spawn particles continuously here
            spawnPhotonParticles(engine, weapon);

            if (firingTimer >= FIRE_DURATION) {
                // After 10s, return to idle frame
                weapon.getAnimation().setFrame(0);
                isFiring = false;
                firingTimer = 0f;
            }

            return; // Skip further animation logic during firing phase
        }

        // Not firing yet: charging phase from frame 0 to 15
        if (chargeLevel > 0f && chargeLevel < 1f) {
            frameIndex = (int)(chargeLevel * 15f);
            weapon.getAnimation().setFrame(frameIndex);
        }

        // Just hit full charge: begin firing phase
        else if (chargeLevel == 1f && !isFiring) {
            isFiring = true;
            firingTimer = 0f;
            frameIndex = 16;
            weapon.getAnimation().setFrame(frameIndex);
            spawnPhotonParticles(engine, weapon);
        }

        // Idle / cooldown
        else if (chargeLevel <= 0f && !isFiring) {
            weapon.getAnimation().setFrame(0);
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
