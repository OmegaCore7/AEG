package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_OmegaBlasterOnHit implements OnHitEffectPlugin {

    private static final Color EXPLOSION_COLOR = new Color(0, 255, 0); // Green color
    private static final Color CORE_COLOR = new Color(255, 255, 255); // White color
    private static final float EXPLOSION_RADIUS = 800f;
    private static final float EXPLOSION_DURATION = 2f;
    private static final float PUSH_FORCE = 500f;
    private static final int EMP_ARC_COUNT = 5;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        // Create the explosion effect at the point of impact
        engine.spawnExplosion(point, new Vector2f(0, 0), EXPLOSION_COLOR, EXPLOSION_RADIUS, EXPLOSION_DURATION);
        engine.spawnExplosion(point, new Vector2f(0, 0), CORE_COLOR, EXPLOSION_RADIUS / 2, EXPLOSION_DURATION / 2);

        // Create EMP arcs radiating outward
        for (int i = 0; i < EMP_ARC_COUNT; i++) {
            Vector2f arcPoint = MathUtils.getRandomPointInCircle(point, EXPLOSION_RADIUS);
            engine.spawnEmpArc(projectile.getSource(), point, null, target,
                    DamageType.ENERGY,  // Damage type
                    100,                  // Damage
                    1000,               // EMP damage
                    2000f,             // Max range
                    "tachyon_lance_emp_impact",  // Impact sound
                    15f,                // Thickness
                    CORE_COLOR,         // Fringe color
                    EXPLOSION_COLOR);   // Core color
        }

        // Apply push force to all entities within the explosion radius
        for (CombatEntityAPI entity : CombatUtils.getEntitiesWithinRange(point, EXPLOSION_RADIUS)) {
            Vector2f pushVector = Vector2f.sub(entity.getLocation(), point, null);
            pushVector.normalise();
            pushVector.scale(PUSH_FORCE);
            entity.getVelocity().set(Vector2f.add(entity.getVelocity(), pushVector, null));
        }
    }
}
