package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import data.shipsystems.helpers.JitterEffectManager;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class AEG_SteelBarrage extends BaseShipSystemScript {

    private static final float RAM_RADIUS = 1000f;
    private static final float RAM_FORCE = 450f;
    private static final float RAM_DAMAGE = 2000f;
    private static final float COLLISION_THRESHOLD = 75f;
    private static final int RAM_COUNT = 5;
    private static final float PAUSE_DURATION = 0.4f;
    private static final Color LIGHT_GREEN_COLOR = new Color(144, 238, 144, 255);
    private static final Color EXPLOSION_COLOR = new Color(175, 220, 120, 255);

    private int ramCounter = 0;
    private float pauseTimer = 0f;
    private ShipAPI targetShip = null;
    private int maneuverStep = 0;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        if (state == State.ACTIVE) {
            handleActiveState(ship, id, effectLevel);
        }

        addJitterCopies(ship);
    }

    private void handleActiveState(ShipAPI ship, String id, float effectLevel) {
        if (ramCounter == 0) {
            targetShip = findClosestTarget(ship);
            if (targetShip != null) {
                ramCounter = RAM_COUNT;
            }
        }

        if (targetShip != null && ramCounter > 0) {
            if (pauseTimer <= 0f) {
                if (maneuverStep == 0) {
                    createHitspark(ship, targetShip);
                }

                performManeuvers(ship, targetShip);

                if (maneuverStep >= 5) {
                    applyRammingForceAndDamage(ship, targetShip, id, effectLevel);
                    createExplosionOrShieldHit(ship, targetShip);

                    resetManeuver();
                } else {
                    incrementManeuverStep();
                }
            } else {
                pauseTimer -= Global.getCombatEngine().getElapsedInLastFrame();
            }
        }
    }

    private void resetManeuver() {
        maneuverStep = 0;
        pauseTimer = PAUSE_DURATION;
        ramCounter--;
    }

    private void incrementManeuverStep() {
        maneuverStep++;
        pauseTimer = PAUSE_DURATION / 2f;
    }

    private ShipAPI findClosestTarget(ShipAPI ship) {
        ShipAPI closestTarget = null;
        float closestDistance = Float.MAX_VALUE;
        for (ShipAPI target : Global.getCombatEngine().getShips()) {
            if (target.getOwner() != ship.getOwner()) {
                float distance = Vector2f.sub(target.getLocation(), ship.getLocation(), null).length();
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestTarget = target;
                }
            }
        }
        return closestTarget;
    }

    private void createHitspark(ShipAPI ship, ShipAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f arcLocation = target.getLocation();

        // Calculate dynamic size based on target ship size
        float maxRange = target.getCollisionRadius();
        float randomRange = (float) (Math.random() * maxRange);
        float randomThickness = (float) (Math.random() * 10 + 5); // Random thickness between 5 and 15

        engine.spawnEmpArc(
                ship, ship.getLocation(), ship, target,
                DamageType.ENERGY,
                0f,
                0f,
                randomRange,
                "tachyon_lance_emp_impact",
                randomThickness,
                LIGHT_GREEN_COLOR,
                LIGHT_GREEN_COLOR
        );
    }

    private void performManeuvers(ShipAPI ship, ShipAPI target) {
        Vector2f direction = Vector2f.sub(target.getLocation(), ship.getLocation(), null);
        direction.normalise();
        Vector2f perpendicular = new Vector2f(-direction.y, direction.x);

        // Calculate dynamic scale based on ship size
        float shipSizeFactor = ship.getCollisionRadius() / 100f; // Adjust the divisor as needed for scaling

        switch (maneuverStep) {
            case 0:
                perpendicular.scale(500f * shipSizeFactor);
                Vector2f.add(ship.getVelocity(), perpendicular, ship.getVelocity());
                break;
            case 1:
                Vector2f drift = new Vector2f(direction);
                drift.scale(200f * shipSizeFactor);
                Vector2f.add(ship.getVelocity(), drift, ship.getVelocity());
                break;
            case 2:
                Vector2f curve = new Vector2f(direction);
                curve.scale(300f * shipSizeFactor);
                Vector2f.add(ship.getVelocity(), curve, ship.getVelocity());
                break;
            case 3:
                perpendicular.scale(-500f * shipSizeFactor);
                Vector2f.add(ship.getVelocity(), perpendicular, ship.getVelocity());
                break;
            case 4:
                Vector2f reverseDrift = new Vector2f(direction);
                reverseDrift.scale(-200f * shipSizeFactor);
                Vector2f.add(ship.getVelocity(), reverseDrift, ship.getVelocity());
                break;
        }

        float randomRotation = (float) (Math.random() * 180 - 90);
        ship.setFacing(ship.getFacing() + randomRotation);

        Vector2f currentVelocity = ship.getVelocity();
        Vector2f targetDirection = Vector2f.sub(target.getLocation(), ship.getLocation(), null);
        targetDirection.normalise();
        targetDirection.scale(currentVelocity.length());
        ship.getVelocity().set(targetDirection);
    }

    private void applyRammingForceAndDamage(ShipAPI ship, ShipAPI target, String id, float effectLevel) {
        Vector2f diff = Vector2f.sub(target.getLocation(), ship.getLocation(), null);
        diff.normalise();
        diff.scale(RAM_FORCE * effectLevel);
        ship.getVelocity().set(diff);

        float distance = Vector2f.sub(target.getLocation(), ship.getLocation(), null).length();
        if (distance <= COLLISION_THRESHOLD) {
            if (target.getShield() != null && target.getShield().isOn()) {
                target.getFluxTracker().increaseFlux(RAM_DAMAGE * 2f * effectLevel, true);
            } else {
                float armorValue = target.getArmorGrid().getArmorRating() * target.getArmorGrid().getMaxArmorInCell();
                if (armorValue > 0) {
                    // Apply a minimum damage to ensure armor is reduced
                    float effectiveDamage = RAM_DAMAGE * effectLevel * (1f - armorValue / (armorValue + RAM_DAMAGE));
                    float minDamage = RAM_DAMAGE * 0.1f * effectLevel; // Minimum damage is 10% of RAM_DAMAGE
                    effectiveDamage = Math.max(effectiveDamage, minDamage);

                    int x = (int) target.getLocation().x;
                    int y = (int) target.getLocation().y;
                    target.getArmorGrid().setArmorValue(x, y, Math.max(0f, armorValue - effectiveDamage));
                } else {
                    target.getMutableStats().getHullDamageTakenMult().modifyMult(id, 1f + (RAM_DAMAGE * effectLevel / target.getMaxHitpoints()));
                }
            }
        }
    }


    private void createExplosionOrShieldHit(ShipAPI ship, ShipAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (target.getShield() != null && target.getShield().isOn()) {
            // Render a massive shield hit recoil effect
            Vector2f shieldHitLocation = target.getShield().getLocation();
            float shieldHitRadius = target.getShield().getRadius();
            engine.spawnExplosion(shieldHitLocation, target.getVelocity(), LIGHT_GREEN_COLOR, shieldHitRadius, 1f);
            engine.addHitParticle(shieldHitLocation, target.getVelocity(), shieldHitRadius * 1.5f, 1f, 0.25f, LIGHT_GREEN_COLOR);
            engine.addHitParticle(shieldHitLocation, target.getVelocity(), shieldHitRadius * 2f, 1f, 0.1f, Color.WHITE);
        } else {
            // Render a normal explosion effect
            engine.spawnExplosion(target.getLocation(), target.getVelocity(), EXPLOSION_COLOR, 300f, 2f);
        }
    }

    private void addJitterCopies(ShipAPI ship) {
        Color jitterColor = new Color(144, 238, 144, 255);
        float jitterDuration = 5.0f;
        float jitterRange = 5.0f;

        for (int i = 0; i < 10; i++) {
            JitterEffectManager.addJitterCopy(ship, jitterColor, jitterDuration, jitterRange);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        // No time dilation to unapply, so this method can remain empty or handle other cleanup if needed
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("True Destruction Steel Fist Barrage!", false);
        }
        return null;
    }
}
