package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.shipsystems.helpers.AEG_GigaDrillDmg;
import java.awt.*;

public class AEG_GDBreaker extends BaseShipSystemScript {

    private boolean firstRun = true;
    private final AEG_GigaDrillDmg drillDamageHelper = new AEG_GigaDrillDmg();

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        WeaponAPI drillWeapon = null;

        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().getId().equals("WS0006")) {
                drillWeapon = w;
                break;
            }
        }

        if (state == State.ACTIVE) {
            if (firstRun) {
                if (drillWeapon != null) {
                    drillWeapon.getAnimation().setFrame(1);
                    drillWeapon.getAnimation().play();
                }
                firstRun = false;

                // Add the notification text
                engine.addFloatingText(ship.getLocation(), "GIGA DRILL BREAKER!", 30f, Color.GREEN, ship, 1f, 2f);
            }

            if (drillWeapon != null) {
                drillWeapon.getSprite().setAlphaMult(1.0f);
                int frame = (int) ((engine.getTotalElapsedTime(false) * 20) % 4) + 1; // Loop through frames 1 to 4, twice as fast
                drillWeapon.getAnimation().setFrame(frame);

                // Make all other weapons face the same direction as WS0006
                float drillAngle = drillWeapon.getCurrAngle();
                for (WeaponAPI w : ship.getAllWeapons()) {
                    if (!w.getSlot().getId().equals("WS0006")) {
                        w.setCurrAngle(drillAngle);
                    }
                }
            }

            ship.getMutableStats().getMaxSpeed().modifyFlat(id, 200f);
            ship.getMutableStats().getAcceleration().modifyFlat(id, 300f);
            ship.getMutableStats().getTurnAcceleration().modifyFlat(id, 200f);
            ship.getMutableStats().getMaxTurnRate().modifyFlat(id, 100f);

            // Increase engine flame size by 5x
            ship.getEngineController().extendFlame(id, 5.0f, 5.0f, 5.0f);

            // Reduce incoming damage by 99%
            ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0.01f);
            ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, 0.01f);
            ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, 0.01f);

            // Apply drill damage
            drillDamageHelper.applyDrillDamage(ship, drillWeapon, engine, 500f);

        } else {
            if (drillWeapon != null) {
                drillWeapon.getAnimation().pause();
                drillWeapon.getAnimation().setFrame(0); // Stay on frame 00 when not active
            }
            firstRun = true;

            // Reset the ship's stats
            ship.getMutableStats().getMaxSpeed().unmodify(id);
            ship.getMutableStats().getAcceleration().unmodify(id);
            ship.getMutableStats().getTurnAcceleration().unmodify(id);
            ship.getMutableStats().getMaxTurnRate().unmodify(id);

            // Reset engine flame size
            ship.getEngineController().extendFlame(id, 1.0f, 1.0f, 1.0f);

            // Reset damage taken modifiers
            ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
            ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
            ship.getMutableStats().getShieldDamageTakenMult().unmodify(id);

            // Reset all weapons to their default angles
            for (WeaponAPI w : ship.getAllWeapons()) {
                w.setCurrAngle(w.getSlot().getAngle());
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

        // Reset engine flame size
        ship.getEngineController().extendFlame(id, 1.0f, 1.0f, 1.0f);

        // Reset damage taken modifiers
        ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
        ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
        ship.getMutableStats().getShieldDamageTakenMult().unmodify(id);

        // Ensure the drill weapon is on frame 00
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().getId().equals("WS0006")) {
                w.getAnimation().pause();
                w.getAnimation().setFrame(0);
            }
            // Reset all weapons to their default angles
            w.setCurrAngle(w.getSlot().getAngle());
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null; // Return status data if needed
    }
}
