package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;

public class AEG_4g_protectEffect implements EveryFrameWeaponEffectPlugin {
    //Fake Mini Gun Beam Generation Parameters
    private boolean goldionModeActive = false;
    private boolean goldionFiring = false;
    private float timeSinceLastShot = 0f;
    private float firingInterval = 0.2f; // start slow
    private float firingRampRate = 0.005f; // how fast it speeds up
    private float minFiringInterval = 0.03f; // fastest possible
    private int beamShotsFired = 0;
    private static final int MAX_BEAMS = 100;
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
        ShipAPI ship = weapon.getShip();
        if (ship == null || weapon == null) return;

// Check if Goldion Armor is active
        goldionModeActive = Boolean.TRUE.equals(ship.getCustomData().get("goldion_active"));
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

        if (goldionModeActive && weapon.isFiring()) {
            goldionFiring = true;
            timeSinceLastShot += amount;

            if (beamShotsFired < MAX_BEAMS && timeSinceLastShot >= firingInterval) {
                fireMinigunBeam(ship, weapon);
                beamShotsFired++;
                timeSinceLastShot = 0f;

                // Ramp up the firing speed
                firingInterval = Math.max(minFiringInterval, firingInterval - firingRampRate);
            }

        } else {
            // Reset when not firing or mode ends
            goldionFiring = false;
            timeSinceLastShot = 0f;
            firingInterval = 0.2f;
            beamShotsFired = 0;
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
    private void fireMinigunBeam(ShipAPI ship, WeaponAPI weapon) {
        CombatEngineAPI engine = Global.getCombatEngine();

        Vector2f firePoint = weapon.getLocation();
        float angle = weapon.getCurrAngle();
        float spread = MathUtils.getRandomNumberInRange(-4f, 4f); // wider spread for chaotic beam storm
        float facing = angle + spread;

        // Length and direction of the beam
        float beamLength = 600f;
        Vector2f beamDir = MathUtils.getPointOnCircumference(null, beamLength, facing);
        Vector2f beamTarget = Vector2f.add(firePoint, beamDir, null);

        // Damage applied in a line (single point for now)
        applyBeamDamageAlongPath(ship, firePoint, beamTarget, 100f);

        // Visual beam flash - very fast fade
        engine.addHitParticle(
                firePoint,
                new Vector2f(),
                80f,
                1.0f,
                0.05f,
                new Color(255, 180, 80, 255)
        );

        // Flash at the beam tip
        engine.spawnExplosion(
                beamTarget,
                new Vector2f(),
                new Color(255, 100, 30, 200),
                40f,
                0.2f
        );

        // Optional: lingering glow trail
        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "beam_light_400"),
                Vector2f.add(firePoint, beamDir, null),
                new Vector2f(),
                new Vector2f(200f, 40f),
                new Vector2f(250f, 60f),
                angle,
                0f,
                new Color(255, 200, 80, 180),
                true,
                0.05f,
                0f,
                0.2f
        );

        // Optional: play sound
        Global.getSoundPlayer().playSound("tachyon_lance_emp_impact", 1f, 0.6f, firePoint, ship.getVelocity());
    }
    private void applyBeamDamageAlongPath(ShipAPI ship, Vector2f from, Vector2f to, float damageAmount) {
        CombatEngineAPI engine = Global.getCombatEngine();

        for (ShipAPI target : engine.getShips()) {
            if (target.getOwner() != ship.getOwner() && target.isAlive()) {
                // Beam hits if close to line
                if (MathUtils.getDistance(target.getLocation(), to) < 75f) {
                    engine.applyDamage(
                            target,
                            target.getLocation(),
                            damageAmount,
                            DamageType.ENERGY,
                            0f,
                            false,
                            false,
                            ship
                    );
                }
            }
        }
    }
}