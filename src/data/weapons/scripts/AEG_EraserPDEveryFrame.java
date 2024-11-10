package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import data.weapons.helper.AEG_TargetingQuadtreeHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AEG_EraserPDEveryFrame implements EveryFrameWeaponEffectPlugin {
    private static final float ZAP_RANGE = 800f;
    private static final float ZAP_DAMAGE = 200f;
    private static final int MAX_CHARGES = 20;

    private int charges = MAX_CHARGES;
    private AEG_TargetingQuadtreeHelper quadtree;
    private final Random random = new Random();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || charges <= 0) {
            return;
        }

        List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
        DamagingProjectileAPI projectile = null;

        for (DamagingProjectileAPI proj : projectiles) {
            if (proj.getWeapon() == weapon) {
                projectile = proj;
                break;
            }
        }

        if (projectile == null) {
            return;
        }

        if (quadtree == null) {
            quadtree = new AEG_TargetingQuadtreeHelper(0, new Vector2f(engine.getMapWidth(), engine.getMapHeight()));
        }

        quadtree.clear();
        for (MissileAPI missile : engine.getMissiles()) {
            quadtree.insert(missile);
        }
        for (DamagingProjectileAPI proj : engine.getProjectiles()) {
            quadtree.insert(proj);
        }

        List<CombatEntityAPI> potentialTargets = new ArrayList<CombatEntityAPI>();
        quadtree.retrieve(potentialTargets, projectile);

        Vector2f projectileLocation = projectile.getLocation();
        float zapRangeSquared = ZAP_RANGE * ZAP_RANGE;

        boolean targetHit = false;
        for (CombatEntityAPI entity : potentialTargets) {
            if (entity.getOwner() != weapon.getShip().getOwner() && MathUtils.getDistanceSquared(projectileLocation, entity.getLocation()) <= zapRangeSquared) {
                zapTarget(engine, projectile, entity);
                charges--;
                targetHit = true;
                if (charges <= 0) {
                    return;
                }
            }
        }

        // If no target was hit, spawn EMP arcs at random locations
        if (!targetHit) {
            spawnRandomEmpArcs(engine, projectile);
        }
    }

    private void zapTarget(CombatEngineAPI engine, DamagingProjectileAPI source, CombatEntityAPI target) {
        Vector2f point = target.getLocation();
        Color coreColor = new Color(255, 255, 255);
        Color fringeColor = new Color(105, 255, 105);

        engine.spawnEmpArc(source.getSource(), source.getLocation(), source, target, DamageType.ENERGY, ZAP_DAMAGE, ZAP_DAMAGE, ZAP_RANGE, "tachyon_lance_emp_impact", 10f, fringeColor, coreColor);
    }

    private void spawnRandomEmpArcs(CombatEngineAPI engine, DamagingProjectileAPI projectile) {
        for (int i = 0; i < 3; i++) { // Spawn 3 random arcs
            Vector2f randomPoint = MathUtils.getRandomPointInCircle(projectile.getLocation(), ZAP_RANGE);
            Color coreColor = new Color(255, 255, 255);
            Color fringeColor = new Color(105, 255, 105);

            engine.spawnEmpArc(projectile.getSource(), projectile.getLocation(), projectile, null, DamageType.ENERGY, ZAP_DAMAGE, ZAP_DAMAGE, ZAP_RANGE, "tachyon_lance_emp_impact", 10f, fringeColor, coreColor);
        }
    }
}
