package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

//Refactored from Lazywizards's Original Armor Pierce script. Give credit where credits do.
public class AEG_ArmorPiercePlugin extends BaseEveryFrameCombatPlugin {
    private static final String PIERCE_SOUND = "explosion_flak";

    // Projectile behavior maps
    private static final Map<String, Boolean> PIERCING_PROJECTILES = new HashMap<String, Boolean>();
    private static final Map<String, Boolean> DELAYED_EXPLOSION_PROJECTILES = new HashMap<String, Boolean>();
    private static final Map<String, Boolean> CUSTOM_PIERCE_EFFECT_PROJECTILES = new HashMap<String, Boolean>();
    private static final Map<String, Boolean> GRAVITY_PULL_PROJECTILES = new HashMap<String, Boolean>();
    private static final Map<String, Boolean> PIERCE_EXCEPT_HARDENED_SHIELDS = new HashMap<>();

    // Runtime state
    private final Map<String, CollisionClass> originalCollisionClasses = new HashMap<String, CollisionClass>();
    private final Map<DamagingProjectileAPI, Boolean> initialHitApplied = new HashMap<DamagingProjectileAPI, Boolean>();
    private final Map<DamagingProjectileAPI, Float> projectileFlightTimes = new HashMap<DamagingProjectileAPI, Float>();

    private Random rand = new Random();

