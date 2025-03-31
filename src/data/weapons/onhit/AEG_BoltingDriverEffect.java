package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.awt.Color;
import java.util.List;

public class AEG_BoltingDriverEffect implements OnHitEffectPlugin {

    private static boolean effectActive = false;

    @Override
    public void onHit(DamagingProjectileAPI projectile, final CombatEntityAPI target, final Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, final CombatEngineAPI engine) {
        // Create hit particles
        for (int i = 0; i < 6 + (int)(Math.random() * 7); i++) {
            float size = 20f + (float)(Math.random() * 30f);
            float duration = 0.5f + (float)(Math.random() * 0.5f);
            engine.addHitParticle(point, new Vector2f(), size, 1, duration, Misc.setAlpha(Misc.getHighlightColor(), 255));
        }

        // Create EMP lightning
        for (int i = 0; i < 2 + (int)(Math.random() * 3); i++) {
            engine.spawnEmpArc(projectile.getSource(), point, target, target,
                    DamageType.ENERGY, projectile.getDamageAmount() * 0.5f, projectile.getEmpAmount() * 0.5f, 10000f, null,
                    10f, target.getShield() != null ? target.getShield().getRingColor() : Misc.getHighlightColor(), target.getShield() != null ? target.getShield().getRingColor() : Misc.getHighlightColor());
        }

        // Apply 10 second timer effect if not already active
        if (!effectActive) {
            effectActive = true;
            engine.addPlugin(new BaseEveryFrameCombatPlugin() {
                private float timer = 10f;

                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (timer <= 0) {
                        engine.removePlugin(this);
                        effectActive = false;
                        return;
                    }

                    timer -= amount;

                    // Create expanding ring and nebula effects along the circumference
                    float radius = 700f * (1 - timer / 10f);
                    for (int i = 0; i < 5; i++) {
                        float angle = (float) (Math.random() * 2 * Math.PI);
                        Vector2f nebulaPoint = new Vector2f(
                                point.x + radius * (float) Math.cos(angle),
                                point.y + radius * (float) Math.sin(angle)
                        );
                        float nebulaSize = 50f + (float)(Math.random() * 100f);
                        engine.addNebulaParticle(nebulaPoint, new Vector2f(), nebulaSize, 1, 0.5f, 0.5f, 1f, Misc.getHighlightColor());
                    }

                    // Apply pull and push effects
                    for (CombatEntityAPI entity : CombatUtils.getEntitiesWithinRange(point, 700f)) {
                        if (entity instanceof ShipAPI) {
                            Vector2f direction = Vector2f.sub(entity.getLocation(), point, null);
                            float distance = direction.length();
                            direction.normalise();

                            if (entity == target) {
                                // Pull the hit ship
                                direction.scale(-150f);
                            } else if (entity.getOwner() != engine.getPlayerShip().getOwner()) {
                                // Push other ships
                                direction.scale(150f);
                            }
                            Vector2f.add(entity.getVelocity(), direction, entity.getVelocity());
                        }
                    }
                }
            });
        }
    }
}