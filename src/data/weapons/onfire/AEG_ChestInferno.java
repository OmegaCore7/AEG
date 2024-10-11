package data.weapons.onfire;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class AEG_ChestInferno implements OnFireEffectPlugin {

    private static final float DAMAGE_PER_SECOND = 1000f;
    private static final float RANGE = 700f;
    private static final float BEAM_THICKNESS = 20f;
    private static final Color BEAM_COLOR = new Color(255, 69, 0, 255); // Bright orange-red color
    private static final Color PARTICLE_COLOR = new Color(255, 140, 0, 200); // Flame color
    private static final int PARTICLE_COUNT = 50;
    private static final float PARTICLE_SIZE = 10f;
    private static final float PARTICLE_LIFETIME = 1.0f;
    private static final float PARTICLE_SPEED = 100f;
    private static final float CHARGE_UP_TIME = 5.0f; // Charge-up time in seconds

    private final IntervalUtil fireInterval = new IntervalUtil(0.1f, 0.1f);
    private float chargeUpProgress = 0f;

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        float amount = engine.getElapsedInLastFrame();
        chargeUpProgress += amount;

        if (chargeUpProgress < CHARGE_UP_TIME) {
            // Create charging particle effects
            for (int i = 0; i < PARTICLE_COUNT; i++) {
                Vector2f particleLocation = new Vector2f(weapon.getLocation());
                Vector2f particleVelocity = new Vector2f((float) Math.random() * PARTICLE_SPEED - PARTICLE_SPEED / 2, (float) Math.random() * PARTICLE_SPEED - PARTICLE_SPEED / 2);
                engine.addHitParticle(
                        particleLocation,
                        particleVelocity,
                        PARTICLE_SIZE,
                        1.0f, // Brightness
                        PARTICLE_LIFETIME,
                        PARTICLE_COLOR
                );
            }
        } else {
            fireInterval.advance(amount);
            if (fireInterval.intervalElapsed()) {
                // Fire the beam
                engine.spawnEmpArcPierceShields(
                        weapon.getShip(), weapon.getLocation(), weapon.getShip(), weapon.getShip().getShipTarget(),
                        DamageType.ENERGY, // Damage type
                        DAMAGE_PER_SECOND * fireInterval.getIntervalDuration(), // Damage amount
                        0f, // EMP damage
                        RANGE, // Max range
                        "tachyon_lance_emp_impact", // Impact sound
                        BEAM_THICKNESS, // Thickness
                        BEAM_COLOR, // Fringe color
                        BEAM_COLOR // Core color
                );

                // Create particle effects
                for (int i = 0; i < PARTICLE_COUNT; i++) {
                    Vector2f particleLocation = new Vector2f(weapon.getLocation());
                    Vector2f particleVelocity = new Vector2f((float) Math.random() * PARTICLE_SPEED - PARTICLE_SPEED / 2, (float) Math.random() * PARTICLE_SPEED - PARTICLE_SPEED / 2);
                    engine.addHitParticle(
                            particleLocation,
                            particleVelocity,
                            PARTICLE_SIZE,
                            1.0f, // Brightness
                            PARTICLE_LIFETIME,
                            PARTICLE_COLOR
                    );
                }
            }
        }
    }
}
