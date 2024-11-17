package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class AEG_transformationFX {

    private static final Color PARTICLE_COLOR = new Color(217, 255, 161, 255);
    private static final float RADIUS = 150f;
    private static final int POINT_COUNT = 20;
    private static final float VARIANCE = 10f;

    private List<Vector2f> points;

    public AEG_transformationFX() {
        points = generatePoints();
    }

    private List<Vector2f> generatePoints() {
        List<Vector2f> points = new ArrayList<>();
        for (int i = 0; i < POINT_COUNT; i++) {
            float angle = (float) (i * (360.0 / POINT_COUNT));
            float radius = RADIUS + MathUtils.getRandomNumberInRange(-VARIANCE / 2, VARIANCE / 2);
            Vector2f point = MathUtils.getPointOnCircumference(null, radius, angle);
            points.add(point);
        }
        return points;
    }

    public void updateParticles(ShipAPI ship, float chargeLevel) {
        for (Vector2f point : points) {
            Vector2f location = Vector2f.add(ship.getLocation(), point, new Vector2f());
            Vector2f velocity = MathUtils.getPointOnCircumference(null, 50f * chargeLevel, MathUtils.getRandomNumberInRange(0f, 360f));
            float size = 10f * chargeLevel;
            float duration = 1f * chargeLevel;
            Global.getCombatEngine().addHitParticle(location, velocity, size, 1f, duration, PARTICLE_COLOR);
        }
    }

    public void createTransformationEffect(ShipAPI ship) {
        for (Vector2f point : points) {
            Vector2f location = Vector2f.add(ship.getLocation(), point, new Vector2f());
            Vector2f velocity = MathUtils.getPointOnCircumference(null, 100f, MathUtils.getRandomNumberInRange(0f, 360f));
            Global.getCombatEngine().spawnExplosion(location, velocity, PARTICLE_COLOR, 20f, 1.25f);
            Global.getCombatEngine().addNebulaSmoothParticle(location, velocity, 15f, 1f, 0.5f, 0.25f, 1f, PARTICLE_COLOR, true);
        }
    }

    public void fadeOutParticles(ShipAPI ship) {
        for (Vector2f point : points) {
            Vector2f location = Vector2f.add(ship.getLocation(), point, new Vector2f());
            Vector2f velocity = MathUtils.getPointOnCircumference(null, 50f, MathUtils.getRandomNumberInRange(0f, 360f));
            Global.getCombatEngine().addHitParticle(location, velocity, 10f, 1f, 1f, new Color(217, 255, 161, 100));
        }
    }
}
