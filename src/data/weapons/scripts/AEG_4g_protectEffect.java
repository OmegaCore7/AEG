package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_4g_protectEffect implements EveryFrameWeaponEffectPlugin {
    private final IntervalUtil effectVisualInterval = new IntervalUtil(0.15f, 0.25f);
    private int frameIndex = 0;
    private float frameDuration = 0.1f; // Duration for each frame
    private float timeSinceLastFrame = 0f;
    private boolean chargingUp = true;
    private boolean shieldActive = false;
    private IntervalUtil fluxIncreaseInterval = new IntervalUtil(2f, 2f); // Flux cost increases every 2 seconds
    private float fluxMultiplier = 1f;
    private float activationTime = 0f;
    private static final float RADIUS = 350f; // Radius for the effects
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
        CombatEngineAPI engine = Global.getCombatEngine();
        boolean isGoldion = Boolean.TRUE.equals(ship.getCustomData().get("goldion_active"));

        float radius = isGoldion ? 500f : 350f;
        float baseDamage = AOE_DAMAGE;
        float baseFlux = AOE_FLUX;

        for (ShipAPI enemy : engine.getShips()) {
            if (enemy.getOwner() == ship.getOwner() || !enemy.isAlive()) continue;
            if (MathUtils.getDistance(ship, enemy) > radius) continue;

            float damage = baseDamage;
            float flux = baseFlux;

            if (enemy.isFighter() || enemy.isDrone()) {
                damage *= MULTIPLIER;
                flux *= MULTIPLIER;
            } else if (isGoldion) {
                switch (enemy.getHullSize()) {
                    case FIGHTER:
                    case FRIGATE:
                        damage *= 3f;
                        flux *= 3f;
                        break;
                    case DESTROYER:
                        damage *= 2f;
                        flux *= 2f;
                        break;
                    case CRUISER:
                        damage *= 1.5f;
                        flux *= 1.5f;
                        break;
                    default:
                        break; // Capitals get normal damage
                }
            }

            enemy.getFluxTracker().increaseFlux(flux * amount, true);
            enemy.setHitpoints(enemy.getHitpoints() - damage * amount);

            // Add visual indicator
            effectVisualInterval.advance(amount); //Time interval for performance
            if (effectVisualInterval.intervalElapsed()) {
                spawnHitEffect(enemy.getLocation(), isGoldion, engine);
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

    private void spawnHitEffect(Vector2f location, boolean isGoldion, CombatEngineAPI engine) {
        int totalParticles = isGoldion ? 4 : 2;
        int centerParticles = Math.max(1, totalParticles / 3); // ~1/3 at center
        int offsetParticles = totalParticles - centerParticles;

        float baseSize = isGoldion ? 35f : 20f;
        float duration = isGoldion ? 0.5f : 0.25f;
        float spawnRadius = isGoldion ? 60f : 30f;

        // --- Centered particles
        for (int i = 0; i < centerParticles; i++) {
            float size = baseSize * MathUtils.getRandomNumberInRange(0.8f, 1.2f);
            Vector2f velocity = MathUtils.getPoint(new Vector2f(), MathUtils.getRandomNumberInRange(10f, 30f), (float)(Math.random() * 360f));
            Color color = getParticleColor(isGoldion);

            engine.spawnExplosion(location, new Vector2f(), color, size, duration);
            engine.addSmoothParticle(location, velocity, size * 0.5f, 1.0f, duration, color);
        }

        // --- Offset particles
        for (int i = 0; i < offsetParticles; i++) {
            float angle = (float) (Math.random() * 360f);
            float dist = MathUtils.getRandomNumberInRange(spawnRadius * 0.4f, spawnRadius);

            Vector2f offset = MathUtils.getPointOnCircumference(location, dist, angle);
            Vector2f velocity = MathUtils.getPoint(new Vector2f(), MathUtils.getRandomNumberInRange(15f, 40f), (float)(Math.random() * 360f));
            float size = baseSize * MathUtils.getRandomNumberInRange(0.7f, 1.3f);
            Color color = getParticleColor(isGoldion);

            engine.spawnExplosion(offset, new Vector2f(), color, size, duration);
            engine.addSmoothParticle(offset, velocity, size * 0.5f, 1.0f, duration, color);
        }

        // --- Optional: Goldion aura
        if (isGoldion) {
            for (int i = 0; i < 2; i++) {
                Vector2f offset = MathUtils.getPointOnCircumference(location, MathUtils.getRandomNumberInRange(40f, 80f), (float) Math.random() * 360f);
                Vector2f auraVelocity = MathUtils.getPoint(new Vector2f(), MathUtils.getRandomNumberInRange(5f, 15f), (float)(Math.random() * 360f));

                float auraSize = MathUtils.getRandomNumberInRange(40f, 80f);
                float endSizeMult = 0.9f + MathUtils.getRandom().nextFloat() * 0.4f;
                float fullBrightnessFraction = 0.5f + MathUtils.getRandom().nextFloat() * 0.4f;
                float auraDuration = MathUtils.getRandomNumberInRange(0.6f, 1.0f);

                Color auraColor = new Color(
                        255 - MathUtils.getRandom().nextInt(50),
                        210 + MathUtils.getRandom().nextInt(30),
                        80 + MathUtils.getRandom().nextInt(40),
                        MathUtils.getRandomNumberInRange(75, 175)
                );

                engine.addNebulaParticle(
                        offset,
                        auraVelocity,
                        auraSize,
                        endSizeMult,
                        0.5f,
                        fullBrightnessFraction,
                        auraDuration,
                        auraColor
                );
            }
        }
    }

    private Color getParticleColor(boolean isGoldion) {
        return isGoldion
                ? new Color(255, 200 + MathUtils.getRandom().nextInt(55), 60 + MathUtils.getRandom().nextInt(30), 150 + MathUtils.getRandom().nextInt(55))
                : new Color(255, 100 + MathUtils.getRandom().nextInt(50), 50, 120 + MathUtils.getRandom().nextInt(50));
    }
}
