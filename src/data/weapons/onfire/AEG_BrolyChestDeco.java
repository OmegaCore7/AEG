package data.weapons.onfire;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.*;
import java.util.List;

public class AEG_BrolyChestDeco implements EveryFrameWeaponEffectPlugin {
    private IntervalUtil interval = new IntervalUtil(0.1f, 0.1f);
    private boolean shieldDeployed = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null) return;

        ShipAPI ship = weapon.getShip();
        if (ship.isAlive() && ship.getOriginalOwner() == 0) { // Assuming player ship
            WeaponAPI weapon1 = getWeaponBySlot(ship, "WS0005");
            WeaponAPI weapon2 = getWeaponBySlot(ship, "WS0006");

            if (weapon1 != null && weapon2 != null) {
                if (weapon1.isFiring() && weapon2.isFiring()) {
                    triggerGlowEffect(weapon);
                    deployShield(ship);
                } else {
                    resetGlowEffect(weapon);
                }
            }
        }
    }

    private WeaponAPI getWeaponBySlot(ShipAPI ship, String slotId) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSlot().getId().equals(slotId)) {
                return weapon;
            }
        }
        return null;
    }

    private void triggerGlowEffect(WeaponAPI weapon) {
        weapon.getSprite().setColor(new Color(105, 255, 105, 255));
    }

    private void resetGlowEffect(WeaponAPI weapon) {
        weapon.getSprite().setColor(Color.white);
    }

    private void deployShield(final ShipAPI ship) {
        if (!shieldDeployed) {
            ship.getShield().toggleOn();
            shieldDeployed = true;
        }
        ship.getShield().setRingColor(new Color(105, 255, 105, 255));
        ship.getShield().setActiveArc(ship.getShield().getArc());
        ship.getShield().setRadius(ship.getShield().getRadius());

        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            private float timer = 2f;

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                timer -= amount;
                if (timer <= 0) {
                    ship.getShield().setActiveArc(ship.getShield().getArc());
                    ship.getShield().setRadius(ship.getShield().getRadius());
                    Global.getCombatEngine().removePlugin(this);
                }
            }
        });
    }
}