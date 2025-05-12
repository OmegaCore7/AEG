package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
//For the Great Iron Cutter
public class AEG_IronCutterVisuals {
    // Define nebula parameters
    private static final float RIPPLE_DURATION = 3f;
    private static final float LIGHTNING_INTERVAL = 0.5f;

    private RippleDistortion ripple;
    private float rippleElapsed = 0f;
    private float lightningTimer = 0f;

    public void update(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;

        float chargeLevel = weapon.getChargeLevel();

        // Update ripple effect
        updateRippleEffect(amount, engine, weapon);

        // Handle lightning arcs
        handleLightning(engine, weapon, chargeLevel, amount);

        // Generate energy mist / nebula buildup
        generateNebula(engine, weapon, chargeLevel);
    }



    public void updateRippleEffect(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;

        rippleElapsed += amount;

        if (rippleElapsed >= RIPPLE_DURATION) {
            Vector2f waveLoc = getOffsetPoint(weapon, 68f, -4f);
            WaveDistortion wave = new WaveDistortion(waveLoc, new Vector2f());
            wave.setIntensity(30f + 70f * weapon.getChargeLevel()); // Increase intensity with charge
            wave.setSize(200f + 300f * weapon.getChargeLevel());    // Increase size with charge
            wave.setLifetime(0.4f);                                  // Duration remains constant
            wave.setArc(0f, 360f);                                   // Full circle
            wave.fadeOutIntensity(0.5f);                             // Smooth fade
            DistortionShader.addDistortion(wave);

            rippleElapsed = 0f;
        }
    }

    private void handleLightning(CombatEngineAPI engine, WeaponAPI weapon, float chargeLevel, float amount) {
        lightningTimer += amount;

        if (lightningTimer >= LIGHTNING_INTERVAL) {
            lightningTimer = 0f;

            // Calculate lightning color based on charge level
            Color lightningColor = new Color(
                    (int)(100 + 155 * chargeLevel), 0, 0,
                    (int)(100 + 155 * chargeLevel)
            );

            // Determine lightning length and size based on charge level
            float lightningLength = 10f + 1000f * chargeLevel;
            float lightningWidth = 20f + 60f * chargeLevel;

            // Generate lightning arcs
            for (int i = 0; i < 3; i++) {
                Vector2f loc = getOffsetPoint(weapon, 68f, -4f);
                Vector2f target = Misc.getPointAtRadius(loc, lightningLength);

                engine.spawnEmpArcVisual(
                        loc,
                        weapon.getShip(),
                        target,
                        null,
                        lightningWidth,
                        lightningColor,
                        lightningColor
                );

            }
            ShipAPI ship = engine.getPlayerShip();
            // Example: Generating lightning arcs from multiple sources
            for (WeaponAPI sourceWeapon : ship.getAllWeapons()) {
                if (sourceWeapon != weapon) {
                    Vector2f sourceLoc = sourceWeapon.getLocation();
                    Vector2f targetLoc = getOffsetPoint(weapon, 68f, -4f);
                    float arcLength = MathUtils.getDistance(sourceLoc, targetLoc);
                    float arcWidth = Math.min(arcLength / 5f, 20f); // Adjust arc width based on distance
                    Color arcColor = new Color(255, 50 + MathUtils.getRandom().nextInt(150),50, 255 - MathUtils.getRandom().nextInt(100)); // Photon Energy
                    engine.spawnEmpArcVisual(sourceLoc, weapon.getShip(), targetLoc, null, arcWidth, arcColor, arcColor);
                }
            }
        }

    }

    private void generateNebula(CombatEngineAPI engine, WeaponAPI weapon, float chargeLevel) {
        Vector2f loc = getOffsetPoint(weapon, 68f, -4f);

        // Get weapon's current angle and create a backward wind vector
        float weaponAngle = weapon.getCurrAngle(); // in degrees
        float windSpeed = 100f + 300f * chargeLevel;

        Vector2f windDirection = Misc.getUnitVectorAtDegreeAngle(weaponAngle + 180f);
        windDirection.scale(windSpeed); // Backward from the blade

        float baseRadiusX = 75f + 500f * chargeLevel; //Length
        float baseRadiusY = 25f + 800f * chargeLevel; // Width

        for (int i = 0; i < 10; i++) {
            float angle = (float)(Math.random() * 360f);
            float x = (float)(Math.cos(Math.toRadians(angle)) * MathUtils.getRandomNumberInRange(0, baseRadiusX));
            float y = (float)(Math.sin(Math.toRadians(angle)) * MathUtils.getRandomNumberInRange(0, baseRadiusY));
            Vector2f offset = new Vector2f(x, y);

            // Rotate the offset to match weapon orientation
            Vector2f rotatedOffset = Misc.rotateAroundOrigin(offset, weaponAngle);

            Vector2f particleLoc = Vector2f.add(loc, rotatedOffset, null);

            // Add a bit of turbulence to the wind
            Vector2f velocity = new Vector2f(windDirection);
            velocity.x += MathUtils.getRandomNumberInRange(-30f, 30f);
            velocity.y += MathUtils.getRandomNumberInRange(-30f, 30f);

            float size = 20f + (float)Math.random() * 40f;
            float endSizeMult = 1.5f + (float)Math.random();
            float duration = 1.5f + (float)Math.random() * 1.5f;

            if (Math.random() < 0.5f) {
                engine.addNebulaSmoothParticle(particleLoc, velocity, size, endSizeMult, 0.3f, 0.6f, duration, getNebulaColor());
            } else {
                engine.addNebulaParticle(particleLoc, velocity, size, endSizeMult, 0.3f, 0.6f, duration, getNebulaColor());
            }
        }
    }


    private Color getNebulaColor() {
        int red = 180 + MathUtils.getRandom().nextInt(75);  // 180–255
        int green = 30 + MathUtils.getRandom().nextInt(100); // 30–130
        int blue = MathUtils.getRandom().nextInt(60);       // 0–60
        int alpha = 50 + MathUtils.getRandom().nextInt(205); // 50–255
        return new Color(red, green, blue, alpha);
    }
    private Vector2f getOffsetPoint(WeaponAPI weapon, float offsetX, float offsetY) {
        Vector2f weaponLoc = weapon.getLocation();
        float angle = weapon.getCurrAngle(); // in degrees

        // Offset vector
        Vector2f offset = new Vector2f(offsetX, offsetY);

        // Rotate the offset by weapon's angle
        Vector2f rotated = Misc.rotateAroundOrigin(offset, angle);

        // Final position = weapon location + rotated offset
        return Vector2f.add(weaponLoc, rotated, null);
    }
}
