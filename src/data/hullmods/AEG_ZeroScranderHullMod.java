package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class AEG_ZeroScranderHullMod extends BaseHullMod {

    private static final float SWAY_SPEED_MULTIPLIER = 0.9f;
    private static final float HOVER_AMPLITUDE = 5f;
    private static final float HOVER_FREQUENCY = 1f;
    private static final int TRAIL_COUNT = 5;
    private static final float TRAIL_SPACING = 10f;
    private static final float TRAIL_FADE_TIME = 0.5f;
    private static final String TRAIL_IMAGE_PATH = "graphics/ships/zero/zero_scrander.png";
    private static final float ABSORPTION_RADIUS = 1000f;
    private static final float BLACKHOLE_RADIUS = 3000f;
    private static final float BLACKHOLE_SPEED = 500f;
    private static final float BLACKHOLE_DAMAGE_BASE = 100f;
    private static final float BLACKHOLE_DAMAGE_PER_PROJECTILE = 50f;

    private IntervalUtil hoverInterval = new IntervalUtil(0.1f, 0.1f);
    private IntervalUtil absorptionInterval = new IntervalUtil(10f, 10f);
    private List<Vector2f> trailPositions = new ArrayList<>();
    private List<Float> trailAlphas = new ArrayList<>();

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) return;

        CombatEngineAPI engine = Global.getCombatEngine();
        WeaponAPI scrander = findWeaponBySlot(ship, "WS0001");

        if (scrander == null) return;

        hoverInterval.advance(amount);
        absorptionInterval.advance(amount);

        if (hoverInterval.intervalElapsed()) {
            updateHover(scrander, amount);
            updateTrail(engine, scrander);
        }

        updateSway(scrander, ship, amount);
        addGlowEffect(engine, scrander);

        if (absorptionInterval.intervalElapsed()) {
            int absorbedProjectiles = absorbProjectiles(engine, ship);
            if (absorbedProjectiles > 0) {
                increaseCombatReadiness(ship, absorbedProjectiles);
                shootHomingBlackhole(engine, ship, absorbedProjectiles);
            }
        }
    }

    private WeaponAPI findWeaponBySlot(ShipAPI ship, String slotId) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSlot().getId().equals(slotId)) {
                return weapon;
            }
        }
        return null;
    }

    private void updateHover(WeaponAPI scrander, float amount) {
        float hoverOffset = (float) Math.sin(Global.getCombatEngine().getTotalElapsedTime(false) * HOVER_FREQUENCY) * HOVER_AMPLITUDE;
        scrander.getLocation().y += hoverOffset * amount;
    }

    private void updateSway(WeaponAPI scrander, ShipAPI ship, float amount) {
        Vector2f shipVelocity = ship.getVelocity();
        Vector2f swayVelocity = new Vector2f(shipVelocity.x * SWAY_SPEED_MULTIPLIER, shipVelocity.y * SWAY_SPEED_MULTIPLIER);
        Vector2f.add(scrander.getLocation(), swayVelocity, scrander.getLocation());
    }

    private void updateTrail(CombatEngineAPI engine, WeaponAPI scrander) {
        Vector2f currentPos = new Vector2f(scrander.getLocation());
        trailPositions.add(0, currentPos);
        trailAlphas.add(0, 1f);

        if (trailPositions.size() > TRAIL_COUNT) {
            trailPositions.remove(trailPositions.size() - 1);
            trailAlphas.remove(trailAlphas.size() - 1);
        }

        for (int i = 0; i < trailPositions.size(); i++) {
            Vector2f pos = trailPositions.get(i);
            float alpha = trailAlphas.get(i);
            Color color;
            if (i % 3 == 0) {
                color = new Color(255, 0, 0, (int) (alpha * 255)); // Red
            } else if (i % 3 == 1) {
                color = new Color(255, 255, 0, (int) (alpha * 255)); // Yellow
            } else {
                color = new Color(255, 140, 0, (int) (alpha * 255)); // Sunset (Orange)
            }
            engine.addHitParticle(pos, new Vector2f(), TRAIL_SPACING, alpha, TRAIL_FADE_TIME, color);
            trailAlphas.set(i, alpha - (1f / TRAIL_COUNT));
        }
    }

    private void addGlowEffect(CombatEngineAPI engine, WeaponAPI scrander) {
        Vector2f location = scrander.getLocation();
        engine.addHitParticle(location, new Vector2f(), 20f, 1f, 0.5f, Color.CYAN);
    }

    private int absorbProjectiles(CombatEngineAPI engine, ShipAPI ship) {
        List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
        int absorbedCount = 0;

        for (DamagingProjectileAPI projectile : projectiles) {
            if (projectile.getOwner() != ship.getOwner() && MathUtils.getDistance(ship, projectile) <= ABSORPTION_RADIUS) {
                engine.removeEntity(projectile);
                absorbedCount++;
            }
        }

        return absorbedCount;
    }

    private void increaseCombatReadiness(ShipAPI ship, int absorbedProjectiles) {
        float currentCR = ship.getCurrentCR();
        float maxCR = ship.getMutableStats().getMaxCombatReadiness().getBaseValue();
        ship.setCurrentCR(Math.min(currentCR + absorbedProjectiles * 0.1f, maxCR));
    }

    private void shootHomingBlackhole(CombatEngineAPI engine, ShipAPI ship, int absorbedProjectiles) {
        CombatEntityAPI target = findNearestEnemy(engine, ship);
        if (target == null) return;

        Vector2f spawnLocation = new Vector2f(ship.getLocation());
        Vector2f directionToTarget = VectorUtils.getDirectionalVector(spawnLocation, target.getLocation());
        directionToTarget.scale(BLACKHOLE_SPEED);

        DamagingProjectileAPI blackhole = (DamagingProjectileAPI) engine.spawnProjectile(
                ship, null, "blackhole_weapon_id", spawnLocation, VectorUtils.getAngle(spawnLocation, target.getLocation()), directionToTarget);

        float damage = BLACKHOLE_DAMAGE_BASE + (absorbedProjectiles * BLACKHOLE_DAMAGE_PER_PROJECTILE);
        blackhole.setDamageAmount(damage);
    }

    private CombatEntityAPI findNearestEnemy(CombatEngineAPI engine, ShipAPI ship) {
        List<ShipAPI> potentialTargets = engine.getShips();
        CombatEntityAPI closestTarget = null;
        float closestDistance = Float.MAX_VALUE;

        for (ShipAPI entity : potentialTargets) {
            if (entity.getOwner() == ship.getOwner() || entity.isAlly()) continue;

            float distance = MathUtils.getDistance(ship.getLocation(), entity.getLocation());
            if (distance < closestDistance && distance <= BLACKHOLE_RADIUS) {
                closestDistance = distance;
                closestTarget = entity;
            }
        }

        return closestTarget;
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // Apply any initial effects here
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "hover and follow the main ship's movement";
        if (index == 1) return "suck up enemy projectiles and increase combat readiness";
        if (index == 2) return "shoot a homing blackhole at the nearest enemy ship";
        return null;
    }
}
