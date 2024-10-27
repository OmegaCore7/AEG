package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AEG_SpiralLanceEffect implements BeamEffectPlugin {

    private final float BASE_WIDTH = 60f; // Updated base width
    private final IntervalUtil arcInterval = new IntervalUtil(0.8f, 0.8f);
    private float beamTime = 0f;
    private boolean explosionScheduled = false;
    private float explosionDelay = 1.5f;
    private Set<CombatEntityAPI> explodedTargets = new HashSet<>();

    @Override
    public void advance(float amount, final CombatEngineAPI engine, final BeamAPI beam) {
        // Don't bother with any checks if the game is paused
        if (engine.isPaused()) {
            return;
        }

        beamTime += amount;

        Vector2f start = beam.getFrom();
        final Vector2f end = beam.getTo();

        // Adjust beam width based on the time since the beam fired
        if (beamTime <= 0.8f) {
            if (beamTime <= 0.24f) {
                beam.setWidth(60f - 30f * (beamTime / 0.24f)); // Shrink to 30f in 0.24 seconds
            } else if (beamTime <= 0.56f) {
                beam.setWidth(30f + 170f * ((beamTime - 0.24f) / 0.32f)); // Expand to 200f in 0.32 seconds
            } else {
                beam.setWidth(200f - 140f * ((beamTime - 0.56f) / 0.24f)); // Shrink back to 60f in 0.24 seconds
            }
        } else {
            beam.setWidth(BASE_WIDTH);
        }

        // Generate EMP arcs traveling from the base to the end of the beam every 0.8 seconds
        arcInterval.advance(amount);
        if (arcInterval.intervalElapsed()) {
            float distance = getDistance(start, end);
            for (float i = 0; i < distance; i += 10 + Math.random() * 5) {
                Vector2f point = new Vector2f(start.x + (end.x - start.x) * (i / distance), start.y + (end.y - start.y) * (i / distance));
                engine.spawnEmpArcVisual(point, null, new Vector2f(point.x + (float) (Math.random() * 10 - 5), point.y + (float) (Math.random() * 10 - 5)), null, 10f, new Color(105, 255, 255, 255), new Color(105, 255, 105, 255));
            }
        }

        // Schedule an explosion with a delay of 1.5 seconds when the beam hits something, ensuring only one explosion per target
        if (beam.getDamageTarget() != null && !explodedTargets.contains(beam.getDamageTarget())) {
            Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                private float timer = explosionDelay;

                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (engine.isPaused()) {
                        return;
                    }

                    timer -= amount;
                    if (timer <= 0f) {
                        engine.spawnExplosion(end, new Vector2f(), new Color(105, 255, 105, 255), 600f, 1f);
                        engine.applyDamage(beam.getDamageTarget(), end, 1000f, DamageType.KINETIC, 500f, false, true, beam.getSource());
                        engine.applyDamage(beam.getDamageTarget(), end, 1000f, DamageType.HIGH_EXPLOSIVE, 500f, false, true, beam.getSource());
                        explodedTargets.add(beam.getDamageTarget());
                        engine.removePlugin(this);
                    }
                }
            });
            explosionScheduled = true;
        }
    }

    private float getDistance(Vector2f point1, Vector2f point2) {
        return (float) Math.hypot(point1.x - point2.x, point1.y - point2.y);
    }
}
