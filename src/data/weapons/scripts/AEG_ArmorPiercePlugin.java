package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

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

            if (spec.equals("AEG_4g_brokenmagnum_torp")) {
                applyBrokenMagnumEffect(engine, proj, entity, amount);
            }

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
        // === Pull nearby ships towards the projectile ===
        float pullRadius = 1000f; // Distance within which ships are affected
        float pullStrength = 250f; // Maximum pull force per second

        List<ShipAPI> nearbyShips = CombatUtils.getShipsWithinRange(proj.getLocation(), pullRadius);
        for (ShipAPI ship : nearbyShips) {
            if (ship == proj.getSource()) continue;

            Vector2f direction = Vector2f.sub(proj.getLocation(), ship.getLocation(), null);
            float distance = direction.length();
            if (distance <= 1f) continue; // Avoid divide-by-zero

            direction.normalise();
            float forceFactor = 1f - (distance / pullRadius); // Linear falloff
            float pullForce = pullStrength * forceFactor * amount; // Frame-scaled force

            Vector2f pull = new Vector2f(direction);
            pull.scale(pullForce);

            Vector2f.add(ship.getVelocity(), pull, ship.getVelocity());
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
    private void applyBrokenMagnumEffect(final CombatEngineAPI engine, final DamagingProjectileAPI proj, final CombatEntityAPI entity, float amount) {
        // === 1. Strong Knockback ===
        if (entity instanceof ShipAPI) {
            Vector2f knockback = Vector2f.sub(entity.getLocation(), proj.getLocation(), null);
            knockback.normalise(); // Fix here
            knockback.scale(25f);
            Vector2f.add(entity.getVelocity(), knockback, entity.getVelocity());
        }

        // === 2. Spiral Particle Trail ===
        for (int i = 0; i < 4; i++) {
            float angleOffset = (float) Math.random() * 360f;
            Vector2f offset = MathUtils.getPointOnCircumference(proj.getLocation(), 8f + (float)Math.random() * 4f, proj.getFacing() + angleOffset);
            engine.addSmoothParticle(
                    offset,
                    proj.getVelocity(),
                    12f + rand.nextInt(8),
                    1.2f,
                    0.2f,
                    new Color(50 + rand.nextInt(50),255 ,110 + rand.nextInt(60), 200 + rand.nextInt(55))
            );
        }

        // === 3. Occasional Corkscrew EMP Arc ===
        if (Math.random() < 0.025f) {
            engine.spawnEmpArcVisual(
                    proj.getLocation(),
                    proj,
                    proj.getLocation(),
                    entity,
                    25f + rand.nextInt(10),
                    new Color(255, 110 + rand.nextInt(70), 50 + rand.nextInt(50), 255 - rand.nextInt(135)),
                    new Color(255, 255  - rand.nextInt(115), 255 - rand.nextInt(65), 180 + rand.nextInt(25))
            );
        }

        // === 4. Bypass Armor Damage Tick ===
        if (Math.random() < 0.2f) {
            float tickDamage = 25f;
            float emp = 25f;

            engine.applyDamage(
                    entity,
                    proj.getLocation(),
                    tickDamage,
                    DamageType.HIGH_EXPLOSIVE,
                    emp,
                    true,
                    false,
                    proj.getSource()
            );
        }

        // === 5. Schedule Delayed Implosion ===
        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            private float timer = 1.25f;

            @Override
            public void advance(float amount, List events) {
                if (engine.isPaused()) return;
                timer -= amount;

                if (timer <= 0f) {
                    engine.spawnExplosion(
                            entity.getLocation(),
                            entity.getVelocity(),
                            new Color(255, 60, 60, 200),
                            proj.getCollisionRadius() * 4f,
                            1.2f
                    );

                    engine.applyDamage(
                            entity,
                            entity.getLocation(),
                            300,
                            DamageType.ENERGY,
                            proj.getEmpAmount(),
                            true,
                            true,
                            proj.getSource()
                    );

                    engine.spawnEmpArcVisual(
                            entity.getLocation(),
                            proj,
                            entity.getLocation(),
                            entity,
                            25f + rand.nextInt(15),
                            new Color(255, 50 + rand.nextInt(100), 50, 255),
                            new Color(255, 200, 200 - rand.nextInt(50), 180 + rand.nextInt(75))
                    );
                    engine.removePlugin(this);
                }
            }
        });
    }
}
