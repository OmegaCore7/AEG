package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

public class AEG_SpiralMunitions implements OnHitEffectPlugin {
    private static final float SLOW_DURATION = 10f; // Duration of the slow effect in seconds
    private static final float RANGE_REDUCTION_DURATION = 10f; // Duration of the range reduction in seconds
    private static final float TARGET_RANGE = 100f; // Desired weapon range
    private static final float PARTICLE_SIZE = 20f;
    private static final float PARTICLE_DURATION = 1f;
    private static final float PARTICLE_BRIGHTNESS = 1f;
    private static final float EFFECT_RADIUS = 75f;
    private static final float ATTRACTION_RADIUS = 800f; // Radius within which projectiles are attracted
    private static final float ATTRACTION_FORCE = 2000f; // Force of attraction

    private static final Color[] PARTICLE_COLORS = {
            new Color(0, 255, 0, 255), // Bright green
            new Color(0, 128, 0, 255), // Darker green
            new Color(0, 255, 255, 255) // Teal
    };

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (proj == null || target == null || point == null || engine == null) {
            Global.getLogger(AEG_SpiralMunitions.class).warn("Null reference detected in onHit parameters");
            return;
        }

        // Apply slow effect
        if (target instanceof ShipAPI) {
            final ShipAPI ship = (ShipAPI) target;
            ship.getMutableStats().getMaxSpeed().modifyMult("AEG_SpiralMunitions_slow", 0.5f);

            // Calculate the reduction needed to set the range to the target value
            float currentRange = ship.getMutableStats().getWeaponRangeThreshold().getModifiedValue();
            float reductionAmount = currentRange - TARGET_RANGE;
            ship.getMutableStats().getWeaponRangeThreshold().modifyFlat("AEG_SpiralMunitions_range", -reductionAmount);

            // Schedule the removal of the effects after the duration
            Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                private float elapsed = 0f;

                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (Global.getCombatEngine().isPaused()) return;
                    elapsed += amount;
                    if (elapsed >= SLOW_DURATION) {
                        ship.getMutableStats().getMaxSpeed().unmodify("AEG_SpiralMunitions_slow");
                        ship.getMutableStats().getWeaponRangeThreshold().unmodify("AEG_SpiralMunitions_range");
                        Global.getCombatEngine().removePlugin(this);
                    }
                }
            });
        }

        // Create absorption particle effect
        for (int i = 0; i < 50; i++) {
            float angle = (float) Math.random() * 360f;
            float distance = (float) Math.random() * EFFECT_RADIUS;
            float x = (float) Math.cos(Math.toRadians(angle)) * distance;
            float y = (float) Math.sin(Math.toRadians(angle)) * distance;
            Vector2f particleLocation = new Vector2f(point.x + x, point.y + y);

            Color particleColor = PARTICLE_COLORS[(int) (Math.random() * PARTICLE_COLORS.length)];
            float transparency = 1.0f - (distance / EFFECT_RADIUS);

            engine.addHitParticle(
                    particleLocation,
                    new Vector2f(0, 0), // No velocity
                    PARTICLE_SIZE,
                    transparency * PARTICLE_BRIGHTNESS, // Brightness
                    PARTICLE_DURATION,
                    particleColor
            );
        }

        // Attract nearby projectiles to the target ship
        for (DamagingProjectileAPI otherProj : engine.getProjectiles()) {
            if (otherProj != proj && Vector2f.sub(point, otherProj.getLocation(), null).length() < ATTRACTION_RADIUS) {
                Vector2f attraction = Vector2f.sub(point, otherProj.getLocation(), null);
                if (attraction.length() > 0) {
                    attraction.normalise();
                    attraction.scale(ATTRACTION_FORCE * engine.getElapsedInLastFrame());
                    otherProj.getVelocity().translate(attraction.x, attraction.y);
                } else {
                    Global.getLogger(AEG_SpiralMunitions.class).warn("Zero length vector detected during attraction");
                }
            }
        }
    }
}
