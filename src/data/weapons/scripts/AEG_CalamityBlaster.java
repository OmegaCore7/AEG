package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import data.weapons.helper.AEG_TargetingQuadtreeHelper;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AEG_CalamityBlaster implements BeamEffectPlugin {
    private static final float MAX_BEAM_WIDTH = 200f;
    private static final float MIN_BEAM_WIDTH = 50f;
    private static final float DAMAGE_INCREMENT = 0.01f;
    private static final float FLUX_INCREMENT = 0.01f;
    private static final float EMP_ARC_INTERVAL = 1f;
    private static final Color BALL_COLOR = new Color(105, 255, 105, 225); // Green color with some transparency
    private static final Color EMP_CORE_COLOR = new Color(255, 255, 255, 255); // White core
    private static final Color EMP_FRINGE_COLOR = new Color(105, 255, 105, 225); // Green fringe

    private float currentBeamWidth = MIN_BEAM_WIDTH;
    private float damageMultiplier = 1f;
    private float fluxMultiplier = 1f;
    private float empArcTimer = 0f;
    private AEG_TargetingQuadtreeHelper quadtreeHelper;
    private Random random;

    public AEG_CalamityBlaster() {
        quadtreeHelper = new AEG_TargetingQuadtreeHelper(0, new Vector2f(1000, 1000)); // Adjust bounds as needed
        random = new Random();
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine.isPaused()) return;

        // Increase beam width and damage/flux multipliers
        currentBeamWidth = Math.min(MAX_BEAM_WIDTH, currentBeamWidth + amount * (MAX_BEAM_WIDTH - MIN_BEAM_WIDTH) / 150f);
        damageMultiplier = Math.min(2f, damageMultiplier + DAMAGE_INCREMENT * amount);
        fluxMultiplier = Math.min(2f, fluxMultiplier + FLUX_INCREMENT * amount);

        // Set beam width
        beam.setWidth(currentBeamWidth);

        // Create the energy ball at the base of the beam
        Vector2f beamStart = beam.getFrom();
        engine.addHitParticle(beamStart, new Vector2f(), currentBeamWidth + 4f, 1f, 0.1f, BALL_COLOR);

        // Emit EMP arcs if beam width is 100f or larger
        if (currentBeamWidth >= 100f) {
            empArcTimer += amount;
            if (empArcTimer >= EMP_ARC_INTERVAL) {
                empArcTimer = 0f;
                emitEmpArc(engine, beamStart, beam);
            }
        }

        // Apply damage and flux cost
        beam.getDamage().setMultiplier(damageMultiplier);
        beam.getSource().getFluxTracker().increaseFlux(beam.getWeapon().getFluxCostToFire() * fluxMultiplier, true);
    }

    private void emitEmpArc(CombatEngineAPI engine, Vector2f beamStart, BeamAPI beam) {
        List<CombatEntityAPI> targets = quadtreeHelper.retrieve(new ArrayList<CombatEntityAPI>(), beam.getSource());
        for (CombatEntityAPI target : targets) {
            if (Math.random() < 0.5) { // Adjust probability as needed
                float distance = Vector2f.sub(target.getLocation(), beamStart, null).length();
                float width = Math.max(5f, distance / 20f); // Thicker for longer distances
                int alpha = 128 + random.nextInt(128); // Random transparency between 128 and 255

                Color coreColor = new Color(EMP_CORE_COLOR.getRed(), EMP_CORE_COLOR.getGreen(), EMP_CORE_COLOR.getBlue(), alpha);
                Color fringeColor = new Color(EMP_FRINGE_COLOR.getRed(), EMP_FRINGE_COLOR.getGreen(), EMP_FRINGE_COLOR.getBlue(), alpha);

                engine.spawnEmpArc(beam.getSource(), beamStart, beam.getSource(), target,
                        DamageType.ENERGY, 100f, 500f, 1000f, "tachyon_lance_emp_impact", width, coreColor, fringeColor);
            }
        }
    }
}
