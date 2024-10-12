package data.shipsystems.helpers;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

public class AEG_EmpArcHelper {

    private final Random random = new Random();

    public void createEmpArcs(CombatEngineAPI engine, Vector2f center, float radius) {
        int arcCount = 20; // Number of arcs for better coverage
        for (int i = 0; i < arcCount; i++) {
            float angle = random.nextFloat() * 360f;
            float x1 = (float) Math.cos(Math.toRadians(angle)) * radius;
            float y1 = (float) Math.sin(Math.toRadians(angle)) * radius;
            Vector2f start = new Vector2f(center.x + x1, center.y + y1);

            float x2 = (float) Math.cos(Math.toRadians(angle)) * (radius + 50f);
            float y2 = (float) Math.sin(Math.toRadians(angle)) * (radius + 50f);
            Vector2f end = new Vector2f(center.x + x2, center.y + y2);

            engine.spawnEmpArcVisual(
                    start,
                    null,
                    end,
                    null,
                    10f, // Core width
                    new Color(255, 255, 255), // White core
                    new Color(255, 255, 0)    // Yellow fringe
            );
        }
    }
}
