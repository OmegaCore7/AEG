package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_ThunderBoltOnHitEffect implements OnHitEffectPlugin {

    private static final float PLASMA_BURST_DAMAGE = 100f;
    private static final float PLASMA_BURST_RADIUS = 50f;
    private static final float STATIC_FIELD_DURATION = 5f;
    private static final float STATIC_FIELD_DAMAGE_PER_SECOND = 10f;
    private static final float STATIC_FIELD_SLOW_EFFECT = 0.5f;
    private static final float EMP_ARC_DAMAGE = 10f;
    private static final float EMP_ARC_INTERVAL = 0.5f;
    private static final int EMP_ARC_COUNT = 12; // Number of EMP arcs around the boundary

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point,
                      boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        // Plasma Burst
        engine.spawnExplosion(point, new Vector2f(0, 0), new Color(240, 80, 0), PLASMA_BURST_RADIUS, 1f);
        engine.applyDamage(target, point, PLASMA_BURST_DAMAGE, DamageType.ENERGY, 0f, false, false, projectile.getSource());

        // Static Field
        IntervalUtil staticFieldInterval = new IntervalUtil(0.1f, 0.1f);
        IntervalUtil empArcInterval = new IntervalUtil(EMP_ARC_INTERVAL, EMP_ARC_INTERVAL);
        for (float t = 0; t < STATIC_FIELD_DURATION; t += 0.1f) {
            staticFieldInterval.advance(0.1f);
            empArcInterval.advance(0.1f);
            if (staticFieldInterval.intervalElapsed()) {
                engine.applyDamage(target, point, STATIC_FIELD_DAMAGE_PER_SECOND * 0.1f, DamageType.ENERGY, 0f, false, false, projectile.getSource());
                // Apply slow effect (this is a simplified example)
                if (target instanceof ShipAPI) {
                    ((ShipAPI) target).getMutableStats().getMaxSpeed().modifyMult("static_field_slow", STATIC_FIELD_SLOW_EFFECT);
                }
            }
            if (empArcInterval.intervalElapsed()) {
                // Create EMP arcs along the boundary
                for (int i = 0; i < EMP_ARC_COUNT; i++) {
                    double angle = 2 * Math.PI * i / EMP_ARC_COUNT;
                    Vector2f empPoint = new Vector2f(
                            point.x + (float) Math.cos(angle) * PLASMA_BURST_RADIUS,
                            point.y + (float) Math.sin(angle) * PLASMA_BURST_RADIUS
                    );
                    for (CombatEntityAPI nearbyEntity : CombatUtils.getEntitiesWithinRange(empPoint, PLASMA_BURST_RADIUS)) {
                        if (nearbyEntity != target && nearbyEntity instanceof ShipAPI) {
                            engine.spawnEmpArc(projectile.getSource(), empPoint, target, nearbyEntity,
                                    DamageType.ENERGY, EMP_ARC_DAMAGE, EMP_ARC_DAMAGE, 1000f, null, 5f, new Color(25, 100, 155, 255), new Color(255, 255, 255, 255));
                        }
                    }
                }
            }
        }
    }
}
