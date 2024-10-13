package data.weapons.onfire;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.shipsystems.helpers.AEG_EmpArcHelper;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

public class AEG_OmegaBlaster implements OnFireEffectPlugin {
    private static final float CHARGE_RADIUS = 200f;
    private static final int PARTICLE_COUNT = 100;
    private static final float PARTICLE_SIZE = 10f;
    private static final float PARTICLE_LIFETIME = 1f;
    private static final float PARTICLE_BRIGHTNESS = 1f;
    private static final Color[] PARTICLE_COLORS = {
            new Color(0, 255, 0, 255), // Bright green
            new Color(0, 128, 0, 255), // Darker green
            new Color(0, 255, 255, 255) // Teal
    };

    private final Random random = new Random();
    private final AEG_EmpArcHelper empArcHelper = new AEG_EmpArcHelper();

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        Vector2f weaponLocation = weapon.getLocation();

        // Create charging particle effect
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float angle = random.nextFloat() * 360f;
            float distance = random.nextFloat() * CHARGE_RADIUS;
            float x = (float) Math.cos(Math.toRadians(angle)) * distance;
            float y = (float) Math.sin(Math.toRadians(angle)) * distance;
            Vector2f particleLocation = new Vector2f(weaponLocation.x + x, weaponLocation.y + y);

            Color particleColor = PARTICLE_COLORS[random.nextInt(PARTICLE_COLORS.length)];
            float transparency = 1.0f - (distance / CHARGE_RADIUS);

            engine.addHitParticle(
                    particleLocation,
                    new Vector2f(0, 0), // No velocity
                    PARTICLE_SIZE,
                    transparency * PARTICLE_BRIGHTNESS, // Brightness
                    PARTICLE_LIFETIME,
                    particleColor
            );
        }

        // Create EMP arcs inside the charging radius
        empArcHelper.createEmpArcs(engine, weaponLocation, CHARGE_RADIUS);
    }
}
