package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_SteelBarrageEffects {

    private static final Color LIGHT_GREEN_COLOR = new Color(144, 238, 144, 255);
    private static final Color EXPLOSION_COLOR = new Color(175, 220, 120, 255);

    public static void createHitspark(ShipAPI ship, ShipAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f arcLocation = target.getLocation();

        float maxRange = target.getCollisionRadius();
        float randomRange = (float) (Math.random() * maxRange);
        float randomThickness = (float) (Math.random() * 10 + 5);

        engine.spawnEmpArc(
                ship, ship.getLocation(), ship, target,
                DamageType.ENERGY,
                0f,
                0f,
                randomRange,
                "tachyon_lance_emp_impact",
                randomThickness,
                LIGHT_GREEN_COLOR,
                LIGHT_GREEN_COLOR
        );
    }

    public static void createExplosionOrShieldHit(ShipAPI ship, ShipAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (target.getShield() != null && target.getShield().isOn()) {
            Vector2f shieldHitLocation = target.getShield().getLocation();
            float shieldHitRadius = target.getShield().getRadius();
            engine.spawnExplosion(shieldHitLocation, target.getVelocity(), LIGHT_GREEN_COLOR, shieldHitRadius, 1f);
            engine.addHitParticle(shieldHitLocation, target.getVelocity(), shieldHitRadius * 1.5f, 1f, 0.25f, LIGHT_GREEN_COLOR);
            engine.addHitParticle(shieldHitLocation, target.getVelocity(), shieldHitRadius * 2f, 1f, 0.1f, Color.WHITE);
        } else {
            engine.spawnExplosion(target.getLocation(), target.getVelocity(), EXPLOSION_COLOR, 300f, 2f);
        }
    }

    public static void addJitterCopies(ShipAPI ship) {
        float jitterDuration = 5.0f;
        float jitterRange = 5.0f;

        for (int i = 0; i < 10; i++) {
            JitterEffectManager.addJitterCopy(ship, LIGHT_GREEN_COLOR, jitterDuration, jitterRange);
        }
    }
}
