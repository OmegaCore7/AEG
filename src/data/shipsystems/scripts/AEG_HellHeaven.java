package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.shipsystems.helpers.AEG_4g_HHImpact;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AEG_HellHeaven extends BaseShipSystemScript {

    private static final float RADIUS = 800f;
    private static final Color NEBULA_COLOR = new Color(0, 255, 100, 255); // Green color
    private IntervalUtil effectInterval = new IntervalUtil(0.1f, 0.1f); // Interval for effect updates
    private IntervalUtil smallLightningInterval = new IntervalUtil(1f, 2f); // Interval for small lightning effects
    private IntervalUtil largeLightningInterval = new IntervalUtil(4f, 4f); // Interval for large lightning bolt
    private IntervalUtil enemyLightningInterval = new IntervalUtil(2f, 2f); // Interval for enemy lightning strikes
    private boolean effectActive = false;
    private float chargeUpTime = 4f; // Charge-up time
    private int leftIndex = 0;
    private int rightIndex = 15;

    @Override
    public void apply(final MutableShipStatsAPI stats, final String id, final State state, final float effectLevel) {
        final ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        if (state == State.IN) {
            effectActive = false;
        }

        if (state == State.ACTIVE) {
            if (!effectActive) {
                effectActive = true;
                Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                    private float timer = 0f;
                    private List<Vector2f> ringPoints = new ArrayList<>();

                    @Override
                    public void advance(float amount, List<InputEventAPI> events) {
                        if (!effectActive) {
                            Global.getCombatEngine().removePlugin(this);
                            return;
                        }

                        timer += amount;
                        effectInterval.advance(amount);
                        smallLightningInterval.advance(amount);
                        largeLightningInterval.advance(amount);
                        enemyLightningInterval.advance(amount);

                        Vector2f location = ship.getLocation();
                        float radius;
                        if (timer <= chargeUpTime) {
                            // Charge-up phase: expanding ring
                            radius = RADIUS * (timer / chargeUpTime);
                        } else {
                            // Fully expanded phase
                            radius = RADIUS;
                        }

                        // Ensure ringPoints is populated correctly
                        if (ringPoints.isEmpty() || radius != RADIUS) {
                            ringPoints.clear();
                            int numPoints = 30; // Number of points along the circumference
                            for (int i = 0; i < numPoints; i++) {
                                float angle = (float) (i * 2 * Math.PI / numPoints);
                                ringPoints.add(new Vector2f(
                                        location.x + radius * (float) Math.cos(angle),
                                        location.y + radius * (float) Math.sin(angle)
                                ));
                            }
                        }

                        // Create green nebula ring
                        for (int i = 0; i < 5; i++) {
                            float angle = (float) (Math.random() * 2 * Math.PI);
                            Vector2f nebulaPoint = new Vector2f(
                                    location.x + radius * (float) Math.cos(angle),
                                    location.y + radius * (float) Math.sin(angle)
                            );
                            float nebulaSize = 50f + (float)(Math.random() * 100f);
                            Global.getCombatEngine().addNebulaParticle(nebulaPoint, new Vector2f(), nebulaSize, 1, 0.5f, 0.5f, 1f, NEBULA_COLOR);
                        }

                        // Create small lightning effects every 1 to 2 seconds
                        if (smallLightningInterval.intervalElapsed() && timer > chargeUpTime) {
                            Vector2f startPoint = ringPoints.get(leftIndex);
                            Vector2f endPoint = ringPoints.get(rightIndex);
                            if (startPoint != null && endPoint != null) {
                                float width = 10f + (float)(Math.random() * 20f); // Random width
                                float transparency = 0.5f + (float)(Math.random() * 0.5f); // Random transparency
                                Global.getCombatEngine().spawnEmpArc(ship, startPoint, ship, ship, DamageType.ENERGY, 0, 0, 1100f, null, width, NEBULA_COLOR, new Color(200,255,200,255));
                            }
                            leftIndex = (leftIndex + 1) % 15;
                            rightIndex = 15 + ((rightIndex + 1) % 15);
                        }

                        // Create large lightning bolt every 4 seconds
                        if (largeLightningInterval.intervalElapsed() && timer > chargeUpTime) {
                            Vector2f startPoint = ringPoints.get(leftIndex);
                            Vector2f endPoint = ringPoints.get(rightIndex);
                            if (startPoint != null && endPoint != null) {
                                float width = 40f + (float)(Math.random() * 20f); // Random width
                                float transparency = 0.5f + (float)(Math.random() * 0.5f); // Random transparency
                                Global.getCombatEngine().spawnEmpArc(ship, startPoint, ship, ship, DamageType.ENERGY, 0, 0, 1100f, null, width, NEBULA_COLOR, new Color(200,255,200,255));
                            }
                            leftIndex = (leftIndex + 1) % 15;
                            rightIndex = 15 + ((rightIndex + 1) % 15);
                        }

                        // Reflect projectiles and missiles
                        reflectProjectilesAndMissiles(ship);

                        // Apply damage to other ships within the field
                        applyFieldDamage(ship, amount);

                        // Apply slowing and flux overload to enemy ships within the field
                        applySlowingAndFluxOverload(ship, amount);

                        // Create green lightning strikes on enemy ships within the field
                        if (enemyLightningInterval.intervalElapsed()) {
                            createEnemyLightningStrikes(ship);
                        }
                        // Integrate the helper script for impact effects
                        AEG_4g_HHImpact impactHelper = new AEG_4g_HHImpact();
                        impactHelper.apply(stats, id, state, effectLevel);

                        // Gradually increase player ship flux while system is active
                        if (ship == Global.getCombatEngine().getPlayerShip()) {
                            float maxFlux = ship.getMaxFlux();
                            float ventRate = ship.getMutableStats().getVentRateMult().base;
                            float systemDuration = ship.getSystem().getChargeUpDur() + ship.getSystem().getChargeActiveDur() + ship.getSystem().getChargeDownDur();
                            float fluxPerSecond = maxFlux + ventRate / systemDuration;
                            ship.getFluxTracker().increaseFlux(fluxPerSecond * amount, false);
                        }
                    }
                });

                // Increase ship speed and maneuverability
                stats.getMaxSpeed().modifyMult(id, 3f);
                stats.getAcceleration().modifyMult(id, 2f);
                stats.getDeceleration().modifyMult(id, 2f);
                stats.getTurnAcceleration().modifyMult(id, 2f);
                stats.getMaxTurnRate().modifyMult(id, 2f);

                // Increase visual flame size on engines
                ship.getEngineController().extendFlame(id, 1.5f, 0.5f, 0.5f);

                // Reduce damage received by 99%
                stats.getHullDamageTakenMult().modifyMult(id, 0.01f);
                stats.getArmorDamageTakenMult().modifyMult(id, 0.01f);
                stats.getShieldDamageTakenMult().modifyMult(id, 0.01f);
            }
        } else {
            if (effectActive) {
                effectActive = false;

                final CombatEngineAPI engine = Global.getCombatEngine();
                final Vector2f loc = ship.getLocation();

                // Quick burst of green light on system end
                engine.addSmoothParticle(loc, new Vector2f(), 300f, 2f, 0.5f + (float)(Math.random() * 1f), new Color(0, 255, 100, 255));
                engine.addSmoothParticle(loc, new Vector2f(), 150f, 1.5f, 0.5f + (float)(Math.random() * 1f), new Color(150, 255, 150, 200));
                engine.addSmoothParticle(loc, new Vector2f(), 80f, 1.2f, 0.4f + (float)(Math.random() * 0.9f), new Color(200, 255, 200, 180));

                // Lingering fading nebula trail effect
                engine.addPlugin(new BaseEveryFrameCombatPlugin() {
                    private float duration = 1f + (float)(Math.random() * 2f); // How long the trail lasts
                    private float timer = 0f;

                    @Override
                    public void advance(float amount, List<InputEventAPI> events) {
                        timer += amount;
                        if (timer > duration) {
                            engine.removePlugin(this);
                            return;
                        }

                        // Radiating particles
                        for (int i = 0; i < 6; i++) {
                            float angle = (float) (Math.random() * 2f * Math.PI);
                            float speed = 50f + (float) (Math.random() * 200f);
                            Vector2f velocity = new Vector2f(
                                    speed * (float) Math.cos(angle),
                                    speed * (float) Math.sin(angle)
                            );

                            engine.addNebulaParticle(
                                    ship.getLocation(),
                                    velocity,
                                    30f + (float) (Math.random() * 50f),
                                    1.5f,
                                    0.5f,
                                    0.5f,
                                    1f,
                                    new Color(0, 255, 100, 150)
                            );
                            ship.setVentCoreColor(new Color(0,255,180,255));
                            ship.setVentFringeColor(new Color(0,255,100,255));
                        }
                    }
                });
            }
        }

            stats.getHullDamageTakenMult().unmodify(id);
            stats.getArmorDamageTakenMult().unmodify(id);
            stats.getShieldDamageTakenMult().unmodify(id);
        }

    private void reflectProjectilesAndMissiles(ShipAPI ship) {
        for (DamagingProjectileAPI projectile : Global.getCombatEngine().getProjectiles()) {
            if (projectile.getOwner() != ship.getOwner() && MathUtils.getDistance(ship, projectile) < RADIUS) {
                // Reflect projectiles
                projectile.setOwner(ship.getOwner());
                projectile.setFacing(projectile.getFacing() + 180f);

                // Check for collision with enemy ships
                for (ShipAPI enemy : Global.getCombatEngine().getShips()) {
                    if (enemy.getOwner() != ship.getOwner() && enemy.isAlive() && MathUtils.getDistance(projectile, enemy) < projectile.getCollisionRadius()) {
                        Global.getCombatEngine().removeEntity(projectile);
                        Global.getCombatEngine().spawnExplosion(
                                projectile.getLocation(),
                                new Vector2f(),
                                new Color(255, 180, 50), // Color of the explosion
                                projectile.getDamageAmount(),
                                0.25f // Smaller explosion for projectiles
                        );
                        break;
                    }
                }
            }
        }
        for (MissileAPI missile : Global.getCombatEngine().getMissiles()) {
            if (missile.getOwner() != ship.getOwner() && MathUtils.getDistance(ship, missile) < RADIUS) {
                // Reflect missiles
                missile.setOwner(ship.getOwner());
                missile.setFacing(missile.getFacing() + 180f);

                // Check for collision with enemy ships
                for (ShipAPI enemy : Global.getCombatEngine().getShips()) {
                    if (enemy.getOwner() != ship.getOwner() && enemy.isAlive() && MathUtils.getDistance(missile, enemy) < missile.getCollisionRadius()) {
                        Global.getCombatEngine().removeEntity(missile);
                        Global.getCombatEngine().spawnExplosion(
                                missile.getLocation(),
                                new Vector2f(),
                                new Color(255, 225, 50), // Color of the explosion
                                missile.getDamageAmount(),
                                1.10f // Larger explosion for missiles
                        );
                        break;
                    }
                }
            }
        }
    }
    private void applyFieldDamage(ShipAPI ship, float amount) {
        for (ShipAPI enemy : Global.getCombatEngine().getShips()) {
            if (enemy.getOwner() != ship.getOwner() && MathUtils.getDistance(ship, enemy) < RADIUS) {
                // Apply damage to enemy ships within the field
                Global.getCombatEngine().applyDamage(enemy, enemy.getLocation(), 50f * amount, DamageType.FRAGMENTATION, 0f, false, false, ship);
                Global.getCombatEngine().applyDamage(enemy, enemy.getLocation(), 50f * amount, DamageType.HIGH_EXPLOSIVE, 0f, false, false, ship);
            }
        }
    }
    private void applySlowingAndFluxOverload(ShipAPI ship, float amount) {
        for (ShipAPI enemy : Global.getCombatEngine().getShips()) {
            if (enemy.getOwner() != ship.getOwner() && MathUtils.getDistance(ship, enemy) < RADIUS) {
                    // Apply flux overload
                    enemy.getFluxTracker().beginOverloadWithTotalBaseDuration(1f);

                    // Apply force to hold enemy ships in place
                    Vector2f velocity = enemy.getVelocity();
                    Vector2f counterForce = new Vector2f(-velocity.x, -velocity.y);
                    enemy.getVelocity().set(0, 0); // Reset velocity to zero
                    enemy.getLocation().translate(counterForce.x * amount, counterForce.y * amount); // Apply counteracting force
                }
            }
        }
    private void createEnemyLightningStrikes(ShipAPI ship) {
        for (ShipAPI enemy : Global.getCombatEngine().getShips()) {
            if (enemy.getOwner() != ship.getOwner() && MathUtils.getDistance(ship, enemy) < RADIUS) {
                Vector2f startPoint;
                if (Math.random() < 0.5) {
                    // Random point within the field
                    float angle = (float) (Math.random() * 2 * Math.PI);
                    float distance = (float) (Math.random() * RADIUS);
                    startPoint = new Vector2f(
                            ship.getLocation().x + distance * (float) Math.cos(angle),
                            ship.getLocation().y + distance * (float) Math.sin(angle)
                    );
                } else {
                    // Point from (70, 0) relative to the ship
                    startPoint = transformRelativeToShip(ship, new Vector2f(70, 0));
                }

                Global.getCombatEngine().spawnEmpArc(ship, startPoint, enemy, enemy, DamageType.ENERGY, 0, 0, 1100f, null, 20f, NEBULA_COLOR, new Color(200,255,200,255));
            }
        }
    }
    private Vector2f transformRelativeToShip(ShipAPI ship, Vector2f relative) {
        float facing = ship.getFacing() * (float) Math.PI / 180f;
        float cos = (float) Math.cos(facing);
        float sin = (float) Math.sin(facing);
        return new Vector2f(
                ship.getLocation().x + relative.x * cos - relative.y * sin,
                ship.getLocation().y + relative.x * sin + relative.y * cos
        );
    }
    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (state == State.ACTIVE) {
            return new StatusData("HellHeaven system active", false);
        }
        return null;
    }
}
