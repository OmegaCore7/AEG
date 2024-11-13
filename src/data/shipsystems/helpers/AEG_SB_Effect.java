package data.shipsystems.helpers;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import java.awt.Color;

public class AEG_SB_Effect {

    public static void applyRammingEffects(ShipAPI ship, CombatEngineAPI engine) {
        // Ensure the shield is active
        if (!ship.getShield().isOn()) {
            ship.getShield().toggleOn();
        }

        // Boost shield unfold rate
        ship.getMutableStats().getShieldUnfoldRateMult().modifyMult("AEG_SB_Effect", 2.0f);

        // Change shield color
        ship.getShield().setRingColor(new Color(105, 255, 105, 255));
    }

    // Placeholder for AEG_ShootOutManeuver effects
    public static void applyShootOutEffects(ShipAPI ship, CombatEngineAPI engine) {
        // Implement shoot out maneuver effects here
    }

    // Placeholder for AEG_SBFinisher effects
    public static void applyFinisherEffects(ShipAPI ship, CombatEngineAPI engine) {
        // Implement finisher effects here
    }
}
