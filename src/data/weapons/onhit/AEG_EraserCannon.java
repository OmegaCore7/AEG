package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Random;

public class AEG_EraserCannon implements OnHitEffectPlugin {
    private static final float DAMAGE_MULT = 3.0f; // Triple the damage
    private static final float EXPLOSION_RADIUS = 300f; // Radius of the explosion
    private static final float DEBUFF_DURATION = 5f; // Duration of the debuff in seconds
    private static final Color[] EXPLOSION_COLORS = {
            new Color(0, 255, 0, 255), // Bright green
            new Color(252, 194, 82, 255), // Orange
            new Color(29, 172, 29, 255) // Lime green
    };

    private final Random random = new Random();

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (proj == null || target == null || point == null || engine == null) return;

        // Create explosion effects with different colors
        for (Color color : EXPLOSION_COLORS) {
            engine.spawnExplosion(point, new Vector2f(0, 0), color, EXPLOSION_RADIUS, 1f);
        }

        // Calculate entry and exit points
        Vector2f exitPoint = calculateExitPoint(target, point);

        // Deal damage along the line
        dealLineDamage(target, point, exitPoint, engine, proj, shieldHit);

        // Visual effects
        createVisualEffects(engine, point, exitPoint, shieldHit);

        // Apply damage to nearby entities
        for (CombatEntityAPI entity : CombatUtils.getEntitiesWithinRange(point, EXPLOSION_RADIUS)) {
            if (entity != target) {
                engine.applyDamage(entity, point, proj.getDamageAmount() * DAMAGE_MULT, proj.getDamageType(), 0f, false, false, proj.getSource());

                // Apply debuff
                if (entity instanceof ShipAPI) {
                    final ShipAPI ship = (ShipAPI) entity;
                    ship.getMutableStats().getMaxSpeed().modifyMult("AEG_ErasorCannon_debuff", 0.5f);
                    ship.getMutableStats().getHullDamageTakenMult().modifyMult("AEG_ErasorCannon_debuff", 1.5f);

                    // Schedule the removal of the debuff after the duration
                    Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                        private float elapsed = 0f;

                        @Override
                        public void advance(float amount, List<InputEventAPI> events) {
                            if (Global.getCombatEngine().isPaused()) return;
                            elapsed += amount;
                            if (elapsed >= DEBUFF_DURATION) {
                                ship.getMutableStats().getMaxSpeed().unmodify("AEG_ErasorCannon_debuff");
                                ship.getMutableStats().getHullDamageTakenMult().unmodify("AEG_ErasorCannon_debuff");
                                Global.getCombatEngine().removePlugin(this);
                            }
                        }
                    });
                }
            }
        }

        // Remove the original projectile
        engine.removeEntity(proj);
    }

    private Vector2f calculateExitPoint(CombatEntityAPI target, Vector2f entryPoint) {
        // Calculate the exit point on the opposite side of the ship
        Vector2f direction = new Vector2f();
        Vector2f.sub(target.getLocation(), entryPoint, direction);
        direction.normalise();
        direction.scale(target.getCollisionRadius() * 2);
        Vector2f exitPoint = new Vector2f();
        Vector2f.add(entryPoint, direction, exitPoint);
        return exitPoint;
    }

    private void dealLineDamage(CombatEntityAPI target, Vector2f entryPoint, Vector2f exitPoint, CombatEngineAPI engine, DamagingProjectileAPI projectile, boolean shieldHit) {
        // Deal damage along the line between entry and exit points
        Vector2f direction = new Vector2f();
        Vector2f.sub(exitPoint, entryPoint, direction);
        direction.normalise();
        float damageAmount = projectile.getDamageAmount();
        if (shieldHit) {
            damageAmount *= 2;
        }
        for (float i = 0; i <= 1; i += 0.1f) {
            Vector2f point = new Vector2f(entryPoint);
            point.x += direction.x * i * target.getCollisionRadius();
            point.y += direction.y * i * target.getCollisionRadius();
            engine.applyDamage(target, point, damageAmount, DamageType.HIGH_EXPLOSIVE, 0, false, false, projectile.getSource());
        }
    }

    private void createVisualEffects(CombatEngineAPI engine, Vector2f entryPoint, Vector2f exitPoint, boolean shieldHit) {
        // Create visual effects along the line
        Vector2f direction = new Vector2f();
        Vector2f.sub(exitPoint, entryPoint, direction);
        direction.normalise();
        for (float i = 0; i <= 1; i += 0.1f) {
            Vector2f point = new Vector2f(entryPoint);
            point.x += direction.x * i * 100;
            point.y += direction.y * i * 100;
            float size = 50f + random.nextFloat() * 150f; // Random size between 50 and 200
            engine.spawnExplosion(point, new Vector2f(), Color.ORANGE, size, 1);
        }
        // Entry and exit explosions
        engine.spawnExplosion(entryPoint, new Vector2f(), Color.ORANGE, 300, 1);
        engine.spawnExplosion(exitPoint, new Vector2f(), Color.ORANGE, 300, 1);

        // EMP hit sparks if shield hit
        if (shieldHit) {
            engine.addHitParticle(entryPoint, new Vector2f(), 50, 1, 0.1f, Color.CYAN);
        }
    }
}
