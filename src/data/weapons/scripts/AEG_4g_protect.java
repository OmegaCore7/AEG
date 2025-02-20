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
    private static final Color PARTICLE_COLOR = new Color(255, 255, 50, 255); // Changed particle color
    private boolean runOnce = false;
    private boolean runOnce2 = false;
    private WeaponAPI weapon;

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

            if (fireInterval.intervalElapsed()) {
                float shieldRadius = ship.getShieldRadiusEvenIfNoShield();
                Vector2f weaponLocation = beam.getFrom(); // Use the beam's starting point

                // Add single large circular distortion effect centered at the weapon's firing point using the shield sprite
                SpriteAPI shieldSprite = Global.getSettings().getSprite("graphics/fx/shields64.png");
                MagicRender.battlespace(
                        shieldSprite,
                        weaponLocation,
                        ZERO,
                        new Vector2f(shieldRadius, shieldRadius),
                        new Vector2f(shieldRadius + 100f, shieldRadius + 100f),
                        360f,
                        0f,
                        PARTICLE_COLOR,
                        true,
                        0.2f, // Increased brightness duration
                        0f,
                        0.7f // Increased alpha value for brightness
                );
            }
        }
    }
}