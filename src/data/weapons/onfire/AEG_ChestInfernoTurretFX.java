package data.weapons.onfire;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_ChestInfernoTurretFX implements EveryFrameWeaponEffectPlugin {

    private boolean hasFired = false;
    private float timer = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip() == null || !weapon.getShip().isAlive()) return;

        Vector2f weaponLoc = weapon.getLocation();
        Vector2f shipVel = weapon.getShip().getVelocity();
        timer += amount;

        // Weapon firing visual overload
        if (weapon.getChargeLevel() > 0f && !hasFired) {
            hasFired = true;

            // 🔥 Plasma burst
            engine.addSmoothParticle(
                    weaponLoc,
                    shipVel,
                    180f,
                    1.5f,
                    0.5f,
                    new Color(255, 80, 40, 200)
            );

            // ⚡ Giant lightning arcs
            for (int i = 0; i < 4; i++) {
                float angle = (float)Math.random() * 360f;
                Vector2f offset = Misc.getUnitVectorAtDegreeAngle(angle);
                offset.scale(100f + (float)Math.random() * 120f);
                Vector2f targetLoc = Vector2f.add(weaponLoc, offset, null);

                engine.spawnEmpArcVisual(
                        weaponLoc,
                        weapon.getShip(),
                        targetLoc,
                        null,
                        10f,
                        new Color(60, 140, 255, 255),
                        new Color(150, 200, 255, 255)

                );
            }

            // 🌌 Smoke/nebula bursts
            for (int i = 0; i < 8; i++) {
                float angle = (float)Math.random() * 360f;
                Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);
                dir.scale(30f + (float)Math.random() * 50f);
                Vector2f smokeLoc = Vector2f.add(weaponLoc, dir, null);

                engine.addNebulaParticle(
                        smokeLoc,
                        new Vector2f(shipVel),
                        50f + (float)Math.random() * 50f,
                        2f,
                        0.3f,
                        0.1f,
                        2f + (float)Math.random() * 2f,
                        new Color(40, 40, 60, 160),
                        false
                );
            }
        }

        if (!weapon.isFiring()) {
            hasFired = false;
        }
    }
}