package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_BlasterShell extends BaseShipSystemScript {

    public static final float DAMAGE_MULT = 1.2f; // Increase damage by 20%
    private static final Color SHIELD_COLOR = new Color(100, 220, 100, 225); // Green color with some transparency
    private static final Color DEFAULT_SHIELD_COLOR = Color.WHITE; // Default shield color
    private static final String SYSTEM_ID = "LaunchAndThrowSystem";

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

        // Blaster Shell effect
        if (state == State.ACTIVE) {
            stats.getShieldDamageTakenMult().modifyMult(id, 1f - DAMAGE_MULT * effectLevel);
            stats.getShieldUpkeepMult().modifyMult(id, 0f);

            // Simulate shield effect
            if (ship.getShield() == null || !ship.getShield().isOn()) {
                // Temporarily modify stats to simulate a shield
                stats.getShieldDamageTakenMult().modifyMult(id, 0.1f); // Simulate shield absorbing 90% damage
                stats.getShieldUpkeepMult().modifyMult(id, 0f); // No shield upkeep cost
                ship.setJitterUnder(this, SHIELD_COLOR, effectLevel, 10, 5f, 10f); // Visual effect
            } else {
                ship.getShield().toggleOn();
                ship.getShield().setRingColor(SHIELD_COLOR);
                ship.getShield().setInnerColor(SHIELD_COLOR);
            }

            // Apply range, turn rate, and damage buffs
            stats.getBallisticWeaponRangeBonus().modifyPercent(id, 20f * effectLevel);
            stats.getEnergyWeaponRangeBonus().modifyPercent(id, 20f * effectLevel);
            stats.getMissileWeaponRangeBonus().modifyPercent(id, 20f * effectLevel);
            stats.getBallisticWeaponDamageMult().modifyMult(id, DAMAGE_MULT);
            stats.getEnergyWeaponDamageMult().modifyMult(id, DAMAGE_MULT);
            stats.getMissileWeaponDamageMult().modifyMult(id, DAMAGE_MULT);
        }
    }


    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) return;

        if (ship.getSystem().getId().equals(SYSTEM_ID) && ship.getSystem().isActive()) {
            System.out.println("System is active, finding nearest enemy...");
            ShipAPI nearestEnemy = findNearestEnemy(ship);
            if (nearestEnemy != null) {
                System.out.println("Nearest enemy found, launching towards enemy...");
                launchTowardsEnemy(ship, nearestEnemy);
                System.out.println("Throwing enemy...");
                throwEnemy(ship, nearestEnemy);
            } else {
                System.out.println("No enemy found within range.");
            }
        }
    }

    private ShipAPI findNearestEnemy(ShipAPI ship) {
        ShipAPI nearestEnemy = null;
        float minDistance = Float.MAX_VALUE;
        for (ShipAPI enemy : CombatUtils.getShipsWithinRange(ship.getLocation(), 10000f)) {
            if (enemy.getOwner() != ship.getOwner()) {
                float distance = MathUtils.getDistance(ship, enemy);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestEnemy = enemy;
                }
            }
        }
        return nearestEnemy;
    }

    private void launchTowardsEnemy(ShipAPI ship, ShipAPI enemy) {
        Vector2f direction = Vector2f.sub(enemy.getLocation(), ship.getLocation(), new Vector2f());
        direction.normalise();
        direction.scale(1000f); // Adjust the speed as needed
        ship.getVelocity().set(direction);
    }

    private void throwEnemy(ShipAPI ship, ShipAPI enemy) {
        // Ram the enemy
        Vector2f ramDirection = Vector2f.sub(enemy.getLocation(), ship.getLocation(), new Vector2f());
        ramDirection.normalise();
        ramDirection.scale(500f); // Adjust the ramming force as needed
        enemy.getVelocity().set(ramDirection);

        // Spin the enemy around the player ship
        Vector2f spinDirection = new Vector2f(-ramDirection.y, ramDirection.x); // Perpendicular vector
        spinDirection.scale(300f); // Adjust the spin force as needed
        Vector2f.add(enemy.getVelocity(), spinDirection, enemy.getVelocity());

        // Launch the enemy in the original direction
        Vector2f launchDirection = new Vector2f(ramDirection);
        launchDirection.scale(1500f); // Adjust the launch force as needed
        Vector2f.add(enemy.getVelocity(), launchDirection, enemy.getVelocity());
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship != null && ship.getShield() != null) {
            ship.getShield().toggleOff();
            ship.getShield().setRingColor(DEFAULT_SHIELD_COLOR); // Reset to default color
            ship.getShield().setInnerColor(DEFAULT_SHIELD_COLOR); // Reset to default color
        }

        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getShieldDamageTakenMult().unmodify(id);
        stats.getShieldUpkeepMult().unmodify(id);
        stats.getBallisticWeaponRangeBonus().unmodify(id);
        stats.getEnergyWeaponRangeBonus().unmodify(id);
        stats.getMissileWeaponRangeBonus().unmodify(id);
        stats.getBallisticWeaponDamageMult().unmodify(id);
        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getMissileWeaponDamageMult().unmodify(id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        switch (index) {
            case 0:
                return new StatusData("improved maneuverability", false);
            case 1:
                return new StatusData("+50 top speed", false);
            case 2:
                return new StatusData("shield absorbs 10x damage", false);
            case 3:
                return new StatusData("increased weapon range and damage", false);
            default:
                return null;
        }
    }
}
