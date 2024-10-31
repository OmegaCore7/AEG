package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import data.weapons.helper.AEG_TargetingQuadtreeHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class AEG_ErasorVolleyOnHit implements OnHitEffectPlugin {
    private static final float EXPLOSION_RADIUS = 300f;
    private static final float EMP_DAMAGE = 100f;
    private static final float EMP_ARC_RANGE = 300f;

    private AEG_TargetingQuadtreeHelper quadtree;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        // Create green explosion
        engine.spawnExplosion(point, new Vector2f(0, 0), Misc.setAlpha(new Color(0, 255, 0), 255), EXPLOSION_RADIUS, 1f);

        // Define EMP arc colors
        Color empCoreColor = new Color(255, 255, 255);
        Color empFringeColor = new Color(105, 255, 105);

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
        List<CombatEntityAPI> potentialTargets = new ArrayList<CombatEntityAPI>();
        quadtree.retrieve(potentialTargets, new SimpleEntity(point));

        // Create EMP arcs to up to two additional targets
        int arcsCreated = 0;
        for (CombatEntityAPI entity : potentialTargets) {
            if (entity != target && MathUtils.getDistance(point, entity.getLocation()) <= EMP_ARC_RANGE) {
                engine.spawnEmpArc(projectile.getSource(), point, target, entity, DamageType.ENERGY, EMP_DAMAGE, EMP_DAMAGE, EMP_ARC_RANGE, "tachyon_lance_emp_impact", 10f, empFringeColor, empCoreColor);
                arcsCreated++;
                if (arcsCreated >= 2) {
                    break;
                }
            }
        }
    }
}
