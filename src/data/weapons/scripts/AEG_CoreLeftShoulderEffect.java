package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;

public class AEG_CoreLeftShoulderEffect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private boolean firstRun = true;
    private IntervalUtil interval = new IntervalUtil(0f, 1f);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || engine.isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        WeaponAPI mainWeapon = null;

        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().getId().equals("WS0006")) { // Main weapon
                mainWeapon = w;
                break;
            }
        }

        if (mainWeapon != null) {
            float offset = 0f;
            weapon.setCurrAngle(mainWeapon.getCurrAngle() + offset);
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        // No specific action on fire
    }
}