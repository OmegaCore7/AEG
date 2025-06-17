package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class AEG_4g_protectEffect implements EveryFrameWeaponEffectPlugin {

    private int frameIndex = 0;
    private float frameDuration = 0.1f; // Duration for each frame
    private float timeSinceLastFrame = 0f;
    private boolean chargingUp = true;
    private boolean shieldActive = false;
    private IntervalUtil fluxIncreaseInterval = new IntervalUtil(2f, 2f); // Flux cost increases every 2 seconds
    private float fluxMultiplier = 1f;
    private float activationTime = 0f;
    private static final float RADIUS = 500f; // Radius for the effects
    private static final float AOE_DAMAGE = 50f; // Base AoE damage
    private static final float AOE_FLUX = 100f; // Base AoE flux increase
    private static final float MULTIPLIER = 5f; // Damage multiplier for fighters, bombers, drones, and strike craft

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        float chargeLevel = weapon.getChargeLevel();
        timeSinceLastFrame += amount;

        // Check if the weapon is selected or active
        boolean isSelected = weapon == weapon.getShip().getSelectedGroupAPI().getActiveWeapon();
        boolean isActive = weapon.isFiring() || chargeLevel > 0;

        // Switch to frame 5 when the weapon is not selected and not active
        if (!isSelected && !isActive) {
            weapon.getAnimation().setFrame(5);
            return;  // Skip the rest of the logic as we already set the frame
        }
        boolean goldionMode = Boolean.TRUE.equals(weapon.getShip().getCustomData().get("goldion_active"));
        if (timeSinceLastFrame >= frameDuration) {
            timeSinceLastFrame = 0f;

            if (chargeLevel > 0 && chargeLevel < 1) {
                // Charge up phase
                chargingUp = true;
                frameIndex = Math.min((int) (chargeLevel * 5), 4);
                //Firing Phase
            } else if (chargeLevel == 1) {
                frameIndex = 4;

                if (!goldionMode && !shieldActive) {
                    activateShield(weapon.getShip());
                    shieldActive = true;
                    activationTime = 0f;
                    fluxMultiplier = 1f;
                }
            } else if (chargeLevel < 0) {
                // Charge down phase
                chargingUp = false;
                frameIndex = Math.max((int) ((1 + chargeLevel) * 5), 0);
                shieldActive = false;
            }

            weapon.getAnimation().setFrame(frameIndex);
        }

        if (weapon.isFiring() && weapon.getShip().getSelectedGroupAPI().getActiveWeapon() == weapon) {
            if (goldionMode) {
                fireGoldionSpears(weapon.getShip(), weapon, amount);
                // Optional: deactivate shield if previously active
                if (shieldActive) {
                    deactivateShield(weapon.getShip());
                    shieldActive = false;
                }
            } else {
                // Defensive behavior
                if (!shieldActive) {
                    activateShield(weapon.getShip());
                    shieldActive = true;
                    activationTime = 0f;
                    fluxMultiplier = 1f;
                }

                activationTime += amount;
                if (activationTime > 10f) {
                    fluxIncreaseInterval.advance(amount);
                    if (fluxIncreaseInterval.intervalElapsed()) {
                        fluxMultiplier = Math.min(fluxMultiplier + 0.2f, 2f);
                        weapon.getShip().getFluxTracker().increaseFlux(weapon.getFluxCostToFire() * fluxMultiplier, true);
                    }
                }

                applyAoEDamage(weapon.getShip(), amount);
                reflectProjectilesAndMissiles(weapon.getShip());
                reduceIncomingBeamDamage(weapon.getShip());
            }
        } else {
            if (shieldActive) {
                deactivateShield(weapon.getShip());
                shieldActive = false;
            }
        }
    }

    private void activateShield(ShipAPI ship) {
        ship.getMutableStats().getShieldDamageTakenMult().modifyMult("AEG_4g_protectEffect", 0.05f); // Reduce beam damage by 95%
        ship.getShield().toggleOff(); // Deactivate shields if active
        ship.getShield().setActiveArc(0); // Prevent shields from being activated
    }

    private void deactivateShield(ShipAPI ship) {
        ship.getMutableStats().getShieldDamageTakenMult().unmodify("AEG_4g_protectEffect");
        fluxMultiplier = 1f; // Reset flux cost multiplier
    }

    private void applyAoEDamage(ShipAPI ship, float amount) {
        for (ShipAPI enemy : Global.getCombatEngine().getShips()) {
            if (enemy.getOwner() != ship.getOwner() && enemy.isAlive() && MathUtils.getDistance(ship, enemy) < RADIUS) {
                float damage = AOE_DAMAGE;
                float flux = AOE_FLUX;

                if (enemy.isFighter() || enemy.isDrone()) {
                    damage *= MULTIPLIER;
                    flux *= MULTIPLIER;
                }

                enemy.getFluxTracker().increaseFlux(flux * amount, true);
                enemy.setHitpoints(enemy.getHitpoints() - damage * amount);
            }
        }
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

    private void reduceIncomingBeamDamage(ShipAPI ship) {
        for (BeamAPI beam : Global.getCombatEngine().getBeams()) {
            if (beam.getSource() != ship && MathUtils.getDistance(ship, beam.getTo()) < RADIUS) {
                beam.getDamage().setDamage(beam.getDamage().getDamage() * 0.05f); // Reduce beam damage by 95%
            }
        }
    }
    private IntervalUtil spearInterval = new IntervalUtil(0.2f, 0.2f);  // Starts slow
    private float spearRampTimer = 0f;

    private void fireGoldionSpears(ShipAPI ship, WeaponAPI weapon, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f weaponLoc = weapon.getLocation();
        float angle = weapon.getCurrAngle();

        spearRampTimer += amount;
        spearInterval.advance(amount);

        // Ramp up fire rate
        float decay = Math.min(spearRampTimer / 5f, 1f);  // Up to 5s to reach max rate
        float interval = 0.2f - (0.15f * decay);
        spearInterval.setInterval(interval, interval);

        if (spearInterval.intervalElapsed()) {
            // Calculate spear direction
            Vector2f dir = new Vector2f((float) Math.cos(Math.toRadians(angle)), (float) Math.sin(Math.toRadians(angle)));
            Vector2f speed = new Vector2f(dir);
            speed.scale(300f + decay * 500f);  // Ramp up speed

            // Optional: add curve or flicker
            float spread = (1f - decay) * 10f;  // Less wobble at full ramp
            angle += MathUtils.getRandomNumberInRange(-spread, spread);
            dir = MathUtils.getPointOnCircumference(null, 1f, angle);
            Vector2f newSpeed = new Vector2f(dir);
            newSpeed.scale(300f + decay * 500f);

            Vector2f spawnLoc = MathUtils.getPointOnCircumference(weaponLoc, 10f, angle);

            // Main particle spear
            engine.addSmoothParticle(spawnLoc, newSpeed, 12f, 1.8f, 0.5f,
                    new Color(255, 225, 100, 255));
            engine.addNebulaParticle(spawnLoc, newSpeed, 18f, 2.0f, 0.1f, 0.2f, 0.6f,
                    new Color(255, 255, 180, 200));

            // EMP Spark every 5th shot
            if ((int) (spearRampTimer * 10) % 5 == 0) {
                Vector2f empLoc = MathUtils.getRandomPointInCircle(spawnLoc, 30f);
                engine.spawnEmpArcVisual(empLoc, ship, empLoc, null, 10f,
                        new Color(255, 255, 100), new Color(255, 180, 50));
            }

            // Impact sparkle at random for visual feedback
            if (Math.random() < 0.2) {
                engine.addHitParticle(spawnLoc, newSpeed, 10f, 1.5f, 0.25f,
                        new Color(255, 230, 130));
            }
        }
        if (!weapon.isFiring()) {
            spearRampTimer = 0f;
        }
    }

}
