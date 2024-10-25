package data.shipsystems.helpers;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_GigaDrillEffects {

    private static final Color CORE_COLOR = new Color(255, 255, 255, 255); // White core
    private static final Color FRINGE_COLOR = new Color(105, 255, 105, 255); // Green fringe
    private static final float ARC_DURATION = 1f; // Duration of the arc

    public static void createEffects(CombatEngineAPI engine, ShipAPI ship, float amount) {
        Vector2f shipLocation = ship.getLocation();
        Vector2f startPoint = new Vector2f(shipLocation.x + 220, shipLocation.y);

        // Define the end points of the lines
        Vector2f endPoint1 = new Vector2f(shipLocation.x + 27, shipLocation.y + 70);
        Vector2f endPoint2 = new Vector2f(shipLocation.x + 27, shipLocation.y - 70);

        // Generate arcs along the lines
        generateArcs(engine, ship, startPoint, endPoint1);
        generateArcs(engine, ship, startPoint, endPoint2);
    }

    private static void generateArcs(CombatEngineAPI engine, ShipAPI ship, Vector2f startPoint, Vector2f endPoint) {
        float step = 0.1f; // Adjust the step size for more or fewer arcs
        for (float t = 0; t <= 1; t += step) {
            Vector2f intermediatePoint = interpolate(startPoint, endPoint, t);
            float angle = MathUtils.getRandomNumberInRange(-30, 30); // Vary the angle
            Vector2f arcEndPoint = MathUtils.getPointOnCircumference(intermediatePoint, MathUtils.getDistance(startPoint, intermediatePoint), angle);

            // Create the EMP arc
            engine.spawnEmpArcVisual(startPoint, ship, arcEndPoint, null, ARC_DURATION, CORE_COLOR, FRINGE_COLOR);
        }
    }

    private static Vector2f interpolate(Vector2f start, Vector2f end, float t) {
        float x = start.x + t * (end.x - start.x);
        float y = start.y + t * (end.y - start.y);
        return new Vector2f(x, y);
    }
}
