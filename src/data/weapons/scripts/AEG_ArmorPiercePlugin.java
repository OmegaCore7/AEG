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
    // Sound to play while piercing a target's armor (should be loopable!)
    private static final String PIERCE_SOUND = "explosion_flak"; // TEMPORARY
    // Projectile ID (String), pierces shields (boolean)
    private static final Map<String, Boolean> PROJ_IDS = new HashMap<>();
    // Keep track of the original collision class (used for shield hits)
    private static final Map<String, CollisionClass> originalCollisionClasses = new HashMap<>();
    // Track whether the initial burst of damage has been applied
    private static final Map<DamagingProjectileAPI, Boolean> initialHitApplied = new HashMap<>();
    // Track the flight time of projectiles
    private static final Map<DamagingProjectileAPI, Float> projectileFlightTimes = new HashMap<>();

    static {
        // Add all projectiles that should pierce armor here
        // Format: Projectile ID (String), pierces shields (boolean)
        PROJ_IDS.put("lw_impaler_shot", false);
        PROJ_IDS.put("AEG_ironcutter_G_torp", true);
    }

    @Override
    public void advance(float amount, List events) {
        final CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }

        for (DamagingProjectileAPI proj : engine.getProjectiles()) {
            String spec = proj.getProjectileSpecId();

            // Is this projectile armor piercing?
            if (!PROJ_IDS.containsKey(spec)) {
                continue;
            }

            // Register the original collision class (used for shield hits)
            if (!originalCollisionClasses.containsKey(spec)) {
                originalCollisionClasses.put(spec, proj.getCollisionClass());
            }

            // Track the flight time of the projectile
            if (!projectileFlightTimes.containsKey(proj)) {
                projectileFlightTimes.put(proj, 0f);
            }
            projectileFlightTimes.put(proj, projectileFlightTimes.get(proj) + amount);

            // We'll do collision checks manually
            proj.setCollisionClass(CollisionClass.NONE);

            // Find nearby ships, missiles and asteroids
            List toCheck = CombatUtils.getShipsWithinRange(proj.getLocation(),
                    proj.getCollisionRadius() + 5f);
            toCheck.addAll(CombatUtils.getMissilesWithinRange(proj.getLocation(),
                    proj.getCollisionRadius() + 5f));
            toCheck.addAll(CombatUtils.getAsteroidsWithinRange(proj.getLocation(),
                    proj.getCollisionRadius() + 5f));
            // Don't include the ship that fired this projectile!
            toCheck.remove(proj.getSource());

            for (Iterator iter2 = toCheck.iterator(); iter2.hasNext();) {
                CombatEntityAPI entity = (CombatEntityAPI) iter2.next();

                // Check for an active phase cloak
                if (entity instanceof ShipAPI) {
                    ShipSystemAPI cloak = ((ShipAPI) entity).getPhaseCloak();
                    if (cloak != null && cloak.isActive()) {
                        continue;
                    }
                }

                // Check for a shield hit
                if ((PROJ_IDS.get(spec) != true)
                        && (entity.getShield() != null
                        && entity.getShield().isOn()
                        && entity.getShield().isWithinArc(proj.getLocation()))) {
                    // If we hit a shield, enable collision
                    proj.setCollisionClass(originalCollisionClasses.get(spec));
                    // Stop the projectile (ensures a hit for fast projectiles)
                    proj.getVelocity().set(entity.getVelocity());
                    // Then move the projectile inside the ship's shield bounds
                    Vector2f.add((Vector2f) VectorUtils.getDirectionalVector(
                                    proj.getLocation(), entity.getLocation()).scale(5f),
                            proj.getLocation(), proj.getLocation());
                }
                // Check if the projectile is inside the entity's bounds
                else if (CollisionUtils.isPointWithinBounds(proj.getLocation(), entity)) {
                    // Apply initial burst of damage if not already applied
                    if (!initialHitApplied.containsKey(proj) || !initialHitApplied.get(proj)) {
                        engine.applyDamage(entity, proj.getLocation(), proj.getDamageAmount(),
                                proj.getDamageType(), proj.getEmpAmount(), true, true, proj.getSource());
                        initialHitApplied.put(proj, true);
                    }

                    // Calculate projectile speed
                    float speed = proj.getVelocity().length();

                    // Damage per frame is based on how long it would take
                    // the projectile to travel through the entity
                    float modifier = 1.0f / ((entity.getCollisionRadius()
                            * 2f) / speed);
                    float damage = (proj.getDamageAmount() * amount) * modifier;
                    float emp = (proj.getEmpAmount() * amount) * modifier;

                    // Apply damage and slow the projectile
                    // Note: BALLISTIC_AS_BEAM projectiles won't be slowed!
                    engine.applyDamage(entity, proj.getLocation(), damage,
                            proj.getDamageType(), emp, true, true, proj.getSource());
                    proj.getVelocity().scale(1.0f - (amount * 1.5f));

                    // Render the hit
                    engine.spawnExplosion(proj.getLocation(), entity.getVelocity(),
                            Color.ORANGE, speed * amount * .65f, .5f);

                    // Play piercing sound (only one sound active per projectile)
                    Global.getSoundPlayer().playLoop(PIERCE_SOUND, proj, 1f, 1f,
                            proj.getLocation(), entity.getVelocity());

                    // Schedule a delayed explosion
                    scheduleDelayedExplosion(engine, proj, entity);
                }
            }

            // Check if the projectile is a missile and its flight time has expired
            if (proj instanceof MissileAPI) {
                MissileSpecAPI missileSpec = (MissileSpecAPI) proj.getProjectileSpec();
                if (projectileFlightTimes.get(proj) >= missileSpec.getMaxFlightTime()) {
                    // Spawn an explosion at the projectile's location
                    engine.spawnExplosion(proj.getLocation(), proj.getVelocity(),
                            Color.ORANGE, proj.getCollisionRadius() * 3f, 1f);

                    // Find entities within the explosion radius
                    List<CombatEntityAPI> entities = CombatUtils.getEntitiesWithinRange(proj.getLocation(), proj.getCollisionRadius() * 3f);

                    // Apply explosion damage to each entity
                    for (CombatEntityAPI entity : entities) {
                        engine.applyDamage(entity, proj.getLocation(), proj.getDamageAmount(),
                                proj.getDamageType(), proj.getEmpAmount(), true, true, proj.getSource());
                    }

                    // Remove the projectile from the game
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
                    // Spawn an explosion at the entity's location
                    engine.spawnExplosion(entity.getLocation(), entity.getVelocity(),
                            Color.ORANGE, proj.getCollisionRadius() * 3f, 1f);

                    // Apply explosion damage to the entity
                    engine.applyDamage(entity, entity.getLocation(), proj.getDamageAmount(),
                            proj.getDamageType(), proj.getEmpAmount(), true, true, proj.getSource());

                    // Remove this plugin
                    engine.removePlugin(this);
                }
            }
        });
    }
}
