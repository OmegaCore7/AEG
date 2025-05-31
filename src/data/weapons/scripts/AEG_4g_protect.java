package data.weapons.scripts;

import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.magiclib.util.MagicRender;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.awt.Color;

public class AEG_4g_protect implements BeamEffectPlugin {

    private IntervalUtil fireInterval = new IntervalUtil(2f, 2f);
    private final Vector2f ZERO = new Vector2f();
    private static final Color PARTICLE_COLOR = new Color(255, 255, 50, 255);
    private boolean runOnce = false;
    private boolean runOnce2 = false;
    private WeaponAPI weapon;
    private float rippleTimer = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        // ✅ Null safety check: essential for early-game or no-enemy scenarios
        if (engine == null || beam == null || beam.getWeapon() == null || beam.getWeapon().getShip() == null) return;

        weapon = beam.getWeapon();
        ShipAPI ship = weapon.getShip();
        if (!engine.isEntityInPlay(ship)) return; // skip if ship isn't fully initialized

        // ✅ Safe shield radius fallback
        float shieldRadius = ship.getShield() != null ? ship.getShieldRadiusEvenIfNoShield() : 100f;

        Vector2f weaponLocation = beam.getFrom();

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
                // ✅ Check if sprite exists before rendering (shields64.png)
                SpriteAPI shieldSprite = Global.getSettings().getSprite("graphics/fx/shields64.png");
                if (shieldSprite != null) {
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
                            0.2f,
                            0f,
                            0.5f
                    );
                }

                // ✅ starburst_glow1.png
                if (rippleTimer >= 0.45f) {
                    SpriteAPI sprite2 = Global.getSettings().getSprite("graphics/fx/starburst_glow1.png");
                    if (sprite2 != null) {
                        MagicRender.battlespace(
                                sprite2,
                                weaponLocation,
                                ZERO,
                                new Vector2f(shieldRadius * 0.8f, shieldRadius * 0.8f),
                                new Vector2f(shieldRadius + 80f, shieldRadius + 80f),
                                360f,
                                0f,
                                new Color(255, 200, 100, 255),
                                true,
                                0.3f,
                                0f,
                                0.5f
                        );
                    }
                }

                // ✅ wormhole_ring.png
                if (rippleTimer >= 0.9f) {
                    SpriteAPI sprite3 = Global.getSettings().getSprite("graphics/fx/wormhole_ring.png");
                    if (sprite3 != null) {
                        MagicRender.battlespace(
                                sprite3,
                                weaponLocation,
                                ZERO,
                                new Vector2f(shieldRadius * 0.6f, shieldRadius * 0.6f),
                                new Vector2f(shieldRadius + 60f, shieldRadius + 60f),
                                360f,
                                0f,
                                new Color(255, 200, 100, 255),
                                true,
                                0.4f,
                                0f,
                                0.6f
                        );
                    }
                }

                // ✅ star_halo.png
                if (rippleTimer >= 1.35f) {
                    SpriteAPI sprite4 = Global.getSettings().getSprite("graphics/fx/star_halo.png");
                    if (sprite4 != null) {
                        MagicRender.battlespace(
                                sprite4,
                                weaponLocation,
                                ZERO,
                                new Vector2f(50f, 50f),
                                new Vector2f(200f, 200f),
                                360f,
                                0f,
                                new Color(255, 255, 50, 255),
                                true,
                                0.5f,
                                0f,
                                0.5f
                        );
                    }

                    // Reset the ripple timer
                    rippleTimer = 0f;
                }
            }
        }
    }
}
