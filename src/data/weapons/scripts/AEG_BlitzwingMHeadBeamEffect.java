package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color; // Import the Color class
import org.lazywizard.lazylib.MathUtils; // Import MathUtils for random number generation
import org.lazywizard.lazylib.combat.CombatUtils; // Import CombatUtils for applying force

public class AEG_BlitzwingMHeadBeamEffect implements BeamEffectPlugin {
    private static final float FIGHTER_DAMAGE_MULTIPLIER = 3.0f; // 3x damage against fighters, strikecraft, and missiles
    private static final float PARTICLE_SIZE_MAX = 25f; // Maximum size of nebula particles
    private static final float PARTICLE_INERTIA_MULT = 0.5f; // Inertia multiplier for particle movement
    private static final float PARTICLE_DRIFT = 45f; // Drift value for particle movement

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine.isPaused()) {
            return;
        }

        ShipAPI ship = beam.getWeapon().getShip();
        ShipSystemAPI system = ship.getSystem();

        // Nebula particle effects
        Vector2f spawnPoint = MathUtils.getRandomPointOnLine(beam.getFrom(), beam.getTo());
        Vector2f velocity = new Vector2f(ship.getVelocity().x * PARTICLE_INERTIA_MULT, ship.getVelocity().y * PARTICLE_INERTIA_MULT);
        velocity = MathUtils.getRandomPointInCircle(velocity, PARTICLE_DRIFT);

        float particleSizeMultiplier = 1.0f;
        float particleBrightnessMultiplier = 1.0f;

        if ((float) Math.random() <= 0.05f) {
            engine.addNebulaParticle(spawnPoint,
                    velocity,
                    PARTICLE_SIZE_MAX * particleSizeMultiplier * (0.75f + (float) Math.random() * 0.5f),
                    MathUtils.getRandomNumberInRange(1.0f, 3f),
                    0f,
                    0f,
                    1f,
                    new Color(beam.getFringeColor().getRed(), beam.getFringeColor().getGreen(), beam.getFringeColor().getBlue(), 100),
                    true);
        }

        engine.addSmoothParticle(spawnPoint, velocity, MathUtils.getRandomNumberInRange(PARTICLE_SIZE_MAX * 0.75f, PARTICLE_SIZE_MAX * particleSizeMultiplier), particleBrightnessMultiplier,
                MathUtils.getRandomNumberInRange(0.4f, 0.9f), beam.getFringeColor());

        // Conditional logic for active ship system
        if (system.getState() == SystemState.IN) {
            // Apply damage increase against fighters and missiles
            CombatEntityAPI target = beam.getDamageTarget();
            if (target instanceof ShipAPI) {
                ShipAPI shipTarget = (ShipAPI) target;
                if (shipTarget.isFighter() || shipTarget.isDrone() || target instanceof MissileAPI) {
                    shipTarget.getMutableStats().getHullDamageTakenMult().modifyMult("BlitzwingMHeadBeamEffect", FIGHTER_DAMAGE_MULTIPLIER);
                    shipTarget.getMutableStats().getArmorDamageTakenMult().modifyMult("BlitzwingMHeadBeamEffect", FIGHTER_DAMAGE_MULTIPLIER);
                    shipTarget.getMutableStats().getShieldDamageTakenMult().modifyMult("BlitzwingMHeadBeamEffect", FIGHTER_DAMAGE_MULTIPLIER);
                }
            }

            // Enhance visual effects when the ship is active
            particleSizeMultiplier = 1.5f;
            particleBrightnessMultiplier = 1.5f;
        }

        // Effects when the beam hits a target
        CombatEntityAPI target = beam.getDamageTarget();
        if (target != null) {
            // Sparks effect
            for (int i = 0; i < 5; i++) {
                Vector2f sparkVelocity = MathUtils.getRandomPointInCircle(null, 100f);
                engine.addHitParticle(beam.getTo(), sparkVelocity, 2f, 1f, 0.5f, Color.YELLOW);
            }

            // Smoke effect
            for (int i = 0; i < 3; i++) {
                Vector2f smokeVelocity = MathUtils.getRandomPointInCircle(null, 20f);
                engine.addNebulaParticle(beam.getTo(), smokeVelocity, 2f, 1f, 0.1f, 0.2f, 1f, Color.GRAY);
                engine.addNebulaParticle(beam.getTo(), smokeVelocity, 5f, 1f, 0.2f, 0.5f, 1.5f, Color.DARK_GRAY);
            }
        }
    }
}