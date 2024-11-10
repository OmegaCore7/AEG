package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import data.weapons.helper.AEG_TargetingQuadtreeHelper;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AEG_CalamityBlaster implements BeamEffectPlugin {
    private static final float MAX_BEAM_WIDTH = 350f;
    private static final float MIN_BEAM_WIDTH = 60f;
    private static final float FLUX_INCREMENT = 0.01f;
    private static final float EMP_ARC_INTERVAL = 0.8f;
    private static final float PULSE_INTERVAL = 2f;
    private static final float PULSE_DURATION = 0.4f;
    private static final float PULSE_EXTRA_WIDTH = 100f; // Increased pulse width
    private static final Color BALL_COLOR = new Color(105, 255, 105, 225); // Green color with some transparency
    private static final Color EMP_CORE_COLOR = new Color(255, 255, 255, 255); // White core
    private static final Color EMP_FRINGE_COLOR = new Color(105, 255, 105, 225); // Green fringe

    private float currentBeamWidth = MIN_BEAM_WIDTH;
    private float damageMultiplier = 1f;
    private float fluxMultiplier = 1f;
    private float empArcTimer = 0f;
    private float pulseTimer = 0f;
    private float beamTime = 0f; // Track the time the beam has been active
    private final AEG_TargetingQuadtreeHelper quadtreeHelper;
    private final Random random;

    public AEG_CalamityBlaster() {
        quadtreeHelper = new AEG_TargetingQuadtreeHelper(0, new Vector2f(1000, 1000)); // Adjust bounds as needed
        random = new Random();
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine.isPaused()) return;

        beamTime += amount;

        // Increase beam width over 3 seconds
        if (beamTime <= 3.0f) {
            currentBeamWidth = MIN_BEAM_WIDTH + (MAX_BEAM_WIDTH - MIN_BEAM_WIDTH) * (beamTime / 3.0f);
        } else {
            currentBeamWidth = MAX_BEAM_WIDTH;
        }

        // Handle pulsing effect
        if (beamTime > 3.0f) {
            pulseTimer += amount;
            if (pulseTimer >= PULSE_INTERVAL) {
                pulseTimer = 0f;
            }
            float pulseProgress = (pulseTimer % PULSE_INTERVAL) / PULSE_DURATION;
            if (pulseProgress <= 1.0f) {
                currentBeamWidth = MAX_BEAM_WIDTH + PULSE_EXTRA_WIDTH * (1.0f - Math.abs(pulseProgress - 0.5f) * 2);
            } else {
                currentBeamWidth = MAX_BEAM_WIDTH;
            }
        }

        // Set beam width
        beam.setWidth(currentBeamWidth);

        // Increase damage multiplier exponentially
        damageMultiplier = (float) Math.pow(2, beamTime);
        fluxMultiplier = Math.min(2f, fluxMultiplier + FLUX_INCREMENT * amount);

        // Create the energy ball at the base of the beam
        Vector2f beamStart = beam.getFrom();
        engine.addHitParticle(beamStart, new Vector2f(), currentBeamWidth + 4f, 1f, 0.1f, BALL_COLOR);

        // Generate EMP arcs traveling from the base to the end of the beam every 0.8 seconds
        empArcTimer += amount;
        if (empArcTimer >= EMP_ARC_INTERVAL) {
            empArcTimer = 0f;
            emitEmpArc(engine, beamStart, beam);
        }

        // Apply damage and flux cost
        beam.getDamage().setMultiplier(damageMultiplier);
        beam.getSource().getFluxTracker().increaseFlux(beam.getWeapon().getFluxCostToFire() * fluxMultiplier, true);

        // Deal fatal damage to anything the beam passes over
        dealFatalDamage(engine, beam);
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
        engine.applyDamage(target, target.getLocation(), target.getHitpoints(), DamageType.ENERGY, 100f, true, true, source);
    }

    private void emitEmpArc(CombatEngineAPI engine, Vector2f beamStart, BeamAPI beam) {
        Vector2f beamEnd = beam.getTo();
        float distance = MathUtils.getDistance(beamStart, beamEnd);
        for (float i = 0; i < distance; i += 350 + random.nextFloat() * 100) { // Random segments between 350 and 450
            Vector2f point = new Vector2f(beamStart.x + (beamEnd.x - beamStart.x) * (i / distance), beamStart.y + (beamEnd.y - beamStart.y) * (i / distance));
            engine.spawnEmpArcVisual(point, null, new Vector2f(point.x + (float) (Math.random() * 10 - 5), point.y + (float) (Math.random() * 10 - 5)), null, 10f, new Color(105, 255, 255, 255), new Color(105, 255, 105, 255));
        }
    }
}
