package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class AEG_RMModuleMovementPlugin extends BaseEveryFrameCombatPlugin {

    private static final String SHIP_ID = "AEG_red_menace"; // Replace with your ship's ID
    private static final String MODULE_VARIANT_2 = "AEG_module_rm_cannon_standard"; // Replace with your second module variant ID
    private static final String MODULE_VARIANT_3 = "AEG_module_leftbooster_standard"; // Replace with your third module variant ID
    private static final String MODULE_VARIANT_4 = "AEG_module_rightbooster_standard"; // Replace with your fourth module variant ID
    private static final float MAX_TURN_ANGLE = 15f; // Maximum turn angle in degrees
    private static final float TURN_SPEED = 5f; // Turn speed

    private static final Vector2f LEFT_PIVOT = new Vector2f(33, 23);
    private static final Vector2f RIGHT_PIVOT = new Vector2f(-33, -23);

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }

        for (ShipAPI ship : engine.getShips()) {
            if (SHIP_ID.equals(ship.getHullSpec().getHullId())) {
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

                if (module2 != null) {
                    // Make Module 2 track targets within a 15-degree arc
                    ShipAPI target = findNearestEnemy(ship);
                    if (target != null) {
                        float angleToTarget = VectorUtils.getAngle(module2.getLocation(), target.getLocation());
                        float angleDifference = MathUtils.getShortestRotation(module2.getFacing(), angleToTarget);

                        if (Math.abs(angleDifference) <= MAX_TURN_ANGLE) {
                            module2.setFacing(MathUtils.clampAngle(module2.getFacing() + Math.signum(angleDifference) * TURN_SPEED * amount));
                        } else {
                            // Return to original position if target is beyond 15-degree arc
                            float originalAngle = ship.getFacing();
                            module2.setFacing(MathUtils.clampAngle(module2.getFacing() + Math.signum(MathUtils.getShortestRotation(module2.getFacing(), originalAngle)) * TURN_SPEED * amount));
                        }

                        Global.getLogger(this.getClass()).info("Module 2 Facing: " + module2.getFacing());
                    }
                }

                if (module3 != null && module4 != null) {
                    float turnRate = ship.getAngularVelocity();
                    float turnAngle = MAX_TURN_ANGLE * (turnRate / ship.getMaxTurnRate());

                    // Clamp the turn angle to the maximum allowed
                    turnAngle = Math.max(-MAX_TURN_ANGLE, Math.min(MAX_TURN_ANGLE, turnAngle));

                    // Apply turning to module 3 and 4
                    turnModule(module3, turnAngle, LEFT_PIVOT, ship.getLocation());
                    turnModule(module4, turnAngle, RIGHT_PIVOT, ship.getLocation());

                    // Synchronize module engines with the ship's engines
                    synchronizeEngines(module3, ship);
                    synchronizeEngines(module4, ship);

                    Global.getLogger(this.getClass()).info("Module 3 Facing: " + module3.getFacing());
                    Global.getLogger(this.getClass()).info("Module 4 Facing: " + module4.getFacing());
                }
            }
        }
    }

    private ShipAPI findNearestEnemy(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        ShipAPI nearestEnemy = null;
        float nearestDistance = Float.MAX_VALUE;

        for (ShipAPI enemy : engine.getShips()) {
            if (enemy.getOwner() != ship.getOwner() && enemy.isAlive()) {
                float distance = MathUtils.getDistance(ship, enemy);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestEnemy = enemy;
                }
            }
        }

        return nearestEnemy;
    }

    private void turnModule(ShipAPI module, float angle, Vector2f pivot, Vector2f shipLocation) {
        if (module.isAlive()) {
            float cos = (float) Math.cos(Math.toRadians(angle));
            float sin = (float) Math.sin(Math.toRadians(angle));

            Vector2f relativePivot = new Vector2f(pivot);
            Vector2f.add(relativePivot, shipLocation, relativePivot);

            Vector2f offset = new Vector2f(module.getLocation());
            Vector2f.sub(offset, relativePivot, offset);

            float newX = offset.x * cos - offset.y * sin + relativePivot.x;
            float newY = offset.x * sin + offset.y * cos + relativePivot.y;

            module.setFacing(module.getFacing() + angle);
            module.getLocation().set(newX, newY);
        }
    }

    private void synchronizeEngines(ShipAPI module, ShipAPI ship) {
        if (module.isAlive()) {
            ShipEngineControllerAPI shipEngines = ship.getEngineController();
            ShipEngineControllerAPI moduleEngines = module.getEngineController();

            if (shipEngines != null && moduleEngines != null) {
                if (shipEngines.isAccelerating()) {
                    module.giveCommand(ShipCommand.ACCELERATE, null, 0);
                }
                if (shipEngines.isAcceleratingBackwards()) {
                    module.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null, 0);
                }
                if (shipEngines.isDecelerating()) {
                    module.giveCommand(ShipCommand.DECELERATE, null, 0);
                }
                if (shipEngines.isStrafingLeft()) {
                    module.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
                }
                if (shipEngines.isStrafingRight()) {
                    module.giveCommand(ShipCommand.STRAFE_RIGHT, null, 0);
                }
                if (shipEngines.isTurningLeft()) {
                    module.giveCommand(ShipCommand.TURN_LEFT, null, 0);
                }
                if (shipEngines.isTurningRight()) {
                    module.giveCommand(ShipCommand.TURN_RIGHT, null, 0);
                }
                if (shipEngines.isFlamingOut() || shipEngines.isFlamedOut()) {
                    moduleEngines.forceFlameout(true);
                    if ( ship.getSystem().isActive()) {
                        moduleEngines.forceShowAccelerating();
                        moduleEngines.getFlameColorShifter();
                    }
                }
            }
        }
    }
}
