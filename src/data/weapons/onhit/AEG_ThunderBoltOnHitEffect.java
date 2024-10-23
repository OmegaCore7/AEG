package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import java.util.*;

public class AEG_ThunderBoltOnHitEffect implements BeamEffectPlugin {
    private static final Color CORE_COLOR = new Color(255, 255, 255, 255);
    private static final Color FRINGE_COLOR = new Color(105, 105, 255, 255);
    private static final float INITIAL_CHAIN_RANGE = 1500f;
    private static final float RANGE_RETENTION_PER_CHAIN = .75f;
    private static final float DAMAGE_RETENTION_PER_CHAIN = .85f;
    private static final int MAXIMUM_CHAINS = 5;
    private final IntervalUtil fireInterval = new IntervalUtil(0.1f, 0.1f);
    private final IntervalUtil beamLightningInterval = new IntervalUtil(1f, 2f);

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        // Handle the beam effect
        if (beam.getBrightness() >= 1f) {
            fireInterval.advance(amount);
            if (fireInterval.intervalElapsed()) {
                createChainLightning(beam, beam.getDamageTarget(), engine);
            }

            // Add small lightning arcs traveling up the beam every 1-2 seconds
            beamLightningInterval.advance(amount);
            if (beamLightningInterval.intervalElapsed()) {
                Vector2f from = beam.getFrom();
                Vector2f to = beam.getTo();
                float distance = getDistance(from, to);
                for (float i = 0; i < distance; i += 10 + Math.random() * 5) {
                    Vector2f point = new Vector2f(from.x + (to.x - from.x) * (i / distance), from.y + (to.y - from.y) * (i / distance));
                    engine.spawnEmpArcVisual(point, null, new Vector2f(point.x + (float) (Math.random() * 10 - 5), point.y + (float) (Math.random() * 10 - 5)), null, 5f, CORE_COLOR, FRINGE_COLOR);
                }
            }
        }
    }

    private void createChainLightning(BeamAPI beam, CombatEntityAPI initialTarget, CombatEngineAPI engine) {
        if (initialTarget == null) return;

        CombatEntityAPI currentTarget = initialTarget;
        Vector2f source = beam.getTo();
        float range = INITIAL_CHAIN_RANGE;
        float damage = beam.getWeapon().getDerivedStats().getDps() * beam.getSource().getMutableStats().getBeamWeaponDamageMult().getModifiedValue() * fireInterval.getIntervalDuration();
        float emp = beam.getWeapon().getDerivedStats().getEmpPerSecond() * fireInterval.getIntervalDuration();
        Set<CombatEntityAPI> struck = new HashSet<>();

        for (int i = 0; i < MAXIMUM_CHAINS; i++) {
            if (currentTarget == null) break;

            engine.spawnEmpArc(beam.getSource(), source, currentTarget, currentTarget,
                    DamageType.ENERGY, damage, emp, 100000f, "tachyon_lance_emp_impact", 15f, FRINGE_COLOR, CORE_COLOR);

            struck.add(currentTarget);
            range *= RANGE_RETENTION_PER_CHAIN;
            damage *= DAMAGE_RETENTION_PER_CHAIN;
            emp *= DAMAGE_RETENTION_PER_CHAIN;

            List<CombatEntityAPI> enemies = getEnemiesInRange(currentTarget, range, beam.getSource().getOwner(), engine);
            enemies.removeAll(struck);

            if (enemies.isEmpty()) break;
            currentTarget = enemies.get((int) (Math.random() * enemies.size()));
            source = currentTarget.getLocation();
        }
    }

    private List<CombatEntityAPI> getEnemiesInRange(CombatEntityAPI source, float range, int owner, CombatEngineAPI engine) {
        List<CombatEntityAPI> enemies = new ArrayList<>();
        for (ShipAPI entity : engine.getShips()) {
            if (entity != null && entity.getOwner() != owner && !entity.isHulk() && getDistance(source.getLocation(), entity.getLocation()) <= range) {
                enemies.add(entity);
            }
        }
        return enemies;
    }

    private float getDistance(Vector2f point1, Vector2f point2) {
        return (float) Math.hypot(point1.x - point2.x, point1.y - point2.y);
    }
}
