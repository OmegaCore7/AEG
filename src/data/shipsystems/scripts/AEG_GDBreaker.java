package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.shipsystems.helpers.AEG_GigaDrillDmg;
import data.shipsystems.helpers.AEG_GigaDrillEffects;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class AEG_GDBreaker extends BaseShipSystemScript {

    private boolean firstRun = true;
    private final IntervalUtil interval = new IntervalUtil(0f, 0.1f);
    private final IntervalUtil particleInterval = new IntervalUtil(0f, 0.1f);
    private static final String DRILL_WEAPON_ID = "AEG_Giga_Drill";
    private static final Map<String, String> WEAPON_SLOTS = new HashMap<>();

    static {
        WEAPON_SLOTS.put("WS0001", "AEG_gurrenl_shoulder_l");
        WEAPON_SLOTS.put("WS0002", "AEG_gurrenl_shoulder_r");
        WEAPON_SLOTS.put("WS0003", "AEG_gurrenl_arm_l");
        WEAPON_SLOTS.put("WS0004", "AEG_gurrenl_arm_r");
        WEAPON_SLOTS.put("WS0005", "AEG_drillmirv");
        WEAPON_SLOTS.put("WS0008", "AEG_spiralcannon");
        WEAPON_SLOTS.put("WS0009", "AEG_gurrenl_legs");
        WEAPON_SLOTS.put("WS0010", "AEG_spiral_lance");
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        WeaponAPI drillWeapon = null;
        Map<String, WeaponAPI> otherWeapons = new HashMap<>();

        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().getId().equals("WS0006") && w.getSpec().getWeaponId().equals(DRILL_WEAPON_ID)) {
                drillWeapon = w;
            } else if (WEAPON_SLOTS.containsKey(w.getSlot().getId())) {
                otherWeapons.put(w.getSlot().getId(), w);
            }
        }

        if (drillWeapon != null) {
            drillWeapon.getSprite().setAlphaMult(state == State.ACTIVE || state == State.COOLDOWN ? 1.0f : 0.0f);
        }

        if (state == State.ACTIVE) {
            if (firstRun && drillWeapon != null) {
                drillWeapon.getAnimation().setFrame(0);
                drillWeapon.getAnimation().play();
                firstRun = false;

                // Add the notification text
                engine.addFloatingText(ship.getLocation(), "GIGA DRILL BREAKER", 30f, Color.GREEN, ship, 1f, 2f);
            }

            Vector2f shipLocation = ship.getLocation();
            particleInterval.advance(engine.getElapsedInLastFrame());
            if (particleInterval.intervalElapsed()) {
                for (int i = 0; i < 10; i++) {
                    Vector2f particleLocation = MathUtils.getRandomPointInCircle(shipLocation, 50f);
                    engine.addHitParticle(particleLocation, new Vector2f(), 10f, 1f, 0.5f, Color.GREEN);
                }
            }

            // Add micro explosions along the path only when the system is active
            interval.advance(engine.getElapsedInLastFrame());
            if (interval.intervalElapsed()) {
                Vector2f explosionLocation = MathUtils.getRandomPointInCircle(shipLocation, 100f);
                engine.spawnExplosion(explosionLocation, new Vector2f(), Color.ORANGE, 30f, 0.5f);
            }

            ship.getMutableStats().getMaxSpeed().modifyFlat(id, 200f);
            ship.getMutableStats().getAcceleration().modifyFlat(id, 300f);
            ship.getMutableStats().getTurnAcceleration().modifyFlat(id, 200f);
            ship.getMutableStats().getMaxTurnRate().modifyFlat(id, 100f);

            engine.addSmoothParticle(ship.getLocation(), ship.getVelocity(), 100f, 1f, 0.5f, Color.GREEN);

            // Apply damage using the helper class
            AEG_GigaDrillDmg.applyDamage(engine, ship, engine.getElapsedInLastFrame());

            // Create special effects using the helper class
            AEG_GigaDrillEffects.createEffects(engine, ship, engine.getElapsedInLastFrame());

            // Hide other weapons
            for (WeaponAPI w : otherWeapons.values()) {
                w.getSprite().setAlphaMult(0.0f);
            }

        } else {
            if (drillWeapon != null) {
                drillWeapon.getAnimation().pause();
                drillWeapon.getAnimation().setFrame(0);
            }
            firstRun = true;

            // Reset the ship's stats
            ship.getMutableStats().getMaxSpeed().unmodify(id);
            ship.getMutableStats().getAcceleration().unmodify(id);
            ship.getMutableStats().getTurnAcceleration().unmodify(id);
            ship.getMutableStats().getMaxTurnRate().unmodify(id);

            // Show other weapons
            for (WeaponAPI w : otherWeapons.values()) {
                w.getSprite().setAlphaMult(1.0f);
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        // Reset the ship's stats
        ship.getMutableStats().getMaxSpeed().unmodify(id);
        ship.getMutableStats().getAcceleration().unmodify(id);
        ship.getMutableStats().getTurnAcceleration().unmodify(id);
        ship.getMutableStats().getMaxTurnRate().unmodify(id);

        // Show other weapons
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (WEAPON_SLOTS.containsKey(w.getSlot().getId())) {
                w.getSprite().setAlphaMult(1.0f);
            }
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null; // Return status data if needed
    }
}
