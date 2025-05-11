package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_IronCutterLeftEffect implements EveryFrameWeaponEffectPlugin {

    private static final int PARTICLE_COUNT = 6;
    private static final float PARTICLE_INTERVAL = 0.1f;
    private static final float OFFSET_X = 10f;
    private static final float OFFSET_Y = 4f;

    private float timer = 0f;
    private boolean wasDepleted = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;

        if (weapon.getAmmo() <= 0) {
            timer += amount;

            if (timer >= PARTICLE_INTERVAL) {
                timer = 0f;
                spawnRegenParticles(engine, weapon);
            }

            wasDepleted = true;
        } else if (wasDepleted) {
            // Reset once ammo is restored
            timer = 0f;
            wasDepleted = false;
        }
    }

    private void spawnRegenParticles(CombatEngineAPI engine, WeaponAPI weapon) {
        Vector2f loc = getOffsetPoint(weapon, OFFSET_X, OFFSET_Y);
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

            float size = 6f + (float)Math.random() * 18f;
            float endSize = size * (1.5f + (float)Math.random());
            float duration = 1.2f + (float)Math.random();

            engine.addNebulaParticle(
                    particleLoc,
                    vel,
                    size,
                    endSize / size,
                    0.3f,
                    0.6f,
                    duration,
                    pickEnergyColor()
            );
        }
    }

    private Color pickEnergyColor() {
        if (Math.random() < 0.5f) {
            return new Color(255, 180 + (int)(Math.random() * 40), 100 + (int)(Math.random() * 50), 180);
        } else {
            return new Color(255, 140 + (int)(Math.random() * 30), 80 + (int)(Math.random() * 50), 170);
        }
    }

    private Vector2f getOffsetPoint(WeaponAPI weapon, float offsetX, float offsetY) {
        Vector2f weaponLoc = weapon.getLocation();
        float angle = weapon.getCurrAngle();
        Vector2f offset = new Vector2f(offsetX, offsetY);
        Vector2f rotated = Misc.rotateAroundOrigin(offset, angle);
        return Vector2f.add(weaponLoc, rotated, null);
    }
}
