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

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        // Handle the lightning charging effect
        if (beam.getWeapon().getChargeLevel() > 0) {
            for (int i = 0; i < beam.getWeapon().getSpec().getHardpointAngleOffsets().size(); i++) {
                Vector2f point = beam.getWeapon().getFirePoint(i);
                engine.addHitParticle(point, new Vector2f(), 10f, beam.getWeapon().getChargeLevel(), 0.1f, CORE_COLOR);
            }
        }

        CombatEntityAPI target = beam.getDamageTarget();
        if (target instanceof ShipAPI && (target.getShield() == null || !target.getShield().isWithinArc(beam.getTo()))) {
            if (beam.getBrightness() >= 1f) {
                fireInterval.advance(amount);
                if (fireInterval.intervalElapsed()) {
                    createChainLightning(beam, target, engine);
                }
            }
        }
    }

    private void createChainLightning(BeamAPI beam, CombatEntityAPI initialTarget, CombatEngineAPI engine) {
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
