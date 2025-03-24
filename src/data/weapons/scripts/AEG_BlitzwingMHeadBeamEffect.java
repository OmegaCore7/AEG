package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color; // Import the Color class

public class AEG_BlitzwingMHeadBeamEffect implements BeamEffectPlugin {
    private static final float FIGHTER_DAMAGE_MULTIPLIER = 3.0f; // 3x damage against fighters, strikecraft, and missiles

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine.isPaused()) {
            return;
        }

        ShipAPI ship = beam.getWeapon().getShip();
        ShipSystemAPI system = ship.getSystem();

        if (system.getState() == SystemState.IN) {
            // Apply damage increase against fighters and missiles
            CombatEntityAPI target = beam.getDamageTarget();
            if (target instanceof ShipAPI) {
                ShipAPI shipTarget = (ShipAPI) target;
                if (shipTarget.isFighter() || shipTarget.isDrone() || target instanceof MissileAPI) {
                    shipTarget.getMutableStats().getHullDamageTakenMult().modifyMult("BlitzwingMHeadBeamEffect", FIGHTER_DAMAGE_MULTIPLIER);
                    shipTarget.getMutableStats().getArmorDamageTakenMult().modifyMult("BlitzwingMHeadBeamEffect", FIGHTER_DAMAGE_MULTIPLIER);
                    shipTarget.getMutableStats().getShieldDamageTakenMult().modifyMult("BlitzwingMHeadBeamEffect", FIGHTER_DAMAGE_MULTIPLIER);
                }
            }
        }
    }
}