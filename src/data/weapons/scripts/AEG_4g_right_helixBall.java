package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class AEG_4g_right_helixBall implements EveryFrameCombatPlugin {
    private final ShipAPI source;
    private final CombatEngineAPI engine;

    private final float SPEED = 350f;
    private final float MAX_DURATION = 10f;
    private final float DAMAGE = 1000f;
    private final float EXPLOSION_RADIUS = 100f;
    private final int ORB_COUNT = 20;

    private float timer = 0f;
    private boolean exploded = false;

    // Spiral params
    private final float spiralRadius = 75f;
    private final float spiralSpeed = 6f;

    // Homing params
    private final float HOMING_TURN_RATE = 90f; // degrees per second
    private final float HOMING_RANGE = 1000f;

    private final Vector2f corePosition;
    private Vector2f coreVelocity;

    public AEG_4g_right_helixBall(ShipAPI source, Vector2f spawnLoc, float angle, CombatEngineAPI engine) {
        this.source = source;
        this.engine = engine;

        this.corePosition = new Vector2f(spawnLoc);
        this.coreVelocity = MathUtils.getPoint(new Vector2f(), SPEED, angle);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine.isPaused() || exploded || source == null || !source.isAlive()) return;

        boolean isGoldionActive = Boolean.TRUE.equals(source.getCustomData().get("goldion_active"));
        if (!isGoldionActive) return;

        timer += amount;
        if (timer >= MAX_DURATION) {
            explode();
            return;
        }

        // HOMING: Find nearest enemy within range
        CombatEntityAPI target = getNearestEnemy(engine, source, corePosition, HOMING_RANGE);
        if (target != null) {
            steerTowards(target.getLocation(), amount);
        }

        // Move core forward
        Vector2f forwardStep = new Vector2f(coreVelocity);
        forwardStep.scale(amount);
        Vector2f.add(corePosition, forwardStep, corePosition);

        // Check impact
        CombatEntityAPI impactTarget = getNearestEnemy(engine, source, corePosition, EXPLOSION_RADIUS);
        if (impactTarget != null) {
            explode(impactTarget);
            return;
        }
        renderCoreBall();
        renderOrbitingPlasma();
        renderNebulaTrail();

    }

    private void steerTowards(Vector2f targetLoc, float amount) {
        Vector2f desiredDir = Vector2f.sub(targetLoc, corePosition, null);
        desiredDir.normalise();

        Vector2f currentDir = new Vector2f(coreVelocity);
        currentDir.normalise();

        // Calculate angle difference in degrees
        float currentAngle = (float) Math.toDegrees(Math.atan2(currentDir.y, currentDir.x));
        float desiredAngle = (float) Math.toDegrees(Math.atan2(desiredDir.y, desiredDir.x));

        float angleDiff = MathUtils.getShortestRotation(currentAngle, desiredAngle);

        // Limit turn rate by HOMING_TURN_RATE * amount
        float maxTurn = HOMING_TURN_RATE * amount;
        float turnAmount = Math.abs(angleDiff) < maxTurn ? angleDiff : Math.copySign(maxTurn, angleDiff);

        float newAngle = currentAngle + turnAmount;

        // Update coreVelocity vector with new angle, maintain SPEED
        coreVelocity = MathUtils.getPoint(new Vector2f(), SPEED, newAngle);
    }

    private void renderOrbs() {
        Color gold = new Color(255, 230, 100, 220);
        Color orange = new Color(255, 150, 50, 200);
        Color red = new Color(255, 60, 60, 180);

        for (int i = 0; i < ORB_COUNT; i++) {
            float angle = (float) (Math.random() * Math.PI * 2f);
            float radius = MathUtils.getRandomNumberInRange(40f, 150f);

            Vector2f offset = MathUtils.getPoint(new Vector2f(), radius, angle);
            Vector2f orbLocation = Vector2f.add(corePosition, offset, null);

            // Velocity: either swirling or randomly bursting outward
            Vector2f velocity = MathUtils.getRandomPointInCircle(null, MathUtils.getRandomNumberInRange(50f, 200f));
            if (Math.random() < 0.3f) {
                // Some are pulled inward a bit for contrast
                velocity = Vector2f.sub(corePosition, orbLocation, null);
                velocity.normalise();
                velocity.scale(MathUtils.getRandomNumberInRange(40f, 80f));
            }

            float size = MathUtils.getRandomNumberInRange(30f, 60f);
            float duration = MathUtils.getRandomNumberInRange(0.5f, 1.5f);

            // Pick chaotic bright color
            Color color = Math.random() < 0.4
                    ? gold
                    : (Math.random() < 0.7 ? orange : red);

            engine.addNebulaParticle(
                    orbLocation,
                    velocity,
                    size,
                    2f,
                    0.1f,
                    0.2f,
                    duration,
                    color
            );

            if (Math.random() < 0.5f) {
                // Occasional smoother particle for pop
                engine.addSmoothParticle(
                        orbLocation,
                        velocity,
                        size * 0.5f,
                        1.5f,
                        0.3f,
                        new Color(255, 255, 180, 255)
                );
            }
        }
    }

    private void explode() {
        exploded = true;
        spawnExplosionFX();
        engine.removePlugin(this);
    }

    private void explode(CombatEntityAPI target) {
        exploded = true;
        spawnExplosionFX();
        engine.applyDamage(
                target,
                corePosition,
                DAMAGE,
                DamageType.ENERGY,
                50f,
                true,
                true,
                source
        );
        engine.removePlugin(this);
    }

    private void spawnExplosionFX() {
        engine.spawnExplosion(corePosition, new Vector2f(), new Color(255, 235, 80), 150f, 1.5f);
        engine.addSmoothParticle(corePosition, new Vector2f(), 200f, 1.8f, 1.2f, new Color(255, 255, 200, 255));
        engine.addHitParticle(corePosition, new Vector2f(), 120f, 1f, 0.5f, Color.white);

        for (int i = 0; i < 24; i++) {
            Vector2f randVel = MathUtils.getRandomPointInCircle(null, 300f);
            engine.addNebulaParticle(
                    corePosition,
                    randVel,
                    MathUtils.getRandomNumberInRange(20f, 40f),
                    2f,
                    0.1f,
                    0.3f,
                    MathUtils.getRandomNumberInRange(1.5f, 3f),
                    new Color(255, 240, 130, 200)
            );
        }
    }
    private void renderCoreBall() {
        // Central glowing plasma ball
        engine.addSmoothParticle(
                corePosition,
                new Vector2f(),
                80f,
                2.5f,
                0.2f,
                new Color(255, 245, 180, 255)
        );
        engine.addSmoothParticle(
                corePosition,
                new Vector2f(),
                120f,
                1.8f,
                0.1f,
                new Color(255, 200, 50, 200)
        );
    }
    private void renderOrbitingPlasma() {
        for (int i = 0; i < ORB_COUNT; i++) {
            float phase = (float) ((Math.PI * 2f) * ((float) i / ORB_COUNT));
            float chaoticOffset = (float) Math.sin(timer * 3f + i) * 0.5f;

            float spiralAngle = spiralSpeed * timer + phase + chaoticOffset;
            float dynamicRadius = spiralRadius + MathUtils.getRandomNumberInRange(-10f, 15f); // slightly unstable orbit

            float offsetX = (float) Math.cos(spiralAngle) * dynamicRadius;
            float offsetY = (float) Math.sin(spiralAngle) * dynamicRadius;

            Vector2f velocityDir = new Vector2f(coreVelocity);
            velocityDir.normalise();
            Vector2f perpendicular = new Vector2f(-velocityDir.y, velocityDir.x);

            Vector2f spiralOffset = new Vector2f(perpendicular);
            spiralOffset.scale(offsetX);

            Vector2f forwardOffset = new Vector2f(velocityDir);
            forwardOffset.scale(offsetY);

            Vector2f orbLocation = new Vector2f(corePosition);
            Vector2f.add(orbLocation, spiralOffset, orbLocation);
            Vector2f.add(orbLocation, forwardOffset, orbLocation);

            Vector2f randMotion = MathUtils.getRandomPointInCircle(null, 40f);

            engine.addSmoothParticle(
                    orbLocation,
                    randMotion,
                    MathUtils.getRandomNumberInRange(20f, 40f),
                    1.2f,
                    0.2f,
                    new Color(255, 200 + MathUtils.getRandom().nextInt(50), 50 + MathUtils.getRandom().nextInt(50), 220)
            );
        }
    }
    private void renderNebulaTrail() {
        Vector2f trailPoint = new Vector2f(corePosition);
        Vector2f behind = new Vector2f(coreVelocity);
        behind.normalise();
        behind.scale(-40f);
        Vector2f.add(trailPoint, behind, trailPoint);

        engine.addNebulaParticle(
                trailPoint,
                new Vector2f(),
                MathUtils.getRandomNumberInRange(30f, 60f),
                1.8f,
                0.2f,
                0.3f,
                1.5f,
                new Color(255, 240, 120, 140)
        );
    }
    private CombatEntityAPI getNearestEnemy(CombatEngineAPI engine, ShipAPI source, Vector2f loc, float range) {
        CombatEntityAPI nearest = null;
        float nearestDist = Float.MAX_VALUE;

        for (ShipAPI ship : engine.getShips()) {
            if (ship == source || ship.getOwner() == source.getOwner() || !ship.isAlive()) continue;
            float dist = MathUtils.getDistance(loc, ship.getLocation());
            if (dist < nearestDist && dist <= range) {
                nearest = ship;
                nearestDist = dist;
            }
        }

        return nearest;
    }
    private void renderChargeUpFX() {
        Color gold = new Color(255, 230, 100, 200);
        Color orange = new Color(255, 120, 40, 180);
        Color red = new Color(255, 60, 60, 160);

        for (int i = 0; i < 10; i++) {
            float angle = (float) (Math.random() * Math.PI * 2f);
            float baseRadius = MathUtils.getRandomNumberInRange(60f, 90f);
            boolean isStray = Math.random() < 0.2; // 20% stray particles

            float radius = isStray ? MathUtils.getRandomNumberInRange(100f, 160f) : baseRadius;

            Vector2f spawnPoint = MathUtils.getPoint(corePosition, radius, angle);
            Vector2f toCore = Vector2f.sub(corePosition, spawnPoint, null);
            toCore.normalise();
            toCore.scale(MathUtils.getRandomNumberInRange(30f, 60f)); // speed towards center

            float size = MathUtils.getRandomNumberInRange(6f, 15f);
            float duration = isStray ? 2.5f : 1.2f;

            Color color = Math.random() < 0.5
                    ? gold
                    : (Math.random() < 0.7 ? orange : red);

            engine.addNebulaParticle(
                    spawnPoint,
                    toCore,
                    size,
                    1.8f,
                    0.1f,
                    0.2f,
                    duration,
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), 200)
            );
        }
    }


    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {}
    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {}
    @Override
    public void renderInUICoords(ViewportAPI viewport) {}
    @Override
    public void init(CombatEngineAPI engine) {}
}
