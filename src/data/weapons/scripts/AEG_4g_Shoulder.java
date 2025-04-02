package data.weapons.scripts;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import java.awt.Color;
import java.util.List;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.Global;

public class AEG_4g_Shoulder extends BaseEveryFrameCombatPlugin {
    private boolean shipActive = false;
    private float timeElapsed = 0f;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || !engine.isInCampaign()) return;

        ShipAPI ship = engine.getPlayerShip();
        if (ship == null) return;

        if (!shipActive && ship.isAlive()) {
            shipActive = true;
        }

        if (shipActive) {
            timeElapsed += amount;
            if (timeElapsed >= 4f) {
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    weapon.getSprite().setColor(Color.GREEN);
                }
                shipActive = false; // Reset to avoid changing color repeatedly
            }
        }
    }
}