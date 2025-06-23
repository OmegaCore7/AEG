package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class AEG_4g_helixSpear implements EveryFrameCombatPlugin {
    private final ShipAPI source;
    private final CombatEngineAPI engine;

    private final float SPEED = 800f;              // faster than the ball
    private final float MAX_DURATION = 6f;         // shorter lifetime for fast firing
    private final float DAMAGE = 250f;              // much lower damage per shot
    private final float EXPLOSION_RADIUS = 40f;    // smaller explosion radius
    private final int ORB_COUNT = 10;               // fewer orbs for narrow effect

    private float timer = 0f;
    private boolean exploded = false;

    // Spiral params - tighter spiral for spear-like shape
    private final float spiralRadius = 20f;
    private final float spiralSpeed = 10f;

    // Homing params
    private final float HOMING_TURN_RATE = 90f;    // keep same turn rate
    private final float HOMING_RANGE = 250f;

    private final Vector2f corePosition;
    private Vector2f coreVelocity;

    private final int playerOwnerId;

    public AEG_4g_helixSpear(ShipAPI source, Vector2f spawnLoc, float angle, CombatEngineAPI engine, int playerOwnerId) {
        this.source = source;
        this.engine = engine;
        this.playerOwnerId = playerOwnerId;
        this.corePosition = new Vector2f(spawnLoc);
        this.coreVelocity = MathUtils.getPoint(new Vector2f(), SPEED, angle);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine.isPaused() || exploded || source == null || !source.isAlive()) return;

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

        renderCoreSpear();
        renderOrbitingPlasma();
        renderNebulaTrail();
    }

    private void steerTowards(Vector2f targetLoc, float amount) {
        Vector2f desiredDir = Vector2f.sub(targetLoc, corePosition, null);
        desiredDir.normalise();

        Vector2f currentDir = new Vector2f(coreVelocity);
        currentDir.normalise();

        float currentAngle = (float) Math.toDegrees(Math.atan2(currentDir.y, currentDir.x));
        float desiredAngle = (float) Math.toDegrees(Math.atan2(desiredDir.y, desiredDir.x));

        float angleDiff = MathUtils.getShortestRotation(currentAngle, desiredAngle);

        float maxTurn = HOMING_TURN_RATE * amount;
        float turnAmount = Math.abs(angleDiff) < maxTurn ? angleDiff : Math.copySign(maxTurn, angleDiff);

        float newAngle = currentAngle + turnAmount;

        coreVelocity = MathUtils.getPoint(new Vector2f(), SPEED, newAngle);
    }

    private void explode() {
        exploded = true;
        Global.getSoundPlayer().playSound("hit_hull_heavy_energy", 1f, 1f, corePosition, coreVelocity);
        spawnExplosionFX();
        engine.removePlugin(this);
    }

    private void explode(CombatEntityAPI target) {
        exploded = true;
        Global.getSoundPlayer().playSound("hit_shield_heavy_energy", 1f, 1f, corePosition, coreVelocity);
        Global.getSoundPlayer().playSound("rifttorpedo_explosion", 0.8f, 1.1f, corePosition, coreVelocity);
        Global.getSoundPlayer().playSound("riftcascade_rift", 0.6f, 1.2f, corePosition, coreVelocity);
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
        engine.spawnExplosion(corePosition, new Vector2f(), new Color(255, 235, 80), 75f, 1f);
        engine.addSmoothParticle(corePosition, new Vector2f(), 100f, 1.2f, 1f, new Color(255, 255, 200, 200));
        engine.addHitParticle(corePosition, new Vector2f(), 60f, 0.8f, 0.4f, Color.white);

        for (int i = 0; i < 16; i++) {
            Vector2f randVel = MathUtils.getRandomPointInCircle(null, 150f);
            engine.addNebulaParticle(
                    corePosition,
                    randVel,
                    MathUtils.getRandomNumberInRange(10f, 20f),
                    1.2f,
                    0.1f,
                    0.2f,
                    MathUtils.getRandomNumberInRange(1f, 2f),
                    new Color(255, 240, 130, 150)
            );
        }
    }

    private void renderCoreSpear() {
        engine.addSmoothParticle(
                corePosition,
                new Vector2f(),
                40f + MathUtils.getRandom().nextInt(40),
                2f,
                0.1f,
                new Color(255, 245 - MathUtils.getRandom().nextInt(80), 180 - MathUtils.getRandom().nextInt(50), 200 - MathUtils.getRandom().nextInt(50))
        );
        engine.addSmoothParticle(
                corePosition,
                new Vector2f(),
                60f + MathUtils.getRandom().nextInt(40),
                1.5f,
                0.1f,
                new Color(255, 200 - MathUtils.getRandom().nextInt(80), MathUtils.getRandom().nextInt(50), 180 - MathUtils.getRandom().nextInt(50))
        );
    }

    private void renderOrbitingPlasma() {
        for (int i = 0; i < ORB_COUNT; i++) {
            float phase = (float) ((Math.PI * 2f) * ((float) i / ORB_COUNT));
            float chaoticOffset = (float) Math.sin(timer * 3f + i) * 0.5f;

            float spiralAngle = spiralSpeed * timer + phase + chaoticOffset;
            float dynamicRadius = spiralRadius + MathUtils.getRandomNumberInRange(-5f, 10f);

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

            Vector2f randMotion = MathUtils.getRandomPointInCircle(null, 20f);

            engine.addSmoothParticle(
                    orbLocation,
                    randMotion,
                    MathUtils.getRandomNumberInRange(10f, 20f),
                    1f,
                    0.15f,
                    new Color(255, 200 + MathUtils.getRandom().nextInt(50), 50 + MathUtils.getRandom().nextInt(50), 180 - MathUtils.getRandom().nextInt(60))
            );
        }
    }

    private void renderNebulaTrail() {
        Vector2f trailPoint = new Vector2f(corePosition);
        Vector2f behind = new Vector2f(coreVelocity);
        behind.normalise();
        behind.scale(-20f);
        Vector2f.add(trailPoint, behind, trailPoint);

        engine.addNebulaParticle(
                trailPoint,
                new Vector2f(),
                MathUtils.getRandomNumberInRange(15f, 30f),
                1.2f,
                0.1f,
                0.2f,
                1.2f,
                new Color(255, 240 - MathUtils.getRandom().nextInt(70), 120 - MathUtils.getRandom().nextInt(80), 110 + MathUtils.getRandom().nextInt(70))
        );
    }

    private CombatEntityAPI getNearestEnemy(CombatEngineAPI engine, ShipAPI source, Vector2f loc, float range) {
        CombatEntityAPI nearest = null;
        float nearestDist = Float.MAX_VALUE;

        for (ShipAPI ship : engine.getShips()) {
            if (!ship.isAlive()) continue;
            if (ship == source) continue; // skip the infused ship itself
            if (ship.getOwner() == playerOwnerId) continue; // skip player's allies
            float dist = MathUtils.getDistance(loc, ship.getLocation());
            if (dist < nearestDist && dist <= range) {
                nearest = ship;
                nearestDist = dist;
            }
        }
        return nearest;
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
