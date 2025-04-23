package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.*;
import java.util.HashMap;
import java.util.List;

public class AEG_PhotonBarrageEffect implements EveryFrameWeaponEffectPlugin {

    private static final float TRAIL_DURATION = 0.4f;

    private boolean initialized = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip() == null || !weapon.getShip().isAlive()) return;

        // Sync rotation to the main beam weapon
        if (!initialized) {
            initialized = true;
        }
        WeaponAPI mainWeapon = null;
        for (WeaponAPI w : weapon.getShip().getAllWeapons()) {
            if ("AEG_photonbeam".equals(w.getId())) {
                mainWeapon = w;
                break;
            }
        }
        if (mainWeapon != null) {
            weapon.setCurrAngle(mainWeapon.getCurrAngle());
        }

        // Add trails to projectiles fired from this weapon
        List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
        for (DamagingProjectileAPI proj : projectiles) {

            //30 percent of projectils will not spawn trails
            if (Math.random() < 0.3f) continue;
            if ("AEG_bp_shot".equals(proj.getProjectileSpecId()) && !proj.isFading() && !proj.didDamage()) {

                // Ensure custom data is initialized (only once)
                if (proj.getCustomData().get("trailWidth") == null) {
                    float randomWidth = MathUtils.getRandomNumberInRange(5f, 20f);
                    float size = randomWidth; // Base width of beam proj is 40
                    proj.setCustomData("trailWidth", size);
                }

                // Check if the projectile already has a fixed width (i.e., has been randomized already)
                if (proj.getCustomData().get("trailWidth") == null) {
                    // Randomize width for this specific projectile (between 0.0 and 1.0 times base width)
                    float randomWidth = MathUtils.getRandomNumberInRange(5f, 20f);
                    float size = randomWidth; // Base width of beam proj is 40
                    proj.getCustomData().put("trailWidth", size); // Store this width in custom data
                }

                // Get the width that was assigned to this projectile
                float size = (float) proj.getCustomData().get("trailWidth");

                // Randomize the length of the trail (between 0.5 and 1.5 times the normal length)
                float randomTrailLengthMultiplier = MathUtils.getRandomNumberInRange(0.5f, 1f);
                float trailLength = TRAIL_DURATION * randomTrailLengthMultiplier;  // Adjust trail duration based on random factor

                // Randomize the transparency of the trail (between 0.3 and 1.0 alpha range)
                float randomAlpha = MathUtils.getRandomNumberInRange(0.3f, 1.0f);

                // Base color is a light yellow-orange (255, 200, 100)
                int baseR = 255;
                int baseG = 200;
                int baseB = 100;

                // Randomize the color to be lighter or darker (within 20% of base color)
                float rFactor = MathUtils.getRandomNumberInRange(0.8f, 1.2f); // Keep within a range (80% - 120% of base)
                float gFactor = MathUtils.getRandomNumberInRange(0.8f, 1.2f);
                float bFactor = MathUtils.getRandomNumberInRange(0.8f, 1.2f);

                // Apply the random factors to the base color and clamp them between 0 and 255
                int r = MathUtils.clamp((int) (baseR * rFactor), 0, 255);
                int g = MathUtils.clamp((int) (baseG * gFactor), 0, 255);
                int b = MathUtils.clamp((int) (baseB * bFactor), 0, 255);

                // Apply random alpha transparency
                float distanceFromProjectile = MathUtils.getDistance(proj.getLocation(), weapon.getLocation());
                float maxAlphaDistance = 100f;
                float alpha = Math.max(0, 1 - (distanceFromProjectile / maxAlphaDistance));  // Alpha fades with distance
                alpha *= randomAlpha;  // Apply random alpha variation
                Color trailColor = new Color(r, g, b, (int) (255 * alpha));

                // Load the texture as a SpriteAPI
                String texturePath = "graphics/fx/beam_weave_core.png"; // Update with your actual texture path
                SpriteAPI sprite = Global.getSettings().getSprite(texturePath);

                // Use the projectile's current velocity to get its facing angle
                float angle = proj.getFacing(); // This replaces weapon.getCurrAngle() or atan2 logic

                // Add trail for beam projectile with fixed width and randomized effects
                MagicTrailPlugin.addTrailMemberAdvanced(
                        proj, // The linked entity, i.e., the projectile itself
                        proj.hashCode(), // Unique ID for the trail
                        sprite, // Use the loaded SpriteAPI here
                        proj.getLocation(), // Starting position of the trail
                        0f, // Start speed (0f, as the trail follows the beam)
                        0f, // End speed (0f, same as start speed)
                        angle, // Angle of the trail based on the weapon's angle
                        0f, // Start angular velocity
                        0f, // End angular velocity
                        size, // Start size (width of the trail, from the projectile's width)
                        size * 0.5f, // End size (shrink over time)
                        trailColor, // Dynamic color based on velocity
                        trailColor, // Ending color, same as starting for consistency
                        1f, // Full opacity
                        0.1f, // Fade in duration
                        trailLength, // Main duration of the trail (randomized length)
                        0.1f, // Fade out duration
                        true, // Additive blending for glowing effect
                        -1f, // No texture looping
                        1000f, // Texture scroll speed
                        0f, // No texture offset
                        null, // No offset velocity
                        null, // No advanced options
                        null, // No special layer to render on
                        1f // Frame offset multiplier
                );

            }

        }
    }
}