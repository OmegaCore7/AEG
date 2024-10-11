package data.weapons.onfire;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

public class AEG_PhotonBarrageBeamStats implements OnFireEffectPlugin {

    private static final float INITIAL_DAMAGE = 2000f;
    private static final float SPLIT_BEAM_DAMAGE = 500f;
    private static final float RANGE = 1000f;
    private static final float BEAM_THICKNESS = 30f;
    private static final Color BEAM_COLOR = new Color(255, 255, 0, 255); // Bright yellow color
    private static final Color PARTICLE_COLOR = new Color(255, 140, 0, 200); // Flame color
    private static final int PARTICLE_COUNT = 50;
    private static final float PARTICLE_SIZE = 10f;
    private static final float PARTICLE_LIFETIME = 1.0f;
    private static final float PARTICLE_SPEED = 100f;
    private static final float CHARGE_UP_TIME = 3.0f; // Charge-up time in seconds

    private final IntervalUtil fireInterval = new IntervalUtil(0.1f, 0.1f);
    private float chargeUpProgress = 0f;
    private final Random random = new Random();

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
                // Fire the initial beam
                engine.spawnEmpArcPierceShields(
                        weapon.getShip(), weapon.getLocation(), weapon.getShip(), weapon.getShip().getShipTarget(),
                        DamageType.ENERGY, // Damage type
                        INITIAL_DAMAGE * fireInterval.getIntervalDuration(), // Damage amount
                        0f, // EMP damage
                        RANGE, // Max range
                        "tachyon_lance_emp_impact", // Impact sound
                        BEAM_THICKNESS, // Thickness
                        BEAM_COLOR, // Fringe color
                        BEAM_COLOR // Core color
                );

                // Create particle effects for the initial beam
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

                // Split into smaller beams with random angles
                for (int i = 0; i < 5; i++) {
                    float angle = random.nextFloat() * 360f; // Random angle in degrees
                    Vector2f splitBeamDirection = new Vector2f((float) Math.cos(Math.toRadians(angle)), (float) Math.sin(Math.toRadians(angle)));
                    splitBeamDirection.scale(RANGE);

                    Vector2f splitBeamLocation = new Vector2f(weapon.getLocation());
                    Vector2f.add(splitBeamLocation, splitBeamDirection, splitBeamLocation);

                    engine.spawnEmpArcPierceShields(
                            weapon.getShip(), splitBeamLocation, weapon.getShip(), weapon.getShip().getShipTarget(),
                            DamageType.ENERGY, // Damage type
                            SPLIT_BEAM_DAMAGE, // Damage amount
                            0f, // EMP damage
                            RANGE, // Max range
                            "tachyon_lance_emp_impact", // Impact sound
                            BEAM_THICKNESS / 2, // Thickness
                            BEAM_COLOR, // Fringe color
                            BEAM_COLOR // Core color
                    );
                }
            }
        }
    }
}
