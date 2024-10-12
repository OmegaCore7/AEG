package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

public class AEG_SpiralMunitions implements OnHitEffectPlugin {
    private static final float SLOW_DURATION = 5f; // Duration of the slow effect in seconds
    private static final float RANGE_REDUCTION_DURATION = 5f; // Duration of the range reduction in seconds
    private static final float RANGE_REDUCTION = 200f; // Reduced weapon range
    private static final float PARTICLE_SIZE = 20f;
    private static final float PARTICLE_DURATION = 1f;
    private static final float PARTICLE_BRIGHTNESS = 1f;
    private static final float EFFECT_RADIUS = 75f;

    private static final Color[] PARTICLE_COLORS = {
            new Color(0, 255, 0, 255), // Bright green
            new Color(0, 128, 0, 255), // Darker green
            new Color(0, 255, 255, 255) // Teal
    };

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (proj == null || target == null || point == null || engine == null) return;

        // Apply slow effect
        if (target instanceof ShipAPI) {
            final ShipAPI ship = (ShipAPI) target;
            ship.getMutableStats().getMaxSpeed().modifyMult("AEG_SpiralMunitions_slow", 0.5f);
            ship.getMutableStats().getWeaponRangeThreshold().modifyFlat("AEG_SpiralMunitions_range", -RANGE_REDUCTION);

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
    }
}

