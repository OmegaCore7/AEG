package data.weapons.scripts;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import java.util.Random;

public class AEG_4G_SpectacularBeamEffect implements BeamEffectPlugin {

    private static final float EMP_DAMAGE = 100f;
    private static final float STRIKE_DAMAGE = 50f;
    private static final float KINETIC_DAMAGE = 100f;
    private static final float HIGH_EXPLOSIVE_DAMAGE = 50f;
    private static final float ARMOR_DAMAGE_MULTIPLIER = 2f;
    private static final float LIGHTNING_INTERVAL = 1f;
    private static final float SMOKE_START_SIZE = 5f;
    private static final float SMOKE_END_SIZE = 10f;
    private static final float SMOKE_DURATION = 1f;

    private float lightningTimer = 0f;
    private Random random = new Random();

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine.isPaused()) return;

        CombatEntityAPI target = beam.getDamageTarget();
        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            Vector2f hitPoint = beam.getTo();

            if (ship.getShield() != null && ship.getShield().isWithinArc(hitPoint)) {
                // Hitting shield
                beam.getDamage().setDamage(beam.getDamage().getDamage() + KINETIC_DAMAGE);
                lightningTimer += amount;
                if (lightningTimer >= LIGHTNING_INTERVAL) {
                    lightningTimer = 0f;
                    spawnEmpLightning(engine, hitPoint, ship);
                }
            } else {
                // Hitting hull
                beam.getDamage().setDamage(beam.getDamage().getDamage() + HIGH_EXPLOSIVE_DAMAGE * ARMOR_DAMAGE_MULTIPLIER);
                spawnEnergySplash(engine, hitPoint);
                spawnSmoke(engine, hitPoint);
                ship.getMutableStats().getCriticalMalfunctionChance().modifyFlat("SpectacularBeamEffect", 0.1f);
            }
        }
    }

    private void spawnEmpLightning(CombatEngineAPI engine, Vector2f hitPoint, ShipAPI ship) {
        for (int i = 0; i < 3; i++) {
            Vector2f targetPoint = new Vector2f(
                    ship.getLocation().x + random.nextFloat() * ship.getCollisionRadius() * 2 - ship.getCollisionRadius(),
                    ship.getLocation().y + random.nextFloat() * ship.getCollisionRadius() * 2 - ship.getCollisionRadius()
            );
            engine.spawnEmpArcVisual(hitPoint, null, targetPoint, ship, 10f, Color.CYAN, Color.WHITE);
            engine.applyDamage(ship, targetPoint, STRIKE_DAMAGE, DamageType.ENERGY, EMP_DAMAGE, false, false, null, true);
        }
    }

    private void spawnEnergySplash(CombatEngineAPI engine, Vector2f hitPoint) {
        for (int i = 0; i < 5; i++) {
            float angle = random.nextFloat() * 360f;
            float speed = 50f + random.nextFloat() * 50f;
            Vector2f velocity = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(speed);
            engine.addHitParticle(hitPoint, velocity, 10f, 1f, 0.5f, Color.YELLOW);
        }
    }

    private void spawnSmoke(CombatEngineAPI engine, Vector2f hitPoint) {
        for (int i = 0; i < 5; i++) {
            float angle = random.nextFloat() * 360f;
            float speed = 10f + random.nextFloat() * 10f;
            Vector2f velocity = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(speed);
            engine.addSmokeParticle(hitPoint, velocity, SMOKE_START_SIZE, 1f, SMOKE_DURATION, Color.GRAY);
        }
    }
}