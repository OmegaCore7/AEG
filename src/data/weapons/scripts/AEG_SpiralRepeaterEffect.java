package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class AEG_SpiralRepeaterEffect implements BeamEffectPlugin {

    private boolean hasFired = false;
    private float random = 1f;
    private final float WIDTH = 25, PARTICLES = 5;
    private final IntervalUtil timer = new IntervalUtil(0.1f, 0.1f);

    private final String id = "pulse_laser_fire";
    private final List<Vector2f> PODS = new ArrayList<>();
    {
        PODS.add(new Vector2f(17.5f, 25.5f));
        PODS.add(new Vector2f(27.5f, 24.5f));
        PODS.add(new Vector2f(37.5f, 23.5f));
    }

    @Override
    public void advance(float amount, final CombatEngineAPI engine, BeamAPI beam) {
        // Don't bother with any checks if the game is paused
        if (engine.isPaused()) {
            return;
        }

        if (beam.getBrightness() == 1) {
            Vector2f start = beam.getFrom();
            Vector2f end = beam.getTo();

            // Extra damage against fighters and hull
            if (beam.didDamageThisFrame()) {
                float damage = beam.getDamage().computeDamageDealt(0.1f);
                if (beam.getDamageTarget().getCollisionClass() == CollisionClass.FIGHTER || beam.getDamageTarget().getCollisionClass() == CollisionClass.SHIP) {
                    engine.applyDamage(beam.getDamageTarget(), end, damage, DamageType.ENERGY, damage / 2, false, true, beam.getSource());
                }
            }

            // Visual fluff
            if (MathUtils.getDistanceSquared(start, end) == 0) {
                return;
            }

            timer.advance(amount);
            if (timer.intervalElapsed()) {
                hasFired = false;
                if (MagicRender.screenCheck(0.1f, start)) {
                    WeaponAPI weapon = beam.getWeapon();
                    ShipAPI ship = beam.getSource();

                    // Weapon fluff
                    Vector2f loc = new Vector2f(PODS.get(MathUtils.getRandomNumberInRange(0, 2)));
                    VectorUtils.rotate(loc, weapon.getCurrAngle());
                    Vector2f.add(loc, weapon.getLocation(), loc);
                    loc = MathUtils.getRandomPointInCircle(loc, 5);

                    Vector2f vel = MathUtils.getPoint(new Vector2f(ship.getVelocity()), MathUtils.getRandomNumberInRange(20, 50), weapon.getCurrAngle() + 45);

                    float size = MathUtils.getRandomNumberInRange(8, 16);
                    float glowth = MathUtils.getRandomNumberInRange(32, 64);

                    // Beam fluff
                    for (int i = 0; i < PARTICLES; i++) {
                        Vector2f point = MathUtils.getPointOnCircumference(start, (float) Math.random() * 300, weapon.getCurrAngle());
                        Vector2f.add(point, MathUtils.getRandomPointInCircle(new Vector2f(), WIDTH / 3), point);
                        vel = MathUtils.getPointOnCircumference(ship.getVelocity(), WIDTH / 2 + (float) Math.random() * 25, ship.getFacing());

                        engine.addHitParticle(
                                point,
                                vel,
                                3 + 7 * (float) Math.random(),
                                0.5f,
                                0.1f + 1f * (float) Math.random(),
                                new Color(0, 128, 0, 255) // Darker green
                        );
                    }
                    engine.addHitParticle(
                            start,
                            beam.getSource().getVelocity(),
                            50 + 50 * (float) Math.random(),
                            1,
                            0.1f + 0.2f * (float) Math.random(),
                            new Color(0, 255, 255, 255) // Teal
                    );
                    engine.addHitParticle(
                            start,
                            beam.getSource().getVelocity(),
                            40,
                            1,
                            0.05f,
                            new Color(255, 255, 255, 255)
                    );
                }
            }

            // Adjust beam width based on distance to target
            float distance = MathUtils.getDistance(start, end);
            float theWidth = WIDTH * (Math.min(1, (float) FastTrig.cos(18 * MathUtils.FPI * Math.min(timer.getElapsed(), 0.05f)) + 1));
            float adjustedWidth = theWidth * (1 + (1 - distance / 1000)); // Larger width closer to target
            beam.setWidth(random * adjustedWidth);

            if (!hasFired) {
                hasFired = true;
                // Play sound
                Global.getSoundPlayer().playSound(id, 0.75f + 0.5f * (float) Math.random(), 1.5f, start, beam.getSource().getVelocity());
            }
        } else {
            hasFired = false;
        }

        // Handle smoke trail based on ammo count
        final WeaponAPI weapon = beam.getWeapon();
        if (weapon.getAmmo() == 0) {
            // Create a green smoke trail at the base of the beam
            final Vector2f sparkPos = beam.getFrom(); // Base of the beam

            for (int i = 0; i < 5; i++) {
                Vector2f smokePos = MathUtils.getRandomPointInCircle(sparkPos, 10);
                engine.addSmokeParticle(
                        smokePos,
                        new Vector2f(),
                        MathUtils.getRandomNumberInRange(2, 5), // Size of the smoke
                        1.0f, // Brightness
                        1.5f, // Duration
                        new Color(105, 105, 105, 155) // Gray color
                );
            }
        }
    }
}
