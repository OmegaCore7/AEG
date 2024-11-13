package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Random;
public class AEG_SB_Effect {

    private static final Random RANDOM = new Random();
    private static final IntervalUtil lightningInterval = new IntervalUtil(2.0f, 2.0f); // Interval set to 2 seconds

    public static void applyRammingEffects(ShipAPI ship, CombatEngineAPI engine) {
        // Ensure the shield is active
        if (!ship.getShield().isOn()) {
            ship.getShield().toggleOn();
        }

        // Improve shield efficiency and damage reduction
        ship.getMutableStats().getShieldDamageTakenMult().modifyMult("AEG_SB_Effect", 0.5f);
        ship.getMutableStats().getShieldUpkeepMult().modifyMult("AEG_SB_Effect", 0.5f);

        // Change shield color
        ship.getShield().setRingColor(new Color(105, 255, 105, 255));
    }

    public static void createCollisionEffects(ShipAPI ship, CombatEntityAPI target, CombatEngineAPI engine) {
        // Check if the interval has elapsed
        lightningInterval.advance(engine.getElapsedInLastFrame());
        if (lightningInterval.intervalElapsed()) {
            // Create 5 green EMP arcs with random widths and lengths
            for (int i = 0; i < 5; i++) {
                float width = 30f + RANDOM.nextFloat() * 30f;
                float length = 100f + RANDOM.nextFloat() * 200f;
                createLightningArc(ship, target, engine, new Color(105, 255, 105, 255), 100f, 200f, width, length);
            }

            // Create 2-3 blue EMP arcs if the target's shield is active
            if (target instanceof ShipAPI && ((ShipAPI) target).getShield() != null && ((ShipAPI) target).getShield().isOn()) {
                int blueStrikes = 2 + RANDOM.nextInt(2);
                for (int i = 0; i < blueStrikes; i++) {
                    float width = 30f + RANDOM.nextFloat() * 30f;
                    float length = 100f + RANDOM.nextFloat() * 200f;
                    createLightningArc(ship, target, engine, new Color(0, 0, 255, 255), 0f, 0f, width, length);
                }
            }
        }

        // Knock the target ship away
        Vector2f pushVector = VectorUtils.getDirectionalVector(ship.getLocation(), target.getLocation());
        pushVector.scale(1000f);
        Vector2f.add(target.getVelocity(), pushVector, target.getVelocity());
    }

    private static void createLightningArc(ShipAPI source, CombatEntityAPI target, CombatEngineAPI engine, Color color, float kineticDamage, float empDamage, float width, float length) {
        Vector2f sourcePoint = getRandomPointOnShield(source);
        Vector2f targetPoint = getRandomPointOnEntity(target);

        engine.spawnEmpArc(source, sourcePoint, source, target, DamageType.ENERGY, kineticDamage, empDamage, 1000f, null, width, color, new Color(105, 255, 105, 255));
    }

    private static Vector2f getRandomPointOnShield(ShipAPI ship) {
        float angle = RANDOM.nextFloat() * 180f - 90f; // Front quadrant
        float radius = ship.getShield().getRadius();
        Vector2f point = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(radius);
        Vector2f.add(point, ship.getShield().getLocation(), point);
        return point;
    }

    private static Vector2f getRandomPointOnEntity(CombatEntityAPI entity) {
        float x = entity.getLocation().x + (RANDOM.nextFloat() - 0.5f) * entity.getCollisionRadius();
        float y = entity.getLocation().y + (RANDOM.nextFloat() - 0.5f) * entity.getCollisionRadius();
        return new Vector2f(x, y);
    }
}