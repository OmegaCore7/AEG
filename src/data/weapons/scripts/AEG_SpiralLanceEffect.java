package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

public class AEG_SpiralLanceEffect implements BeamEffectPlugin {

    private final float BASE_WIDTH = 60f; // Updated base width
    private final IntervalUtil arcInterval = new IntervalUtil(0.8f, 0.8f);

    @Override
    public void advance(float amount, final CombatEngineAPI engine, BeamAPI beam) {
        // Don't bother with any checks if the game is paused
        if (engine.isPaused()) {
            return;
        }

        Vector2f start = beam.getFrom();
        Vector2f end = beam.getTo();

        // Set beam width
        beam.setWidth(BASE_WIDTH);

        // Generate EMP arcs traveling from the base to the end of the beam every 0.8 seconds
        arcInterval.advance(amount);
        if (arcInterval.intervalElapsed()) {
            float distance = getDistance(start, end);
            for (float i = 0; i < distance; i += 10 + Math.random() * 5) {
                Vector2f point = new Vector2f(start.x + (end.x - start.x) * (i / distance), start.y + (end.y - start.y) * (i / distance));
                engine.spawnEmpArcVisual(point, null, new Vector2f(point.x + (float) (Math.random() * 10 - 5), point.y + (float) (Math.random() * 10 - 5)), null, 10f, new Color(105, 255, 255, 255), new Color(105, 255, 105, 255));
            }
        }
    }

    private float getDistance(Vector2f point1, Vector2f point2) {
        return (float) Math.hypot(point1.x - point2.x, point1.y - point2.y);
    }
}
