package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_right_kneedrill implements EveryFrameWeaponEffectPlugin {

    private static final float PUSH_FORCE = 10f;
    private static final float SPEED_BUFF = 1.2f;
    private static final float MANEUVER_BUFF = 1.5f;
    private static final String BUFF_ID = "AEG_kneedrill_buff";

    private static final int FRAME_INVISIBLE = 0;
    private static final int FRAME_MAX = 6;

    private int currentFrame = FRAME_INVISIBLE;
    private boolean coolingDown = false;
    private boolean wasFiring = false;

    private static final String GOLDION_ACTIVE_KEY = "goldion_active";

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon == null || engine == null) return;

        ShipAPI ship = weapon.getShip();
        if (ship == null) return;

        boolean isFiring = weapon.isFiring();

        // Check if Goldion Armor mode is active
        boolean goldionActive = Boolean.TRUE.equals(ship.getCustomData().get(GOLDION_ACTIVE_KEY));


        if (isFiring) {
            wasFiring = true;
            if (currentFrame < FRAME_MAX) {
                currentFrame++;
            }

            coolingDown = false;
            applyEffects(ship);
            forceEngineBoost(ship, true);

            // 🔥 Only spawn and apply drill when it's fully extended
            if (goldionActive && currentFrame == FRAME_MAX) {
                spawnEnergyDrillEffect(weapon, engine);
                applyDrillDamage(ship, weapon, engine);
            }
        } else if (wasFiring) {
            if (currentFrame > FRAME_INVISIBLE) {
                currentFrame--;
                coolingDown = true;
            } else {
                wasFiring = false;
                coolingDown = false;
            }
            forceEngineBoost(ship, false);
        }

        weapon.getAnimation().setFrame(currentFrame);
    }

    private void applyEffects(ShipAPI ship) {
        ship.getMutableStats().getMaxSpeed().modifyMult(BUFF_ID, SPEED_BUFF);
        ship.getMutableStats().getTurnAcceleration().modifyMult(BUFF_ID, MANEUVER_BUFF);
        ship.getMutableStats().getMaxTurnRate().modifyMult(BUFF_ID, MANEUVER_BUFF);
        applyForwardPush(ship);
    }

    private void applyForwardPush(ShipAPI ship) {
        float angle = (float) Math.toRadians(ship.getFacing());
        float pushX = (float) Math.cos(angle) * PUSH_FORCE;
        float pushY = (float) Math.sin(angle) * PUSH_FORCE;
        Vector2f velocity = ship.getVelocity();
        velocity.x += pushX;
        velocity.y += pushY;
    }

    private void forceEngineBoost(ShipAPI ship, boolean activate) {
        if (ship.getEngineController() != null) {
            if (activate) {
                ship.getEngineController().extendFlame(this, 0.5f, 2.0f, 2.0f);
            } else {
                ship.getEngineController().extendFlame(this, 0.0f, 1.0f, 1.0f);
            }
        }
    }

    private void spawnEnergyDrillEffect(WeaponAPI weapon, CombatEngineAPI engine) {
        Vector2f baseCenter = weapon.getFirePoint(0);  // this is the correct firing offset
        float angle = weapon.getCurrAngle();

        float drillLength = 50f; // longer drill
        // Dynamic drill width oscillation
        float baseWidth = 28f;
        float amplitude = 10f; // how much it widens and thins
        float maxSpeed = 200f;

        Vector2f shipVel = weapon.getShip().getVelocity();

// Get forward velocity component
        float angleRad = (float) Math.toRadians(angle);
        Vector2f forwardUnit = new Vector2f((float) Math.cos(angleRad), (float) Math.sin(angleRad));
        float forwardSpeed = Vector2f.dot(shipVel, forwardUnit);
        float speedFactor = Math.min(Math.max(forwardSpeed / maxSpeed, 0f), 1f); // clamp 0–1

// Control pulse frequency based on forward movement
        float time = engine.getTotalElapsedTime(false);
        float pulseRate = 2f + 4f * speedFactor; // in Hz

// Sinusoidal pulsing width
        float width = baseWidth + (float)Math.sin(time * pulseRate * Math.PI * 2f) * amplitude;
        // wider base

        // Triangle points: tip in front, base sides behind
        Vector2f tip = MathUtils.getPointOnCircumference(baseCenter, drillLength, angle);
        Vector2f left = MathUtils.getPointOnCircumference(baseCenter, width / 2f, angle - 90f);
        Vector2f right = MathUtils.getPointOnCircumference(baseCenter, width / 2f, angle + 90f);

        // Outline with hit particles
        float spacing = 1f;
        drawLineParticles(engine, tip, left, spacing);
        drawLineParticles(engine, tip, right, spacing);
        drawLineParticles(engine, left, right, spacing);

        // Swirling energy center
        spawnDrillSwirlParticles(engine, baseCenter, tip, left, right);
    }

    private void drawLineParticles(CombatEngineAPI engine, Vector2f from, Vector2f to, float spacing) {
        float dist = MathUtils.getDistance(from, to);
        Vector2f dir = Vector2f.sub(to, from, null);
        dir.normalise();

        for (float i = 0; i <= dist; i += spacing) {
            Vector2f point = new Vector2f(from.x + dir.x * i, from.y + dir.y * i);
            Color color = new Color(255, 220, 50, 200); // Gold line
            engine.addHitParticle(point, new Vector2f(), 2f, 1f, 0.15f, color);
        }
    }

    private void spawnDrillSwirlParticles(CombatEngineAPI engine, Vector2f base, Vector2f tip, Vector2f left, Vector2f right) {
        float swirlCount = 8;
        float swirlRange = 0.7f;
        float angle = VectorUtils.getAngle(base, tip);
        float length = MathUtils.getDistance(base, tip);

        for (int i = 0; i < swirlCount; i++) {
            float t = (float) i / swirlCount;
            float radius = (1f - t) * 30f; // taper effect
            float theta = (engine.getTotalElapsedTime(false) * 300f + i * 45f) % 360f; // rotation animation

            Vector2f center = MathUtils.getPointOnCircumference(base, t * length, angle);
            Vector2f swirl = MathUtils.getPointOnCircumference(center, radius * swirlRange, theta);

            engine.addNebulaParticle(
                    swirl,
                    new Vector2f(),
                    MathUtils.getRandomNumberInRange(10f, 20f),
                    1.8f,
                    0.2f,
                    0.5f,
                    1f,
                    new Color(255, 230, 150, 180)
            );
        }
    }
    private void applyDrillDamage(ShipAPI ship, WeaponAPI weapon, CombatEngineAPI engine) {
        Vector2f origin = weapon.getFirePoint(0);
        float angle = weapon.getCurrAngle();
        float coneLength = 160f;
        float coneWidth = 60f;

        float totalDamagePerSecond = 400f;
        float empPerSecond = 50f;

        float damage = totalDamagePerSecond * engine.getElapsedInLastFrame(); // per-frame
        float emp = empPerSecond * engine.getElapsedInLastFrame();

        for (ShipAPI target : engine.getShips()) {
            if (target == ship || target.isHulk() || target.isShuttlePod() || target.getOwner() == ship.getOwner()) continue;

            float dist = MathUtils.getDistance(origin, target.getLocation());
            if (dist > coneLength + target.getCollisionRadius()) continue;

            float angleTo = VectorUtils.getAngle(origin, target.getLocation());
            float angleDiff = Math.abs(MathUtils.getShortestRotation(angle, angleTo));
            if (angleDiff <= coneWidth / 2f) {
                // Apply half energy, half high explosive
                engine.applyDamage(target, target.getLocation(), damage * 0.5f, DamageType.ENERGY, emp * 0.5f, false, false, ship);
                engine.applyDamage(target, target.getLocation(), damage * 0.5f, DamageType.HIGH_EXPLOSIVE, 0f, false, false, ship);

                // Impact flash at target
                engine.spawnExplosion(
                        target.getLocation(),
                        new Vector2f(),
                        new Color(255, 180, 50, 255),
                        25f,
                        0.2f
                );
            }
        }
    }

}
