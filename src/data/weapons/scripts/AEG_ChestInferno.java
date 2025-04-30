package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class AEG_ChestInferno extends BaseCombatLayeredRenderingPlugin
        implements OnFireEffectPlugin, OnHitEffectPlugin, EveryFrameWeaponEffectPlugin {
    private AEG_ChestInfernoChargeEffectHelper chargeHelper = new AEG_ChestInfernoChargeEffectHelper();

    protected List<AEG_ChestInferno> trails = new ArrayList<>();
    protected DamagingProjectileAPI proj;
    protected DamagingProjectileAPI prev;
    protected float baseFacing;
    protected List<ParticleData> particles = new ArrayList<>();
    private static final Random random = new Random();
    public AEG_ChestInferno() {}

    public AEG_ChestInferno(DamagingProjectileAPI proj, DamagingProjectileAPI prev) {
        this.proj = proj;
        this.prev = prev;
        this.baseFacing = proj.getFacing();

        int num = 15;
        for (int i = 0; i < num; i++) {
            particles.add(new ParticleData(proj));
        }
    }

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        String key = "aeg_prev_" + weapon.getShip().getId() + "_" + weapon.getSlot().getId();
        DamagingProjectileAPI prevProj = (DamagingProjectileAPI) engine.getCustomData().get(key);

        AEG_ChestInferno trail = new AEG_ChestInferno(projectile, prevProj);
        engine.addLayeredRenderingPlugin(trail);
        trail.init(projectile);
        engine.getCustomData().put(key, projectile);
        trails.add(0, trail);
    }


    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        // Call the visual helper here
        chargeHelper.advance(amount, engine, weapon);

        if (Global.getCombatEngine().isPaused()) return;
        Iterator<AEG_ChestInferno> iter = trails.iterator();
        while (iter.hasNext()) {
            if (iter.next().isExpired()) iter.remove();
        }

        // ⚡ Lightning arcs between chained projectiles
        for (AEG_ChestInferno trail : trails) {
            if (trail.prev != null && !trail.prev.isExpired()) {
                if (Math.random() < 0.1f) {
                    Vector2f from = trail.prev.getLocation();
                    Vector2f to = trail.proj.getLocation();
                    float dur = 1f + (float)Math.random() * 3f;
                    Global.getCombatEngine().addHitParticle(Misc.getDiff(from, to), new Vector2f(), 50f - random.nextInt(25), 1f + random.nextInt(1), dur, new Color(255, 200 - random.nextInt(100), 50 - random.nextInt(45)));
                    Global.getCombatEngine().spawnEmpArcVisual(from, null, to, null, 2f + random.nextInt(18), new Color(255, 200 - random.nextInt(150), 100 - random.nextInt(90)), new Color(255, 100 + random.nextInt(100), 0 + random.nextInt(200)));
                }
            }
            // Rare but long, trailing arcs
            if (Math.random() < 0.01f) { // lower frequency than usual arcs
                Vector2f from = trail.proj.getLocation();
                Vector2f dir = Misc.getUnitVectorAtDegreeAngle((float)Math.random() * 360f);
                dir.scale(150f + (float)Math.random() * 100f);
                Vector2f to = Vector2f.add(from, dir, null);

                Global.getCombatEngine().spawnEmpArcVisual(
                        from, null, to, null,
                        8f + random.nextInt(42),
                        new Color(100 + random.nextInt(100), 200 - random.nextInt(50), 255, 200 + random.nextInt(55)),
                        new Color(20 + random.nextInt(100), 150 - random.nextInt(50), 255, 255 - random.nextInt(100))
                );
            }
        }
    }
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point,
                      boolean shieldHit, ApplyDamageResultAPI result, CombatEngineAPI engine) {

        Vector2f vel = (target instanceof ShipAPI) ? target.getVelocity() : new Vector2f();

        // Fireball-style explosion
        engine.spawnExplosion(point, vel, new Color(255, 80 + random.nextInt(50), 10, 255), 200f - random.nextInt(150), 1.5f - random.nextInt(1));
        engine.addHitParticle(point, vel, 400f - random.nextInt(350), 1f, 0.3f, new Color(255, 120, 40,255 - random.nextInt(200)));

        // Lingering fire cloud
        for (int i = 0; i < 20; i++) {
            float angle = (float) (Math.random() * 360f);
            Vector2f offset = Misc.getUnitVectorAtDegreeAngle(angle);
            offset.scale(50f + (float) Math.random() * 100f);
            Vector2f particlePoint = Vector2f.add(point, offset, null);

            float size = 30f + (float) Math.random() * 50f;
            float dur = 1f + (float) Math.random() * 1.5f;

            engine.addNebulaParticle(particlePoint, vel, size, 2f, 0.5f, 0.2f, dur, new Color(255, 90 + random.nextInt(100), 30 + random.nextInt(50), 200 + random.nextInt(55)));
        }

        // EMP arcs bursting from the impact
        int numArcs = 3 + random.nextInt(3); // 3 to 5 arcs
        for (int i = 0; i < numArcs; i++) {
            Vector2f arcTarget = Misc.getUnitVectorAtDegreeAngle((float) Math.random() * 360f);
            arcTarget.scale(100f + (float) Math.random() * 150f);
            Vector2f targetPoint = Vector2f.add(point, arcTarget, null);

            engine.spawnEmpArcVisual(point, target, targetPoint, null,
                    10f + random.nextInt(50), new Color(255, 180, 50, 200 + random.nextInt(55)), new Color(255, 80, 10, 255 - random.nextInt(105)));
        }
// Only apply scorch and smoke effects if the impact hit the hull
        if (!shieldHit && target instanceof ShipAPI) {
            for (int i = 0; i < 12; i++) {
                float angle = (float) Math.random() * 360f;
                Vector2f offset = Misc.getUnitVectorAtDegreeAngle(angle);
                offset.scale(10f + (float)Math.random() * 25f);
                Vector2f particlePoint = Vector2f.add(point, offset, null);

                float size = 40f + (float)Math.random() * 60f;
                float duration = 2f + (float)Math.random() * 3f;

                Color smokeColor = new Color(20, 20, 20, 150 + random.nextInt(100)); // dark smoke
                engine.addNebulaParticle(
                        particlePoint,
                        new Vector2f(vel),
                        size,
                        1.5f,
                        0.3f,
                        0.1f,
                        duration,
                        smokeColor,
                        false
                );
            }

            // Central lingering "scorch"
            engine.addSmoothParticle(
                    point,
                    new Vector2f(),
                    100f - random.nextInt(65),
                    0.5f,
                    2f + random.nextInt(6),
                    new Color(30, 10, 5, 90 + random.nextInt(100))
            );
        }

    }
    public void init(CombatEntityAPI entity) {
        super.init(entity);
        if (proj != null) entity.getLocation().set(proj.getLocation());
    }

    public boolean isExpired() {
        return proj == null || proj.isExpired() || !Global.getCombatEngine().isEntityInPlay(proj);
    }

    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused()) return;
        if (proj != null) entity.getLocation().set(proj.getLocation());
        for (ParticleData p : particles) p.advance(amount);
    }

    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        float x = entity.getLocation().x;
        float y = entity.getLocation().y;
        float brightness = proj.getBrightness() * viewport.getAlphaMult();

        for (ParticleData p : particles) {
            float size = proj.getProjectileSpec().getWidth() * 0.8f * p.scale;
            Vector2f offset = Misc.rotateAroundOrigin(p.offset, Misc.getAngleDiff(baseFacing, proj.getFacing()));
            Vector2f loc = new Vector2f(x + offset.x, y + offset.y);
            p.sprite.setAngle(p.angle);
            p.sprite.setSize(size, size);
            p.sprite.setAlphaMult(brightness * p.fader.getBrightness());
            p.sprite.setColor(new Color(255, 120, 20, 80 + random.nextInt(100)));
            p.sprite.renderAtCenter(loc.x, loc.y);
        }
    }

    protected EnumSet<CombatEngineLayers> layers = EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return layers;
    }

    // ────────────────────────────────────────────────────────────────
    public static class ParticleData {
        public SpriteAPI sprite;
        public Vector2f offset = new Vector2f();
        public Vector2f vel = new Vector2f();
        public float scale = 1f;
        public float scaleIncreaseRate = 1f;
        public float angle;
        public float turnDir;
        public FaderUtil fader;

        public ParticleData(DamagingProjectileAPI proj) {
            sprite = Global.getSettings().getSprite("misc", "nebula_particles");
            sprite.setTexWidth(0.25f);
            sprite.setTexHeight(0.25f);
            sprite.setTexX(Misc.random.nextInt(4) * 0.25f);
            sprite.setTexY(Misc.random.nextInt(4) * 0.25f);
            sprite.setAdditiveBlend();

            float maxDur = proj.getWeapon().getRange() / proj.getWeapon().getProjectileSpeed();
            scale = 1.5f + (float)Math.random() * 0.75f;
            scaleIncreaseRate = 3.5f / maxDur;
            angle = (float)Math.random() * 360f;
            turnDir = Math.signum((float)Math.random() - 0.5f) * 60f * (float)Math.random();

            Vector2f unit = Misc.getUnitVectorAtDegreeAngle((float)Math.random() * 360f);
            unit.scale(proj.getProjectileSpec().getWidth() / maxDur * 0.4f);
            vel = unit;

            fader = new FaderUtil(0f, 0.25f, 0.5f);
            fader.fadeIn();
        }

        public void advance(float amount) {
            scale += scaleIncreaseRate * amount;
            offset.x += vel.x * amount;
            offset.y += vel.y * amount;
            angle += turnDir * amount;
            fader.advance(amount);
        }
    }
}

