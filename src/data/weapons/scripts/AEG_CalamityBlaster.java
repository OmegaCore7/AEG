package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import data.weapons.helper.AEG_TargetingQuadtreeHelper;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class AEG_CalamityBlaster implements BeamEffectPlugin {
    private static final float BASE_BEAM_WIDTH = 60f;
    private static final float MAX_BEAM_WIDTH = 450f;
    private static final float FLUX_INCREMENT = 0.01f;
    private static final float EMP_ARC_INTERVAL = 0.08f;
    private static final Color BALL_COLOR = new Color(105, 255, 105, 225);
    private static final Color EMP_CORE_COLOR = new Color(255, 0, 0, 255);
    private static final Color EMP_FRINGE_COLOR = new Color(255, 165, 0, 255);

    private float currentBeamWidth = BASE_BEAM_WIDTH;
    private float damageMultiplier = 1f;
    private float fluxMultiplier = 1f;
    private float empArcTimer = 0f;
    private float beamTime = 0f;
    private final AEG_TargetingQuadtreeHelper quadtreeHelper;
    private final Random random;

    public AEG_CalamityBlaster() {
        quadtreeHelper = new AEG_TargetingQuadtreeHelper(0, new Vector2f(1000, 1000));
        random = new Random();
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine.isPaused()) return;

        beamTime += amount;

        // Gradually increase beam width and double damage every 2 seconds
        if (beamTime <= 10.0f) {
            currentBeamWidth = BASE_BEAM_WIDTH + (MAX_BEAM_WIDTH - BASE_BEAM_WIDTH) * (beamTime / 10f);
            if ((int) beamTime % 2 == 0) {
                beam.getDamage().setDamage(beam.getDamage().getDamage() * 2);
            }
        }

        // Set beam width
        beam.setWidth(currentBeamWidth);

        // Create the energy ball at the base of the beam
        Vector2f beamStart = beam.getFrom();
        engine.addHitParticle(beamStart, new Vector2f(), currentBeamWidth + 4f, 1f, 0.1f, BALL_COLOR);

        // Generate EMP arcs traveling from the base to the end of the beam every 0.08 seconds
        empArcTimer += amount;
        if (empArcTimer >= EMP_ARC_INTERVAL) {
            empArcTimer = 0f;
            emitEmpArc(engine, beamStart, beam);
        }

        // Apply damage and flux cost
        beam.getDamage().setMultiplier(damageMultiplier);
        beam.getSource().getFluxTracker().increaseFlux(beam.getWeapon().getFluxCostToFire() * fluxMultiplier, true);

        // Deal damage to targets
        List<CombatEntityAPI> targets = getTargets(engine, beam);
        for (CombatEntityAPI target : targets) {
            applyDamage(engine, beam, target);
        }

        // Spawn particles after 5 seconds at max width
        if (beamTime >= 15f) {
            spawnParticles(engine, beam);
        }

        // Handle beam effects after 5 seconds at max width
        if (beamTime >= 15f && beamTime <= 20f) {
            currentBeamWidth = MAX_BEAM_WIDTH + (450f - MAX_BEAM_WIDTH) * ((beamTime - 15f) / 5f);
            beam.setWidth(currentBeamWidth);
        } else if (beamTime > 20f) {
            currentBeamWidth = 450f;
            beam.setWidth(currentBeamWidth);
        }

        // Trigger explosions regardless of hitting shields or ships
        if (beamTime > 20f) {
            triggerExplosions(engine, beam);
        }
    }

    private void emitEmpArc(CombatEngineAPI engine, Vector2f beamStart, BeamAPI beam) {
        Vector2f beamEnd = beam.getTo();
        int numArcs = random.nextInt(4) + 1;
        for (int i = 0; i < numArcs; i++) {
            float thickness = 5f + random.nextFloat() * 20f;
            engine.spawnEmpArcVisual(beamStart, null, beamEnd, null, thickness, EMP_CORE_COLOR, EMP_FRINGE_COLOR);
        }
    }

    private void applyDamage(CombatEngineAPI engine, BeamAPI beam, CombatEntityAPI target) {
        float baseDamage = beam.getDamage().getBaseDamage();
        if (target.getShield() != null && target.getShield().isWithinArc(beam.getTo())) {
            engine.applyDamage(target, target.getLocation(), baseDamage * 0.5f, DamageType.KINETIC, 0, false, false, beam.getSource());
        } else {
            engine.applyDamage(target, target.getLocation(), baseDamage * 0.5f, DamageType.HIGH_EXPLOSIVE, 0, false, false, beam.getSource());
            engine.applyDamage(target, target.getLocation(), baseDamage * 0.5f, DamageType.FRAGMENTATION, 0, false, false, beam.getSource());
        }
    }

    private void spawnParticles(CombatEngineAPI engine, BeamAPI beam) {
        Vector2f beamBase = beam.getFrom();
        for (int i = 0; i < 100; i++) {
            Vector2f particleLocation = MathUtils.getRandomPointInCircle(beamBase, 500f);
            float transparency = 1f - (MathUtils.getDistance(beamBase, particleLocation) / 500f);
            engine.addHitParticle(particleLocation, new Vector2f(), 10f, transparency, 1f, Color.GREEN);
        }
    }

    private List<CombatEntityAPI> getTargets(CombatEngineAPI engine, BeamAPI beam) {
        return CombatUtils.getEntitiesWithinRange(beam.getTo(), beam.getWidth());
    }

    private void dealFatalDamage(CombatEngineAPI engine, BeamAPI beam) {
        Vector2f beamStart = beam.getFrom();
        Vector2f beamEnd = beam.getTo();
        List<CombatEntityAPI> targets = quadtreeHelper.retrieve(new ArrayList<CombatEntityAPI>(), beam.getSource());

        for (CombatEntityAPI target : targets) {
            Vector2f collisionPoint = CollisionUtils.getCollisionPoint(beamStart, beamEnd, target);
            if (collisionPoint != null) {
                applyFatalDamage(engine, beam.getSource(), target);
            }
        }
    }

    private void applyFatalDamage(CombatEngineAPI engine, ShipAPI source, CombatEntityAPI target) {
        engine.applyDamage(target, target.getLocation(), target.getHitpoints(), DamageType.ENERGY, 0, false, false, source);
    }

    private void triggerExplosions(final CombatEngineAPI engine, final BeamAPI beam) {
        Vector2f beamBase = beam.getFrom();
        Vector2f targetLocation = beam.getTo();
        Vector2f endPoint = MathUtils.getPointOnCircumference(targetLocation, 100f, VectorUtils.getAngle(beamBase, targetLocation));

        for (int i = 0; i < 50; i++) {
            Vector2f explosionPoint = MathUtils.getRandomPointOnLine(beamBase, endPoint);
            float size = 10f + random.nextFloat() * 290f;
            engine.spawnExplosion(explosionPoint, new Vector2f(), new Color(105, 255, 105, 255), size, 1f);

            // Retrieve entities within the explosion range
            List<CombatEntityAPI> entities = CombatUtils.getEntitiesWithinRange(explosionPoint, size);
            for (CombatEntityAPI entity : entities) {
                engine.applyDamage(entity, explosionPoint, size, DamageType.HIGH_EXPLOSIVE, 0, false, false, beam.getSource());
                engine.applyDamage(entity, explosionPoint, size, DamageType.KINETIC, 0, false, false, beam.getSource());
                engine.applyDamage(entity, explosionPoint, size, DamageType.FRAGMENTATION, 0, false, false, beam.getSource());
            }
        }

        disableWeapon(engine, beam);
    }

    private void disableWeapon(final CombatEngineAPI engine, final BeamAPI beam) {
        beam.getWeapon().disable(true);
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            private float disableTimer = 0f;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (engine.isPaused()) return;

                disableTimer += amount;
                if (disableTimer >= 30f) {
                    beam.getWeapon().disable(false);
                    engine.removePlugin(this);
                }
            }
        });
    }
}
