package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class AEG_transformationFX {

    private static final Color PARTICLE_COLOR = new Color(217, 255, 161, 255);

    public AEG_transformationFX() {
        // No need to generate points anymore
    }

    public void updateParticles(ShipAPI ship, float chargeLevel) {
        // Update particles only at engines and shoulder decos
        updateEngineParticles(ship, chargeLevel);
        updateShoulderParticles(ship, chargeLevel);
    }

    private void updateEngineParticles(ShipAPI ship, float chargeLevel) {
        // Logic to update particles at engines
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSlot().getId().startsWith("engine")) {
                Vector2f location = weapon.getLocation();
                Vector2f velocity = MathUtils.getPointOnCircumference(null, 50f * chargeLevel, MathUtils.getRandomNumberInRange(0f, 360f));
                float size = 10f * chargeLevel;
                float duration = 1f * chargeLevel;
                Global.getCombatEngine().addHitParticle(location, velocity, size, 1f, duration, PARTICLE_COLOR);
            }
        }
    }

    private void updateShoulderParticles(ShipAPI ship, float chargeLevel) {
        // Logic to update particles at shoulder decos
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSlot().getId().equals("AEG_broly_shoulder_l") || weapon.getSlot().getId().equals("AEG_broly_shoulder_r")) {
                Vector2f location = weapon.getLocation();
                Vector2f velocity = MathUtils.getPointOnCircumference(null, 50f * chargeLevel, MathUtils.getRandomNumberInRange(0f, 360f));
                float size = 10f * chargeLevel;
                float duration = 1f * chargeLevel;
                Global.getCombatEngine().addHitParticle(location, velocity, size, 1f, duration, PARTICLE_COLOR);
            }
        }
    }

    public void createTransformationEffect(ShipAPI ship) {
        // Ensure hair animation plays before any boosts
        syncHairDecoWeapon(ship);
    }

    public void fadeOutParticles(ShipAPI ship) {
        // Fade out particles at engines and shoulder decos
        fadeOutEngineParticles(ship);
        fadeOutShoulderParticles(ship);
    }

    private void fadeOutEngineParticles(ShipAPI ship) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSlot().getId().startsWith("engine")) {
                Vector2f location = weapon.getLocation();
                Vector2f velocity = MathUtils.getPointOnCircumference(null, 50f, MathUtils.getRandomNumberInRange(0f, 360f));
                Global.getCombatEngine().addHitParticle(location, velocity, 10f, 1f, 1f, new Color(217, 255, 161, 100));
            }
        }
    }

    private void fadeOutShoulderParticles(ShipAPI ship) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSlot().getId().equals("AEG_broly_shoulder_l") || weapon.getSlot().getId().equals("AEG_broly_shoulder_r")) {
                Vector2f location = weapon.getLocation();
                Vector2f velocity = MathUtils.getPointOnCircumference(null, 50f, MathUtils.getRandomNumberInRange(0f, 360f));
                Global.getCombatEngine().addHitParticle(location, velocity, 10f, 1f, 1f, new Color(217, 255, 161, 100));
            }
        }
    }

    private void syncHairDecoWeapon(ShipAPI ship) {
        if (ship == null) return;

        WeaponAPI headWeapon = null;
        WeaponAPI hairWeapon = null;

        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if ("WS0011".equals(weapon.getSlot().getId())) {
                headWeapon = weapon;
            } else if ("WS0012".equals(weapon.getSlot().getId())) {
                hairWeapon = weapon;
            }
        }

        if (headWeapon != null && hairWeapon != null) {
            hairWeapon.setCurrAngle(headWeapon.getCurrAngle());
            hairWeapon.getAnimation().play();
            hairWeapon.getAnimation().setFrame(0);
            hairWeapon.getAnimation().setFrameRate(12f); // Play frames 00-12

            final WeaponAPI finalHairWeapon = hairWeapon;
            final WeaponAPI finalHeadWeapon = headWeapon;
            Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                private boolean loopStarted = false;

                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    finalHairWeapon.setCurrAngle(finalHeadWeapon.getCurrAngle());
                    if (finalHairWeapon.getAnimation().getFrame() >= 12 && !loopStarted) {
                        finalHairWeapon.getAnimation().setFrame(6);
                        finalHairWeapon.getAnimation().setFrameRate(12f / (12 - 6)); // Loop frames 6-12
                        loopStarted = true;
                    }
                }
            });
        }
    }

    public void reset() {
        // Reset any necessary state
    }
}