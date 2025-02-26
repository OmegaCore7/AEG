package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;

public class AEG_4g_left_boltdriver implements EveryFrameWeaponEffectPlugin {

    private ShipAPI ship;
    private WeaponAPI weapon;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ship = weapon.getShip();
        this.weapon = weapon;

        // Ensure this logic only runs once per frame when the weapon is selected
        if (ship.getSelectedGroupAPI().getActiveWeapon() != weapon) {
            // If not selected, set the animation to frame 0
            weapon.getAnimation().setFrame(0);
        } else {
            // If the weapon is selected, set the animation to frame 1
            weapon.getAnimation().setFrame(1);
        }
    }
}