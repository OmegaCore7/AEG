package data.shipsystems.scripts;

import com.fs.starfarer.api.AnimationAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class AEG_GDBreaker implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private boolean firstRun = true;
    private IntervalUtil interval = new IntervalUtil(0f, 0.1f);
    private IntervalUtil particleInterval = new IntervalUtil(0f, 0.1f);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || engine.isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        if (ship == null) return;

        ShipSystemAPI system = ship.getSystem();
        if (system == null) return;

        WeaponAPI drillWeapon = null;
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getId().equals("WS0006")) {
                drillWeapon = w;
                break;
            }
        }

        if (drillWeapon != null) {
            drillWeapon.getSprite().setAlphaMult(system.isActive() || system.isCoolingDown() ? 1.0f : 0.0f);
        }

        if (system.isActive()) {
            if (firstRun && drillWeapon != null) {
                drillWeapon.getAnimation().setFrame(0);
                drillWeapon.getAnimation().play();
                firstRun = false;

                // Add the notification text
                engine.addFloatingText(ship.getLocation(), "GIGA DRILL BREAKER", 30f, Color.GREEN, ship, 1f, 2f);
            }

            Vector2f shipLocation = ship.getLocation();
            particleInterval.advance(amount);
            if (particleInterval.intervalElapsed()) {
                for (int i = 0; i < 10; i++) {
                    Vector2f particleLocation = MathUtils.getRandomPointInCircle(shipLocation, 50f);
                    engine.addHitParticle(particleLocation, new Vector2f(), 10f, 1f, 0.5f, Color.GREEN);
                }
            }

            // Add micro explosions along the path only when the system is active
            interval.advance(amount);
            if (interval.intervalElapsed()) {
                Vector2f explosionLocation = MathUtils.getRandomPointInCircle(shipLocation, 100f);
                engine.spawnExplosion(explosionLocation, new Vector2f(), Color.ORANGE, 30f, 0.5f);
            }

            ship.getMutableStats().getMaxSpeed().modifyFlat("drill_system", 200f);
            ship.getMutableStats().getAcceleration().modifyFlat("drill_system", 300f);
            ship.getMutableStats().getTurnAcceleration().modifyFlat("drill_system", 200f);
            ship.getMutableStats().getMaxTurnRate().modifyFlat("drill_system", 100f);

            engine.addSmoothParticle(ship.getLocation(), ship.getVelocity(), 100f, 1f, 0.5f, Color.GREEN);

            List<CombatEntityAPI> entities = CombatUtils.getEntitiesWithinRange(ship.getLocation(), 100f);
            if (entities != null) {
                for (CombatEntityAPI entity : entities) {
                    if (entity instanceof ShipAPI && entity != ship) {
                        float damage = 500f; // Example damage value
                        engine.applyDamage(entity, entity.getLocation(), damage, DamageType.ENERGY, 0f, false, false, ship);
                    }
                }
            }
        } else {
            if (drillWeapon != null) {
                drillWeapon.getAnimation().pause();
                drillWeapon.getAnimation().setFrame(0);
            }
            firstRun = true;

            // Reset the ship's stats
            ship.getMutableStats().getMaxSpeed().unmodify("drill_system");
            ship.getMutableStats().getAcceleration().unmodify("drill_system");
            ship.getMutableStats().getTurnAcceleration().unmodify("drill_system");
            ship.getMutableStats().getMaxTurnRate().unmodify("drill_system");
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        // Implement any additional effects when the weapon fires, if needed
    }
}
