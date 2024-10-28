package data.weapons.scripts;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
public class AEG_ArmorPiercePlugin extends BaseEveryFrameCombatPlugin {
    private static final String PIERCE_SOUND = "explosion_flak"; // TEMPORARY
    private static final Map<String, Boolean> PROJ_IDS = new HashMap<>();
    private static final Map<String, CollisionClass> originalCollisionClasses = new HashMap<>();
    private static final Map<DamagingProjectileAPI, Boolean> initialHitApplied = new HashMap<>();
    private static final Map<DamagingProjectileAPI, Float> projectileFlightTimes = new HashMap<>();

    static {
        PROJ_IDS.put("lw_impaler_shot", false);
        PROJ_IDS.put("AEG_ironcutter_G_torp", true);
        PROJ_IDS.put("Aeg_OmegaBlaster_Shot", true);
    }
    @Override
    public void advance(float amount, List events) {
        final CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }

        for (DamagingProjectileAPI proj : engine.getProjectiles()) {
            String spec = proj.getProjectileSpecId();

            if (spec == null || !PROJ_IDS.containsKey(spec)) {
                continue;
            }

            if (!originalCollisionClasses.containsKey(spec)) {
                originalCollisionClasses.put(spec, proj.getCollisionClass());
            }

            if (!projectileFlightTimes.containsKey(proj)) {
                projectileFlightTimes.put(proj, 0f);
            }
            projectileFlightTimes.put(proj, projectileFlightTimes.get(proj) + amount);

            proj.setCollisionClass(CollisionClass.NONE);

            List toCheck = CombatUtils.getShipsWithinRange(proj.getLocation(),
                    proj.getCollisionRadius() + 5f);
            toCheck.addAll(CombatUtils.getMissilesWithinRange(proj.getLocation(),
                    proj.getCollisionRadius() + 5f));
            toCheck.addAll(CombatUtils.getAsteroidsWithinRange(proj.getLocation(),
                    proj.getCollisionRadius() + 5f));
            toCheck.remove(proj.getSource());

            for (Iterator iter2 = toCheck.iterator(); iter2.hasNext();) {
                CombatEntityAPI entity = (CombatEntityAPI) iter2.next();

                if (entity instanceof ShipAPI) {
                    ShipSystemAPI cloak = ((ShipAPI) entity).getPhaseCloak();
                    if (cloak != null && cloak.isActive()) {
                        continue;
                    }
                }

                if ((PROJ_IDS.get(spec) != true)
                        && (entity.getShield() != null
                        && entity.getShield().isOn()
                        && entity.getShield().isWithinArc(proj.getLocation()))) {
                    proj.setCollisionClass(originalCollisionClasses.get(spec));
                    proj.getVelocity().set(entity.getVelocity());
                    Vector2f.add((Vector2f) VectorUtils.getDirectionalVector(
                                    proj.getLocation(), entity.getLocation()).scale(5f),
                            proj.getLocation(), proj.getLocation());
                } else if (CollisionUtils.isPointWithinBounds(proj.getLocation(), entity)) {
                    if (!initialHitApplied.containsKey(proj) || !initialHitApplied.get(proj)) {
                        engine.applyDamage(entity, proj.getLocation(), proj.getDamageAmount(),
                                proj.getDamageType(), proj.getEmpAmount(), true, true, proj.getSource());
                        initialHitApplied.put(proj, true);
                    }

                    float speed = proj.getVelocity().length();
                    float modifier = 1.0f / ((entity.getCollisionRadius() * 2f) / speed);
                    float damage = (proj.getDamageAmount() * amount) * modifier;
                    float emp = (proj.getEmpAmount() * amount) * modifier;

                    engine.applyDamage(entity, proj.getLocation(), damage,
                            proj.getDamageType(), emp, true, true, proj.getSource());
                    proj.getVelocity().scale(1.0f - (amount * 1.5f));

                    engine.spawnExplosion(proj.getLocation(), entity.getVelocity(),
                            Color.ORANGE, speed * amount * .65f, .5f);

                    Global.getSoundPlayer().playLoop(PIERCE_SOUND, proj, 1f, 1f,
                            proj.getLocation(), entity.getVelocity());

                    scheduleDelayedExplosion(engine, proj, entity);
                }
            }

            if (proj instanceof MissileAPI) {
                MissileSpecAPI missileSpec = (MissileSpecAPI) proj.getProjectileSpec();
                if (missileSpec != null && projectileFlightTimes.get(proj) >= missileSpec.getMaxFlightTime()) {
                    engine.spawnExplosion(proj.getLocation(), proj.getVelocity(),
                            Color.ORANGE, proj.getCollisionRadius() * 3f, 1f);

                    List<CombatEntityAPI> entities = CombatUtils.getEntitiesWithinRange(proj.getLocation(), proj.getCollisionRadius() * 3f);

                    for (CombatEntityAPI entity : entities) {
                        engine.applyDamage(entity, proj.getLocation(), proj.getDamageAmount(),
                                proj.getDamageType(), proj.getEmpAmount(), true, true, proj.getSource());
                    }

                    engine.removeEntity(proj);
                }
            }
        }
    }
    private void scheduleDelayedExplosion(final CombatEngineAPI engine, final DamagingProjectileAPI proj, final CombatEntityAPI entity) {
        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            private float timer = 2f;

            @Override
            public void advance(float amount, List events) {
                if (engine.isPaused()) {
                    return;
                }

                timer -= amount;
                if (timer <= 0f) {
                    engine.spawnExplosion(entity.getLocation(), entity.getVelocity(),
                            Color.ORANGE, proj.getCollisionRadius() * 3f, 1f);

                    engine.applyDamage(entity, entity.getLocation(), proj.getDamageAmount(),
                            proj.getDamageType(), proj.getEmpAmount(), true, true, proj.getSource());

                    engine.removePlugin(this);
                }
            }
        });
    }
}
