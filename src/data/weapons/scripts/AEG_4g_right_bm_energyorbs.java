package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.List;

public class AEG_4g_right_bm_energyorbs implements EveryFrameCombatPlugin {

    private final ShipAPI source;
    private final Vector2f location;
    private final CombatEngineAPI engine;
    private final Vector2f velocity;
    private float angle;
    private float timer = 0f;
    private boolean exploded = false;

    private final float SPEED = 150f;
    private final float MAX_DURATION = 3.5f;
    private final float EXPLOSION_RADIUS = 60f;
    private final float DAMAGE = 200f;

    public AEG_4g_right_bm_energyorbs(ShipAPI source, Vector2f spawnLoc, float angle, CombatEngineAPI engine) {
        this.source = source;
        this.location = new Vector2f(spawnLoc);
        this.angle = angle;
        this.velocity = MathUtils.getPoint(new Vector2f(), SPEED, angle);
        this.engine = engine;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine.isPaused() || exploded || source == null || !source.isAlive()) return;
        ShipAPI ship = source;

        boolean isGoldionActive = ship.getCustomData().get("goldion_active") instanceof Boolean
                && (Boolean) ship.getCustomData().get("goldion_active");

        if (!isGoldionActive) {
            // Goldion mode NOT active, skip all orb logic (no animation, no spawn)
            return;
        }
        timer += amount;
        if (timer >= MAX_DURATION) {
            explode();
            return;
        }

        CombatEntityAPI target = getNearestEnemy(engine, source, location, 1000f);
        if (target != null) {
            Vector2f toTarget = Vector2f.sub(target.getLocation(), location, null);
            toTarget.normalise();
            toTarget.scale(SPEED);
            velocity.set(toTarget);
        }

        // Move the orb
        Vector2f.add(location, (Vector2f) new Vector2f(velocity).scale(amount), location);

        // Check for proximity hit
        if (target != null && MathUtils.getDistance(location, target.getLocation()) <= EXPLOSION_RADIUS) {
            explode(target);
        }

        // Trail effect
        engine.addSmoothParticle(location, new Vector2f(), 15f, 1.2f, 0.3f,
                new Color(255, 230, 100, 200));
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
                0f,
                false,
                false,
                source
        );
        engine.removePlugin(this);
    }

    private void spawnExplosionFX() {
        engine.spawnExplosion(location, velocity, new Color(255, 215, 50), 50f, 0.5f);
        for (int i = 0; i < 12; i++) {
            Vector2f randVel = MathUtils.getRandomPointInCircle(null, 100f);
            engine.addNebulaParticle(
                    location,
                    randVel,
                    MathUtils.getRandomNumberInRange(10f, 20f),
                    1.8f,
                    0.1f,
                    0.4f,
                    0.7f,
                    new Color(255, 240, 130, 220)
            );
        }
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
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
        // no input handling needed
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
        // no custom rendering needed
    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {
        // no UI rendering needed
    }
    @Override
    public void init(CombatEngineAPI engine) {
        // Already handled via constructor, or can be ignored.
    }
}
