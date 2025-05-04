package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AEG_ArmorPiercePlugin extends BaseEveryFrameCombatPlugin {
    private static final String PIERCE_SOUND = "explosion_flak";

    // Projectile behavior maps
    private static final Map<String, Boolean> PIERCING_PROJECTILES = new HashMap<String, Boolean>();
    private static final Map<String, Boolean> DELAYED_EXPLOSION_PROJECTILES = new HashMap<String, Boolean>();
    private static final Map<String, Boolean> CUSTOM_PIERCE_EFFECT_PROJECTILES = new HashMap<String, Boolean>();

    // Runtime state
    private final Map<String, CollisionClass> originalCollisionClasses = new HashMap<String, CollisionClass>();
    private final Map<DamagingProjectileAPI, Boolean> initialHitApplied = new HashMap<DamagingProjectileAPI, Boolean>();
    private final Map<DamagingProjectileAPI, Float> projectileFlightTimes = new HashMap<DamagingProjectileAPI, Float>();
    private Random rand = new Random();

    static {
        PIERCING_PROJECTILES.put("AEG_ironcutter_G_torp", true);
        PIERCING_PROJECTILES.put("Aeg_OmegaBlaster_Shot", true);
        PIERCING_PROJECTILES.put("AEG_4g_brokenmagnum_torp", true);

        DELAYED_EXPLOSION_PROJECTILES.put("AEG_ironcutter_G_torp", true);
        CUSTOM_PIERCE_EFFECT_PROJECTILES.put("Aeg_OmegaBlaster_Shot", true);
        DELAYED_EXPLOSION_PROJECTILES.put("Aeg_OmegaBlaster_Shot", true);
    }

    @Override
    public void advance(float amount, List events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;

        List projectiles = engine.getProjectiles();
        for (int i = 0; i < projectiles.size(); i++) {
            DamagingProjectileAPI proj = (DamagingProjectileAPI) projectiles.get(i);
            String spec = proj.getProjectileSpecId();
            if (spec == null || !PIERCING_PROJECTILES.containsKey(spec)) continue;

            if (!originalCollisionClasses.containsKey(spec)) {
                originalCollisionClasses.put(spec, proj.getCollisionClass());
            }

            if (!projectileFlightTimes.containsKey(proj)) {
                projectileFlightTimes.put(proj, 0f);
            }
            projectileFlightTimes.put(proj, projectileFlightTimes.get(proj) + amount);

            proj.setCollisionClass(CollisionClass.NONE);

            handleEntityPierce(proj, engine, amount);
            handleProjectileExpiration(proj, engine);
        }
    }

    private void handleEntityPierce(DamagingProjectileAPI proj, CombatEngineAPI engine, float amount) {
        List targets = CombatUtils.getEntitiesWithinRange(proj.getLocation(), proj.getCollisionRadius() + 5f);
        targets.remove(proj.getSource());

        for (int i = 0; i < targets.size(); i++) {
            CombatEntityAPI entity = (CombatEntityAPI) targets.get(i);
            if (!CollisionUtils.isPointWithinBounds(proj.getLocation(), entity)) continue;

            if (entity instanceof ShipAPI) {
                ShipSystemAPI cloak = ((ShipAPI) entity).getPhaseCloak();
                if (cloak != null && cloak.isActive()) continue;
            }

            String spec = proj.getProjectileSpecId();

            if (!CUSTOM_PIERCE_EFFECT_PROJECTILES.containsKey(spec)
                    && entity.getShield() != null
                    && entity.getShield().isWithinArc(proj.getLocation())) {
                proj.setCollisionClass(originalCollisionClasses.get(spec));
                return;
            }

            if (!initialHitApplied.containsKey(proj)) {
                applyInitialDamage(engine, proj, entity);
                initialHitApplied.put(proj, true);

                if (DELAYED_EXPLOSION_PROJECTILES.containsKey(spec)) {
                    scheduleDelayedExplosion(engine, proj, entity);
                }
            }

            if (CUSTOM_PIERCE_EFFECT_PROJECTILES.containsKey(spec)) {
                applyCustomPierceEffect(engine, proj, entity, amount);
            }

            proj.getVelocity().scale(1.0f - (amount * 1.5f));

            engine.spawnExplosion(
                    proj.getLocation(),
                    entity.getVelocity(),
                    Color.ORANGE,
                    proj.getVelocity().length() * amount * 0.65f,
                    0.5f
            );

            Global.getSoundPlayer().playLoop(
                    PIERCE_SOUND,
                    proj,
                    1f,
                    1f,
                    proj.getLocation(),
                    entity.getVelocity()
            );
        }
    }

    private void applyInitialDamage(CombatEngineAPI engine, DamagingProjectileAPI proj, CombatEntityAPI entity) {
        engine.applyDamage(
                entity,
                proj.getLocation(),
                proj.getDamageAmount(),
                proj.getDamageType(),
                proj.getEmpAmount(),
                true,
                true,
                proj.getSource()
        );
    }

    private void applyCustomPierceEffect(CombatEngineAPI engine, DamagingProjectileAPI proj, CombatEntityAPI entity, float amount) {
        if (Math.random() < 0.05f) {
            float tickDamage = proj.getDamageAmount() * 0.25f;
            float emp = proj.getEmpAmount() * 0.25f;

            engine.applyDamage(
                    entity,
                    proj.getLocation(),
                    tickDamage,
                    proj.getDamageType(),
                    emp,
                    true,
                    true,
                    proj.getSource()
            );

            engine.spawnEmpArcVisual(
                    proj.getLocation(),
                    proj,
                    proj.getLocation(),
                    entity,
                    10f + rand.nextInt(40),
                    new Color(255 - rand.nextInt(105),255,50 + rand.nextInt(50),255 - rand.nextInt(75)),
                    new Color(255 - rand.nextInt(55),255,255 - rand.nextInt(70),255 - rand.nextInt(65))
            );
        }
    }

    private void handleProjectileExpiration(DamagingProjectileAPI proj, CombatEngineAPI engine) {
        if (!(proj instanceof MissileAPI)) return;

        MissileSpecAPI missileSpec = (MissileSpecAPI) proj.getProjectileSpec();
        if (missileSpec == null) return;

        Float time = projectileFlightTimes.get(proj);
        if (time != null && time >= missileSpec.getMaxFlightTime()) {
            engine.spawnExplosion(
                    proj.getLocation(),
                    proj.getVelocity(),
                    Color.ORANGE,
                    proj.getCollisionRadius() * 3f,
                    1f
            );

            List affected = CombatUtils.getEntitiesWithinRange(
                    proj.getLocation(),
                    proj.getCollisionRadius() * 3f
            );

            for (int i = 0; i < affected.size(); i++) {
                CombatEntityAPI entity = (CombatEntityAPI) affected.get(i);
                engine.applyDamage(
                        entity,
                        proj.getLocation(),
                        proj.getDamageAmount(),
                        proj.getDamageType(),
                        proj.getEmpAmount(),
                        true,
                        true,
                        proj.getSource()
                );
            }

            engine.removeEntity(proj);
        }
    }

    private void scheduleDelayedExplosion(final CombatEngineAPI engine, final DamagingProjectileAPI proj, final CombatEntityAPI entity) {
        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            private float timer = 2f;

            @Override
            public void advance(float amount, List events) {
                if (engine.isPaused()) return;

                timer -= amount;
                if (timer <= 0f) {
                    engine.spawnExplosion(
                            entity.getLocation(),
                            entity.getVelocity(),
                            Color.ORANGE,
                            proj.getCollisionRadius() * 3f,
                            1f
                    );

                    engine.applyDamage(
                            entity,
                            entity.getLocation(),
                            proj.getDamageAmount(),
                            proj.getDamageType(),
                            proj.getEmpAmount(),
                            true,
                            true,
                            proj.getSource()
                    );

                    engine.removePlugin(this);
                }
            }
        });
    }
}
