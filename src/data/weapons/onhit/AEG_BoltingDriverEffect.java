package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class AEG_BoltingDriverEffect implements OnHitEffectPlugin {
    private static final String DISTORTION_SPRITE = "graphics/fx/wormhole_ring_bright2.png";
    private static boolean effectActive = false;
    private static final Random random = new Random();

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

        // Apply 20 second timer effect if not already active
        if (!effectActive) {
            effectActive = true;
            engine.addPlugin(new BaseEveryFrameCombatPlugin() {
                private float timer = 20f;
                private float spawnTimer = 0f;

                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    timer -= amount;
                    spawnTimer -= amount;

                    if (timer <= 0f) {
                        engine.removePlugin(this);
                        effectActive = false;
                        return;
                    }

                    ShipAPI ship = null;
                    final Vector2f ringLocation = new Vector2f(point);

                    // === 1. Giant shimmer dome using a transparent sprite ===
                    MagicRender.battlespace(
                            Global.getSettings().getSprite(DISTORTION_SPRITE),
                            ringLocation,
                            new Vector2f(),
                            new Vector2f(700, 700),
                            new Vector2f(),
                            0f, 0f,
                            new Color(
                                    255 - random.nextInt(110),                     // red
                                    140 + random.nextInt(110), // yellow/orange
                                    20 + random.nextInt(120),  // just a hint of red/orange
                                    70 + random.nextInt(185)   // alpha
                            ),
                            true,
                            0f,
                            0.2f,
                            0.1f
                    );
                    // Apply pull and push effects
                    for (CombatEntityAPI entity : CombatUtils.getEntitiesWithinRange(point, 700f)) {
                        if (entity instanceof ShipAPI) {
                            ship = (ShipAPI) entity;
                            if (ship == engine.getPlayerShip()) {
                                continue; // Skip the player ship
                            }

                            Vector2f direction = Vector2f.sub(entity.getLocation(), point, null);
                            float distance = direction.length();
                            direction.normalise();

                            if (entity == target) {
                                // Pull the hit ship
                                direction.scale(-125f);
                            } else {
                                // Push other ships
                                direction.scale(125f);
                            }
                            Vector2f.add(entity.getVelocity(), direction, entity.getVelocity());
                        }
                    }
                }
            });
        }
    }
}