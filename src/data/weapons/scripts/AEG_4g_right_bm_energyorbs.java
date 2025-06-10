package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class AEG_4g_right_bm_energyorbs implements EveryFrameCombatPlugin {
    private final ShipAPI source;
    private final CombatEngineAPI engine;

    private final float SPEED = 350f;
    private final float MAX_DURATION = 10f;
    private final float DAMAGE = 1000f;
    private final float EXPLOSION_RADIUS = 100f; // Wider for visual impact

    private float timer = 0f;
    private boolean exploded = false;

    // Spiral motion parameters
    private final float spiralRadius = 75f;
    private final float spiralSpeed = 6f;
    private final float spiralPhase;

    private final Vector2f corePosition;
    private final Vector2f coreVelocity;
    private final Vector2f location = new Vector2f();

    public AEG_4g_right_bm_energyorbs(ShipAPI source, Vector2f spawnLoc, float angle, CombatEngineAPI engine, float spiralPhase) {
        this.source = source;
        this.engine = engine;
        this.spiralPhase = spiralPhase;

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

        // Move the core forward
        Vector2f forwardStep = new Vector2f(coreVelocity);
        forwardStep.scale(amount);
        Vector2f.add(corePosition, forwardStep, corePosition);

        // Calculate spiral offset
        float spiralAngle = spiralSpeed * timer + spiralPhase;
        float offsetX = (float) Math.cos(spiralAngle) * spiralRadius;
        float offsetY = (float) Math.sin(spiralAngle) * spiralRadius;

        Vector2f velocityDir = new Vector2f(coreVelocity);
        velocityDir.normalise();
        Vector2f perpendicular = new Vector2f(-velocityDir.y, velocityDir.x);

        Vector2f spiralOffset = new Vector2f(perpendicular);
        spiralOffset.scale(offsetX);

        Vector2f forwardOffset = new Vector2f(velocityDir);
        forwardOffset.scale(offsetY);

        Vector2f.add(corePosition, spiralOffset, location);
        Vector2f.add(location, forwardOffset, location);

        // Check for impact with enemies
        CombatEntityAPI target = getNearestEnemy(engine, source, location, EXPLOSION_RADIUS);
        if (target != null) {
            explode(target);
        }

        // === VISUAL EFFECTS ===
        renderEffects();
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
                location,
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
        engine.spawnExplosion(location, new Vector2f(), new Color(255, 235, 80), 150f, 1.5f);
        engine.addSmoothParticle(location, new Vector2f(), 200f, 1.8f, 1.2f, new Color(255, 255, 200, 255));
        engine.addHitParticle(location, new Vector2f(), 120f, 1f, 0.5f, Color.white);

        for (int i = 0; i < 24; i++) {
            Vector2f randVel = MathUtils.getRandomPointInCircle(null, 300f);
            engine.addNebulaParticle(
                    location,
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

    private void renderEffects() {
        engine.addSmoothParticle(
                location,
                new Vector2f(),
                25f + MathUtils.getRandom().nextInt(40),
                1.2f,
                0.3f,
                new Color(255, 230, MathUtils.getRandom().nextInt(100), 220)
        );

        engine.addNebulaParticle(
                location,
                new Vector2f(),
                MathUtils.getRandomNumberInRange(10f, 20f),
                1.6f,
                0.2f,
                0.4f,
                MathUtils.getRandomNumberInRange(2f, 3f),
                new Color(255, 240, 120, 160)
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

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {}

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {}

    @Override
    public void renderInUICoords(ViewportAPI viewport) {}

    @Override
    public void init(CombatEngineAPI engine) {}
}
