package data.weapons.scripts;

import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.magiclib.util.MagicRender;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.awt.Color;

public class AEG_4g_protect implements BeamEffectPlugin {

    private IntervalUtil fireInterval = new IntervalUtil(2f, 2f); // Interval set to 2 seconds
    private final Vector2f ZERO = new Vector2f();
    private static final Color PARTICLE_COLOR = new Color(255, 255, 50, 255); // Changed particle color with lighter transparency
    private boolean runOnce = false;
    private boolean runOnce2 = false;
    private WeaponAPI weapon;
    private float rippleTimer = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        weapon = beam.getWeapon();
        ShipAPI ship = weapon.getShip();

        if (!runOnce) {
            runOnce = true;
        }
        if (weapon.getChargeLevel() >= 1f) {
            if (!runOnce2)
                runOnce2 = true;
        }

        if (weapon.isFiring()) {
            fireInterval.advance(amount);
            rippleTimer += amount;

            if (fireInterval.intervalElapsed()) {
                float shieldRadius = ship.getShieldRadiusEvenIfNoShield();
                Vector2f weaponLocation = beam.getFrom(); // Use the beam's starting point

                // Add first ripple distortion effect with lighter transparency
                SpriteAPI shieldSprite = Global.getSettings().getSprite("graphics/fx/shields64.png");
                MagicRender.battlespace(
                        shieldSprite,
                        weaponLocation,
                        ZERO,
                        new Vector2f(shieldRadius, shieldRadius),
                        new Vector2f(shieldRadius + 200f, shieldRadius + 200f),
                        360f,
                        0f,
                        PARTICLE_COLOR,
                        true,
                        0.2f, // Increased brightness duration
                        0f,
                        0.5f // Lighter alpha value for transparency
                );

                // Add second ripple distortion effect after 0.45 seconds using shields256ring.png
                if (rippleTimer >= 0.45f) {
                    MagicRender.battlespace(
                            Global.getSettings().getSprite("graphics/fx/starburst_glow1.png"),
                            weaponLocation,
                            ZERO,
                            new Vector2f(shieldRadius * 0.8f, shieldRadius * 0.8f),
                            new Vector2f(shieldRadius + 80f, shieldRadius + 80f),
                            360f,
                            0f,
                            new Color(255, 200, 100, 255), // Lighter yellow
                            true,
                            0.3f, // Duration of the ripple effect
                            0f,
                            0.5f // Alpha value for the ripple effect
                    );
                }

                // Add third ripple distortion effect after 0.9 seconds using shields256ringb.png
                if (rippleTimer >= 0.9f) {
                    MagicRender.battlespace(
                            Global.getSettings().getSprite("graphics/fx/wormhole_ring.png"),
                            weaponLocation,
                            ZERO,
                            new Vector2f(shieldRadius * 0.6f, shieldRadius * 0.6f),
                            new Vector2f(shieldRadius + 60f, shieldRadius + 60f),
                            360f,
                            0f,
                            new Color(255, 200, 100, 255), // Light orange
                            true,
                            0.4f, // Duration of the ripple effect
                            0f,
                            0.6f // Alpha value for the ripple effect
                    );
                }

                // Add Starhalo ripple distortion effect last with lighter transparency
                if (rippleTimer >= 1.35f) {
                    MagicRender.battlespace(
                            Global.getSettings().getSprite("graphics/fx/star_halo.png"),
                            weaponLocation,
                            ZERO,
                            new Vector2f(50f, 50f),
                            new Vector2f(200f, 200f),
                            360f,
                            0f,
                            new Color(255, 255, 50, 255), // Lighter transparency
                            true,
                            0.5f, // Duration of the ripple effect
                            0f,
                            0.5f // Alpha value for the ripple effect
                    );

                    // Reset the ripple timer
                    rippleTimer = 0f;
                }
            }
        }
    }
}