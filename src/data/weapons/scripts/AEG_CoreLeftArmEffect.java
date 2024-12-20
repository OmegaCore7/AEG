package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.VectorUtils;

public class AEG_CoreLeftArmEffect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private boolean firstRun = true;
    private IntervalUtil interval = new IntervalUtil(0f, 0.1f);

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
            weapon.getSlot().getLocation().set(VectorUtils.rotateAroundPivot(new Vector2f(weapon.getSlot().getLocation().getX() + 21f, weapon.getSlot().getLocation().getY() + 7f), weapon.getSlot().getLocation(), weapon.getCurrAngle() - ship.getFacing()));
            weapon.setCurrAngle(ship.getFacing() + offset);
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        // No specific action on fire
    }
}