package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import java.awt.Color;
import java.util.List;

public class AEG_Gigantic_Catastraphe extends BaseShipSystemScript {

    public static float DAMAGE_MULT = 0.9f;
    private static final Color SHIELD_COLOR = new Color(100, 220, 100, 225); // Green color with some transparency
    private static final float PUSH_RADIUS = 300f;
    private static final float BASE_PUSH_FORCE = 500f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        // Plasma Jets effect
        if (state == State.OUT) {
            stats.getMaxSpeed().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
        } else {
            stats.getMaxSpeed().modifyFlat(id, 50f);
            stats.getAcceleration().modifyPercent(id, 200f * effectLevel);
            stats.getDeceleration().modifyPercent(id, 200f * effectLevel);
            stats.getTurnAcceleration().modifyFlat(id, 30f * effectLevel);
            stats.getTurnAcceleration().modifyPercent(id, 200f * effectLevel);
            stats.getMaxTurnRate().modifyFlat(id, 15f);
            stats.getMaxTurnRate().modifyPercent(id, 100f);
        }

        // Fortress Shield effect
        if (state == State.ACTIVE) {
            stats.getShieldDamageTakenMult().modifyMult(id, 1f - DAMAGE_MULT * effectLevel);
            stats.getShieldUpkeepMult().modifyMult(id, 0f);

            // Activate shield and change color
            if (!ship.getShield().isOn()) {
                ship.getShield().toggleOn();
            }
            ship.getShield().setRingColor(SHIELD_COLOR);
            ship.getShield().setInnerColor(SHIELD_COLOR);

            // Apply pushing force
            applyPushingForce(ship);
        }

        // Fire all weapons
        if (state == State.ACTIVE) {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.isDisabled() || weapon.isPermanentlyDisabled()) continue;
                weapon.forceFire(ship);
            }
        }
    }

    private void applyPushingForce(ShipAPI ship) {
        List<CombatEntityAPI> entities = Global.getCombatEngine().getAllEntities();
        for (CombatEntityAPI entity : entities) {
            if (entity == ship) continue;
            float distance = MathUtils.getDistance(ship, entity);
            if (distance > PUSH_RADIUS) continue;

            float force = BASE_PUSH_FORCE;
            if (entity instanceof MissileAPI || entity instanceof FighterWingAPI) {
                force *= 1.0f;
            } else if (entity instanceof ShipAPI) {
                ShipAPI targetShip = (ShipAPI) entity;
                switch (targetShip.getHullSize()) {
                    case FRIGATE:
                        force *= 0.75f;
                        break;
                    case DESTROYER:
                        force *= 0.5f;
                        break;
                    case CRUISER:
                        force *= 0.25f;
                        break;
                    case CAPITAL_SHIP:
                        force *= 0.125f;
                        break;
                    default:
                        break;
                }
            } else {
                continue;
            }

            Vector2f direction = Vector2f.sub(entity.getLocation(), ship.getLocation(), new Vector2f());
            direction = MathUtils.normalize(direction);
            direction.scale(force);
            entity.getVelocity().add(direction);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship != null && ship.getShield() != null) {
            ship.getShield().toggleOff();
            ship.getShield().setRingColor(null); // Reset to default color
            ship.getShield().setInnerColor(null); // Reset to default color
        }

        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getShieldDamageTakenMult().unmodify(id);
        stats.getShieldUpkeepMult().unmodify(id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("improved maneuverability", false);
        } else if (index == 1) {
            return new StatusData("+50 top speed", false);
        } else if (index == 2) {
            return new StatusData("shield absorbs 10x damage", false);
        }
        return null;
    }
}
