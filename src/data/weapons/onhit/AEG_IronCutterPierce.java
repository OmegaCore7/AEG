package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class AEG_IronCutterPierce implements OnHitEffectPlugin {
    private static final float LINE_LENGTH = 400f;
    private final Random rand = new Random();

    @Override
    public void onHit(final DamagingProjectileAPI projectile, final CombatEntityAPI target,
                      final Vector2f point, final boolean shieldHit, ApplyDamageResultAPI damageResult,
                      final CombatEngineAPI engine) {
        if (engine == null || !(target instanceof ShipAPI)) return;

        final ShipAPI ship = (ShipAPI) target;
        final ShipAPI source = (ShipAPI) projectile.getSource();

        if (shieldHit) {
            // Use EMP logic from first script
            handleEMPChain(engine, source, target, point);
            return;
        }

        // Hull hit logic
        Vector2f direction = Vector2f.sub(target.getLocation(), point, null);
        if (direction.lengthSquared() == 0) {
            direction = new Vector2f(
                    (float) Math.cos(Math.toRadians(projectile.getFacing())),
                    (float) Math.sin(Math.toRadians(projectile.getFacing()))
            );
        } else {
            direction.normalise();
        }

        final Vector2f exitPoint = new Vector2f(point);
        Vector2f scaledDir = new Vector2f(direction);
        scaledDir.scale(LINE_LENGTH);
        Vector2f.add(exitPoint, scaledDir, exitPoint);

        final Vector2f finalDirection = direction;
        engine.addPlugin(new EveryFrameCombatPlugin() {
            private boolean triggered = false;
            private float elapsed = 0f;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (!triggered) {
                    triggered = true;
                    applyLineDamage(engine, target, point, finalDirection, projectile);
                    spawnHullStrikeVisuals(engine, point, exitPoint, finalDirection, ship, projectile);
                }
                elapsed += amount;
                if (elapsed > 0.25f) engine.removePlugin(this);
            }

            @Override public void init(CombatEngineAPI engine) {}
            @Override public void renderInWorldCoords(ViewportAPI viewport) {}
            @Override public void renderInUICoords(ViewportAPI viewport) {}
            @Override public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {}
        });

        engine.removeEntity(projectile);
    }

    private void applyLineDamage(CombatEngineAPI engine, CombatEntityAPI target, Vector2f entryPoint,
                                 Vector2f direction, DamagingProjectileAPI projectile) {
        float baseDamage = projectile.getDamageAmount();
        float ticks = 10f;
        float damagePerTick = baseDamage / ticks;
        float step = LINE_LENGTH / ticks;

        for (int i = 0; i <= ticks; i++) {
            Vector2f p = new Vector2f(entryPoint);
            p.x += direction.x * i * step;
            p.y += direction.y * i * step;
            engine.applyDamage(target, p, damagePerTick, DamageType.HIGH_EXPLOSIVE, 50, false, false, projectile.getSource());
        }
    }

    private void spawnHullStrikeVisuals(final CombatEngineAPI engine, final Vector2f entry, Vector2f exit, Vector2f direction,
                                        final ShipAPI target, final DamagingProjectileAPI projectile) {
        final Vector2f lineDir = Vector2f.sub(exit, entry, null);
        float distance = lineDir.length();
        lineDir.normalise();

        final int segments = 6;
        final float spacing = distance / segments;

        for (int i = 0; i <= segments; i++) {
            Vector2f point = getOffsetPointAlongLine(entry, lineDir, spacing * i);
            spawnExplosionWithRipple(engine, point, 60f + rand.nextFloat() * 80f, 0.3f + rand.nextFloat() * 0.3f);
        }

        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            float timer = 0f;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                timer += amount;
                if (timer > 0.15f && timer < 0.5f) {
                    for (int i = 0; i <= segments; i++) {
                        Vector2f point = getOffsetPointAlongLine(entry, lineDir, spacing * i);
                        spawnExplosionWithRipple(engine, point, 90f + rand.nextFloat() * 100f, 0.3f + rand.nextFloat() * 0.3f);
                    }
                }

                if (timer >= 0.5f + rand.nextInt(2)) {
                    Vector2f finalStrikePoint = new Vector2f(target.getLocation());
                    finalStrikePoint.x += rand.nextFloat() * 80f - 40f;
                    finalStrikePoint.y += rand.nextFloat() * 80f - 40f;

                    engine.spawnExplosion(finalStrikePoint, new Vector2f(), new Color(255, 100 + rand.nextInt(155), 20 + rand.nextInt(80), 255 - rand.nextInt(80)), 250f + rand.nextInt(100), 1.5f);
                    RippleDistortion ripple = new RippleDistortion(finalStrikePoint, new Vector2f());
                    ripple.setSize(400f);
                    ripple.setIntensity(100f);
                    ripple.setLifetime(0.6f);
                    ripple.fadeInIntensity(0.1f);
                    ripple.fadeOutIntensity(0.4f);
                    DistortionShader.addDistortion(ripple);

                    engine.applyDamage(target, finalStrikePoint, 300f, DamageType.HIGH_EXPLOSIVE, 0f, false, false, projectile.getSource());
                    engine.removePlugin(this);
                }
            }
        });
    }

    private void handleEMPChain(final CombatEngineAPI engine, final ShipAPI source, final CombatEntityAPI initialTarget, final Vector2f point) {
        int numArcs = 3 + rand.nextInt(3);

        for (int i = 0; i < numArcs; i++) {
            CombatEntityAPI empTarget = findRandomNearbyTarget(point, 500f, source);
            if (empTarget != null) {
                spawnEMPArc(engine, source, point, empTarget);
            }
        }

        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            private float timer = 0f;
            private boolean triggered = false;
            CombatEntityAPI currentTarget = initialTarget;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (engine.isPaused()) return;
                timer += amount;

                if (timer >= 0.4f && !triggered) {
                    triggered = true;
                    int delayedArcs = 2 + rand.nextInt(2);
                    for (int i = 0; i < delayedArcs; i++) {
                        CombatEntityAPI nextTarget = findRandomNearbyTarget(currentTarget.getLocation(), 600f, source);
                        if (nextTarget != null) {
                            spawnEMPArc(engine, source, currentTarget.getLocation(), nextTarget);
                            currentTarget = nextTarget;
                        }
                    }
                    engine.removePlugin(this);
                }
            }
        });
    }

    private void spawnEMPArc(CombatEngineAPI engine, ShipAPI source, Vector2f from, CombatEntityAPI to) {
        Color empColor = new Color(100 + rand.nextInt(100), 200 + rand.nextInt(55), 255, 200);
        Color coreColor = new Color(200 + rand.nextInt(55), 255, 255, 200 + rand.nextInt(56));
        float emp = 100f + rand.nextFloat() * 200f;
        float dam = 80f + rand.nextFloat() * 60f;
        float thickness = 5f + rand.nextInt(45);

        engine.spawnEmpArcVisual(from, source, to.getLocation(), to, thickness, empColor, coreColor);
        engine.spawnEmpArc(source, from, to, to, DamageType.ENERGY, dam, emp, 10000f, null, thickness, empColor, coreColor);

        engine.spawnExplosion(to.getLocation(), new Vector2f(), empColor, 30f + rand.nextFloat() * 30f, 0.3f);

        RippleDistortion ripple = new RippleDistortion(to.getLocation(), new Vector2f());
        ripple.setSize(150f + rand.nextFloat() * 100f);
        ripple.setIntensity(2f + rand.nextFloat() * 2f);
        ripple.setFrameRate(60f);
        ripple.fadeInSize(0.1f);
        ripple.fadeOutIntensity(0.4f);
        DistortionShader.addDistortion(ripple);
    }

    private CombatEntityAPI findRandomNearbyTarget(Vector2f point, float radius, ShipAPI sourceShip) {
        List<CombatEntityAPI> candidates = new ArrayList<>();

        // Ships
        for (ShipAPI ship : Global.getCombatEngine().getShips()) {
            if (ship == sourceShip || !ship.isAlive() || ship.isShuttlePod()) continue;

            if (Misc.getDistance(point, ship.getLocation()) < radius) {
                candidates.add(ship);
            }
        }

        // Missiles
        for (MissileAPI missile : Global.getCombatEngine().getMissiles()) {
            if (missile.getOwner() == sourceShip.getOwner() || missile.isExpired() || missile.isFading() || missile.didDamage()) continue;

            if (Misc.getDistance(point, missile.getLocation()) < radius) {
                candidates.add(missile);
            }
        }

        // Fighters (separate from ships for safety)
        for (ShipAPI fighter : Global.getCombatEngine().getShips()) {
            if (!fighter.isFighter() || fighter.getOwner() == sourceShip.getOwner() || !fighter.isAlive()) continue;

            if (Misc.getDistance(point, fighter.getLocation()) < radius) {
                candidates.add(fighter);
            }
        }

        if (!candidates.isEmpty()) {
            return candidates.get(rand.nextInt(candidates.size()));
        }

        return null;
    }

    private Vector2f getOffsetPointAlongLine(Vector2f origin, Vector2f dir, float distance) {
        Vector2f point = new Vector2f(origin);
        point.x += dir.x * distance;
        point.y += dir.y * distance;

        float angle = rand.nextFloat() * 360f;
        float offsetMag = rand.nextFloat() * 30f;
        point.x += Math.cos(Math.toRadians(angle)) * offsetMag;
        point.y += Math.sin(Math.toRadians(angle)) * offsetMag;

        return point;
    }

    private void spawnExplosionWithRipple(CombatEngineAPI engine, Vector2f location, float size, float duration) {
        engine.spawnExplosion(location, new Vector2f(), new Color(
                255 - rand.nextInt(55),
                150 - rand.nextInt(50),
                50 + rand.nextInt(50),
                255), size, duration);

        RippleDistortion ripple = new RippleDistortion(location, new Vector2f());
        ripple.setSize(size + rand.nextFloat() * 50f);
        ripple.setIntensity(60f + rand.nextFloat() * 40f);
        ripple.setLifetime(duration);
        ripple.fadeInIntensity(0.1f);
        ripple.fadeOutIntensity(0.3f);
        DistortionShader.addDistortion(ripple);
    }
}
