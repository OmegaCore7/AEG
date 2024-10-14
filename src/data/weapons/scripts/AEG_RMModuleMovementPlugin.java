package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class AEG_RMModuleMovementPlugin extends BaseEveryFrameCombatPlugin {

    private static final String SHIP_ID = "AEG_red_menace"; // Replace with your ship's ID
    private static final String WEAPON_SLOT_1 = "WS0008"; // Replace with your first weapon slot ID
    private static final String MODULE_VARIANT_2 = "AEG_module_rm_cannon_standard"; // Replace with your second module variant ID
    private static final String MODULE_VARIANT_3 = "AEG_module_leftbooster_standard"; // Replace with your third module variant ID
    private static final String MODULE_VARIANT_4 = "AEG_module_rightbooster_standard"; // Replace with your fourth module variant ID
    private static final float SWAY_ARC = 25f; // Customizable arc for swaying

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }

        for (ShipAPI ship : engine.getShips()) {
            if (SHIP_ID.equals(ship.getHullSpec().getHullId())) {
                WeaponAPI weapon1 = null;
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    if (WEAPON_SLOT_1.equals(weapon.getSlot().getId())) {
                        weapon1 = weapon;
                        break;
                    }
                }

                ShipAPI module2 = null;
                ShipAPI module3 = null;
                ShipAPI module4 = null;
                for (ShipAPI module : ship.getChildModulesCopy()) {
                    if (MODULE_VARIANT_2.equals(module.getVariant().getHullVariantId())) {
                        module2 = module;
                    } else if (MODULE_VARIANT_3.equals(module.getVariant().getHullVariantId())) {
                        module3 = module;
                    } else if (MODULE_VARIANT_4.equals(module.getVariant().getHullVariantId())) {
                        module4 = module;
                    }
                }

                if (weapon1 != null) {
                    float weapon1Angle = weapon1.getCurrAngle();
                    Global.getLogger(this.getClass()).info("Weapon 1 Angle: " + weapon1Angle);

                    // Set the angle of weapon 2 to match the angle of weapon 1
                    if (module2 != null) {
                        for (WeaponAPI weapon : module2.getAllWeapons()) {
                            weapon.setCurrAngle(weapon1Angle);
                            Global.getLogger(this.getClass()).info("Module 2 Weapon Angle set to: " + weapon1Angle);
                        }
                    }
                }

                if (module3 != null && module4 != null) {
                    float turnRate = ship.getAngularVelocity();
                    float swayAngle = SWAY_ARC * (turnRate / ship.getMaxTurnRate());

                    // Apply curved movement to module 3 and 4
                    module3.setFacing(module3.getFacing() + swayAngle);
                    module4.setFacing(module4.getFacing() + swayAngle);

                    // Coordinate module engines with the ship's engines
                    Vector2f shipVelocity = ship.getVelocity();
                    module3.getVelocity().set(shipVelocity);
                    module4.getVelocity().set(shipVelocity);

                    Global.getLogger(this.getClass()).info("Module 3 Facing: " + module3.getFacing());
                    Global.getLogger(this.getClass()).info("Module 4 Facing: " + module4.getFacing());
                }
            }
        }
    }
}
