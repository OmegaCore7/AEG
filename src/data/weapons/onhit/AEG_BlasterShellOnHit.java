package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import data.weapons.helper.AEG_TargetingQuadtreeHelper;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AEG_BlasterShellOnHit implements OnHitEffectPlugin {

    private static final Color EXPLOSION_COLOR = new Color(0, 255, 0); // Green color
    private static final Color CORE_COLOR = new Color(158, 239, 158); // Light Green color
    private static final float EXPLOSION_RADIUS = 800f;
    private static final float EXPLOSION_DURATION = 2f;
    private static final float PUSH_FORCE = 500f;
    private static final int EMP_ARC_COUNT = 5;
    private static final float EMP_DAMAGE = 100f;
    private static final float EMP_ARC_RANGE = 300f;
    private static final float KNOCKBACK_FORCE = 500f;

    private AEG_TargetingQuadtreeHelper quadtree;

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
                    EMP_DAMAGE,         // Damage
                    EMP_DAMAGE,         // EMP damage
                    EMP_ARC_RANGE,      // Max range
                    "tachyon_lance_emp_impact",  // Impact sound
                    15f,                // Thickness
                    CORE_COLOR,         // Fringe color
                    EXPLOSION_COLOR);   // Core color
        }

        // Initialize quadtree if necessary
        if (quadtree == null) {
            quadtree = new AEG_TargetingQuadtreeHelper(0, new Vector2f(engine.getMapWidth(), engine.getMapHeight()));
        }

        // Clear and populate quadtree with potential targets
        quadtree.clear();
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != projectile.getSource().getOwner()) {
                quadtree.insert(ship);
            }
        }

        // Retrieve potential targets within EMP arc range
        List<CombatEntityAPI> potentialTargets = new ArrayList<>();
        quadtree.retrieve(potentialTargets, new SimpleEntity(point));

        // Create EMP arcs to up to two additional targets
        int arcsCreated = 0;
        for (CombatEntityAPI entity : potentialTargets) {
            if (entity != target && MathUtils.getDistance(point, entity.getLocation()) <= EMP_ARC_RANGE) {
                engine.spawnEmpArc(projectile.getSource(), point, target, entity, DamageType.ENERGY, EMP_DAMAGE, EMP_DAMAGE, EMP_ARC_RANGE, "tachyon_lance_emp_impact", 10f, CORE_COLOR, EXPLOSION_COLOR);
                arcsCreated++;
                if (arcsCreated >= 2) {
                    break;
                }
            }
        }

        // Apply push force to all entities within the explosion radius
        for (CombatEntityAPI entity : CombatUtils.getEntitiesWithinRange(point, EXPLOSION_RADIUS)) {
            Vector2f pushVector = Vector2f.sub(entity.getLocation(), point, null);
            pushVector.normalise();
            pushVector.scale(PUSH_FORCE);
            entity.getVelocity().set(Vector2f.add(entity.getVelocity(), pushVector, null));
        }

        // Apply knockback to the target
        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            Vector2f knockbackDir = Vector2f.sub(ship.getLocation(), point, new Vector2f());
            knockbackDir.normalise();
            knockbackDir.scale(KNOCKBACK_FORCE);
            ship.getVelocity().translate(knockbackDir.x, knockbackDir.y);

            // Play sound and visual effects for knockback
            Global.getSoundPlayer().playSound("shield_hit_heavy", 1.0f, 1.0f, ship.getLocation(), ship.getVelocity());
            engine.addHitParticle(ship.getLocation(), ship.getVelocity(), 100, 1, 0.5f, Color.WHITE);
        }

        // Apply additional shield recoil effect if shield was hit
        if (shieldHit && target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            // Additional visual effect for shield recoil
            engine.addHitParticle(ship.getLocation(), ship.getVelocity(), 150, 1, 0.5f, Color.CYAN);
        }
    }
}