    static {
        PIERCING_PROJECTILES.put("AEG_ironcutter_G_torp", true);
        PIERCING_PROJECTILES.put("Aeg_OmegaBlaster_Shot", true);
        PIERCING_PROJECTILES.put("AEG_4g_brokenmagnum_torp", true);
        PIERCING_PROJECTILES.put("AEG_rust_torp", true);

        //Does it pierce Shields?
        CUSTOM_PIERCE_EFFECT_PROJECTILES.put("AEG_ironcutter_G_torp", true);
        CUSTOM_PIERCE_EFFECT_PROJECTILES.put("Aeg_OmegaBlaster_Shot", true);
        CUSTOM_PIERCE_EFFECT_PROJECTILES.put("AEG_4g_brokenmagnum_torp", true);


        // Pierce shields but not Hardened Shields
        PIERCE_EXCEPT_HARDENED_SHIELDS.put("AEG_rust_torp", true); // example

        //Extra Effects
        GRAVITY_PULL_PROJECTILES.put("Aeg_OmegaBlaster_Shot", true); // currently only one

        //Dynamic Delayed Explosion
        DELAYED_EXPLOSION_PROJECTILES.put("AEG_ironcutter_G_torp", true);
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
            if (GRAVITY_PULL_PROJECTILES.containsKey(spec)) {
                applyGravityPullEffect(engine, proj, amount);
            }
            handleEntityPierce(proj, engine, amount);
            handleProjectileExpiration(proj, engine);
        }
        // Cleanup for projectileFlightTimes
        Iterator<DamagingProjectileAPI> flightIterator = projectileFlightTimes.keySet().iterator();
        while (flightIterator.hasNext()) {
            DamagingProjectileAPI p = flightIterator.next();
            if (!engine.isEntityInPlay(p)) {
                flightIterator.remove();
            }
        }

// Cleanup for initialHitApplied
        Iterator<DamagingProjectileAPI> hitIterator = initialHitApplied.keySet().iterator();
        while (hitIterator.hasNext()) {
            DamagingProjectileAPI p = hitIterator.next();
            if (!engine.isEntityInPlay(p)) {
                hitIterator.remove();
            }
        }
    }

    private void handleEntityPierce(DamagingProjectileAPI proj, CombatEngineAPI engine, float amount) {
        List<CombatEntityAPI> targets = CombatUtils.getEntitiesWithinRange(proj.getLocation(), proj.getCollisionRadius());
        targets.remove(proj.getSource());

        for (CombatEntityAPI entity : targets) {
            if (entity instanceof MissileAPI || entity instanceof DamagingProjectileAPI) {
                // If projectile is part of the delayed explosion type, remove other projectiles
                if (DELAYED_EXPLOSION_PROJECTILES.containsKey(proj.getProjectileSpecId())) {
                    if (entity != proj) {
                        engine.spawnExplosion(
                                entity.getLocation(),
                                entity.getVelocity(),
                                new Color(255 - rand.nextInt(205), 100 + rand.nextInt(80), 50 + rand.nextInt(100), 180 + rand.nextInt(50)),
                                5f + rand.nextInt(35),
                                0.25f
                        );
                        engine.removeEntity(entity);
                    }
                }
                continue; // Skip further processing for wiped ordnance
            }
        }

        for (int i = 0; i < targets.size(); i++) {
            CombatEntityAPI entity = targets.get(i);
            if (!CollisionUtils.isPointWithinBounds(proj.getLocation(), entity)) continue;

            if (entity instanceof ShipAPI) {
                ShipSystemAPI cloak = ((ShipAPI) entity).getPhaseCloak();
                if (cloak != null && cloak.isActive()) continue;
            }

            String spec = proj.getProjectileSpecId();

            if (entity instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) entity;
                ShieldAPI shield = ship.getShield();

                if (shield != null && shield.isWithinArc(proj.getLocation())) {
                    boolean hasHardenedShields = ship.getVariant().hasHullMod("hardenedshields");

                    // 1. Check if this projectile should not pierce any shield at all
                    if (!CUSTOM_PIERCE_EFFECT_PROJECTILES.containsKey(spec)) {
                        proj.setCollisionClass(originalCollisionClasses.get(spec));
                        return;
                    }

                    // 2. Check if this projectile should pierce shields, but *not* Hardened Shields
                    if (PIERCE_EXCEPT_HARDENED_SHIELDS.containsKey(spec) && hasHardenedShields) {
                        proj.setCollisionClass(originalCollisionClasses.get(spec));
                        return;
                    }
                }
            }

            // Apply custom effects if applicable
            if (!initialHitApplied.containsKey(proj)) {
                applyInitialDamage(engine, proj, entity);
                initialHitApplied.put(proj, true);
            }

            if (DELAYED_EXPLOSION_PROJECTILES.containsKey(spec)) {
                scheduleDelayedExplosion(engine, proj, entity);
            }

            if (CUSTOM_PIERCE_EFFECT_PROJECTILES.containsKey(spec)) {
                applyCustomPierceEffect(engine, proj, entity, amount);
            }

            if (spec.equals("AEG_4g_brokenmagnum_torp")) {
                applyBrokenMagnumEffect(engine, proj, entity, amount);
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
            Vector2f offset = MathUtils.getPointOnCircumference(proj.getLocation(), 8f + (float) Math.random() * 4f, proj.getFacing() + angleOffset);
            engine.addSmoothParticle(
                    offset,
                    proj.getVelocity(),
                    12f + rand.nextInt(8),
                    1.2f,
                    0.2f,
                    new Color(50 + rand.nextInt(50), 255, 110 + rand.nextInt(60), 200 + rand.nextInt(55))
            );
        }

        // === 3. Occasional Corkscrew EMP Arc ===
        if (Math.random() < 0.2f) {
            engine.spawnEmpArcVisual(
                    proj.getLocation(),
                    proj,
                    proj.getLocation(),
                    entity,
                    25f + rand.nextInt(10),
                    new Color(255, 110 + rand.nextInt(70), 50 + rand.nextInt(50), 255 - rand.nextInt(135)),
                    new Color(255, 255 - rand.nextInt(115), 255 - rand.nextInt(65), 180 + rand.nextInt(25))
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

    private void applyGravityPullEffect(CombatEngineAPI engine, DamagingProjectileAPI proj, float amount) {
        float pullRadius = 1000f;
        float pullStrength = 250f;

        List<ShipAPI> nearbyShips = CombatUtils.getShipsWithinRange(proj.getLocation(), pullRadius);
        for (ShipAPI ship : nearbyShips) {
            if (ship == proj.getSource()) continue;

            Vector2f direction = Vector2f.sub(proj.getLocation(), ship.getLocation(), null);
            float distance = direction.length();
            if (distance <= 1f) continue;

            direction.normalise();
            float forceFactor = 1f - (distance / pullRadius);
            float pullForce = pullStrength * forceFactor * amount;

            Vector2f pull = new Vector2f(direction);
            pull.scale(pullForce);

            Vector2f.add(ship.getVelocity(), pull, ship.getVelocity());
        }
    }

}