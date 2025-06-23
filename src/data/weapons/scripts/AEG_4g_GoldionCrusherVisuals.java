package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;

public class AEG_4g_GoldionCrusherVisuals implements BeamEffectPlugin {


    private boolean hasFired = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine == null || beam == null || beam.getWeapon() == null || beam.getWeapon().getShip() == null) return;
        beam.setWidth(0f);
        WeaponAPI weapon = beam.getWeapon();
        ShipAPI ship = weapon.getShip();
        Object data = ship.getCustomData().get("goldion_active");
        boolean goldionMode = (data instanceof Boolean) && (Boolean) data;
        if (!goldionMode) {
            beam.setWidth(300f);         // Invisible
            return;
        }

        // Otherwise: do beam FX here
    }
}
