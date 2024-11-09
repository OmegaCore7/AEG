package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

public class AEG_EraserCannon implements OnHitEffectPlugin {
    private static final float DAMAGE_MULT = 2.0f; // Double the damage
    private static final float EXPLOSION_RADIUS = 400f; // Radius of the explosion
    private static final float KNOCKBACK_FORCE = 100f; // Force of the knockback
    private static final float DEBUFF_DURATION = 5f; // Duration of the debuff in seconds
    private static final Color[] EXPLOSION_COLORS = {
            new Color(0, 255, 0, 255), // Bright green
            new Color(252, 194, 82, 255), // Orange
            new Color(29, 172, 29, 255) // Lime green
    };

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (proj == null || target == null || point == null || engine == null) return;

        // Create explosion effects with different colors
        for (Color color : EXPLOSION_COLORS) {
            engine.spawnExplosion(point, new Vector2f(0, 0), color, EXPLOSION_RADIUS, 1f);
        }

        // Apply damage to nearby entities
        for (CombatEntityAPI entity : CombatUtils.getEntitiesWithinRange(point, EXPLOSION_RADIUS)) {
            if (entity != target) {
                engine.applyDamage(entity, point, proj.getDamageAmount() * DAMAGE_MULT, proj.getDamageType(), 0f, false, false, proj.getSource());

                // Apply knockback
                Vector2f knockback = Vector2f.sub(entity.getLocation(), point, null);
                if (knockback.length() > 0) {
                    knockback.normalise();
                    knockback.scale(KNOCKBACK_FORCE);
                    entity.getVelocity().translate(knockback.x, knockback.y);
                }

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
    }
}
