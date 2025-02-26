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

        if (timeSinceLastFrame >= frameDuration) {
            timeSinceLastFrame = 0f;

            if (chargeLevel > 0 && chargeLevel < 1) {
                // Charge up phase
                chargingUp = true;
                frameIndex = Math.min((int) (chargeLevel * 5), 4);
            } else if (chargeLevel == 1) {
                // Firing phase
                frameIndex = 4;
                if (!shieldActive) {
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

        if (shieldActive && weapon.isFiring() && weapon.getShip().getSelectedGroupAPI().getActiveWeapon() == weapon) {
            activationTime += amount;
            if (activationTime > 10f) {
                fluxIncreaseInterval.advance(amount);
                if (fluxIncreaseInterval.intervalElapsed()) {
                    fluxMultiplier = Math.min(fluxMultiplier + 0.2f, 2f);
                    weapon.getShip().getFluxTracker().increaseFlux(weapon.getFluxCostToFire() * fluxMultiplier, true);
                }
            }

            // Apply AoE damage to nearby enemy ships
            applyAoEDamage(weapon.getShip(), amount);

            // Reflect projectiles and missiles
            reflectProjectilesAndMissiles(weapon.getShip());

            // Reduce incoming beam damage
            reduceIncomingBeamDamage(weapon.getShip());
        } else {
            deactivateShield(weapon.getShip());
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
}
