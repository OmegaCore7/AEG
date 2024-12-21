package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class AEG_IncarnationModulePlugin extends BaseEveryFrameCombatPlugin {

    private static final String SHIP_ID = "AEG_Incarnation"; // New ship ID
    private static final String MODULE_VARIANT_CENTER = "AEG_module_incarnation_rear_equiped"; // Rear engine module
    private static final String MODULE_VARIANT_LEFT = "AEG_module_incarnaton_left_equiped"; // Left booster
    private static final String MODULE_VARIANT_RIGHT = "AEG_module_incarnaton_right_equiped"; // Right booster
    private static final float MAX_TURN_ANGLE = 15f; // Maximum turn angle in degrees
    private static final float TURN_SPEED = 5f; // Turn speed
    private static final float TURN_SMOOTHING = 0.2f; // Adjust for smoother turns

    private static final Vector2f LEFT_PIVOT = new Vector2f(33, 23);
    private static final Vector2f RIGHT_PIVOT = new Vector2f(-33, -23);
    private static final Vector2f CENTER_PIVOT = new Vector2f(0, -40); // Center rear pivot

    private float lastTurnAngle = 0; // Store the last turn angle for smoothing

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }

        for (ShipAPI ship : engine.getShips()) {
            if (SHIP_ID.equals(ship.getHullSpec().getHullId())) {
                ShipAPI centerModule = null;
                ShipAPI leftModule = null;
                ShipAPI rightModule = null;

                for (ShipAPI module : ship.getChildModulesCopy()) {
                    if (MODULE_VARIANT_CENTER.equals(module.getVariant().getHullVariantId())) {
                        centerModule = module;
                    } else if (MODULE_VARIANT_LEFT.equals(module.getVariant().getHullVariantId())) {
                        leftModule = module;
                    } else if (MODULE_VARIANT_RIGHT.equals(module.getVariant().getHullVariantId())) {
                        rightModule = module;
                    }
                }

                if (centerModule != null) {
                    float turnRate = ship.getAngularVelocity();
                    float turnAngle = MAX_TURN_ANGLE * (turnRate / ship.getMaxTurnRate());

                    // Clamp the turn angle to the maximum allowed
                    turnAngle = Math.max(-MAX_TURN_ANGLE, Math.min(MAX_TURN_ANGLE, turnAngle));

                    // Smooth the turn rate to reduce jitteriness
                    float smoothedAngle = lastTurnAngle + TURN_SMOOTHING * (turnAngle - lastTurnAngle);
                    lastTurnAngle = smoothedAngle;

                    // Apply turning to the center module
                    turnModule(centerModule, smoothedAngle, CENTER_PIVOT, ship.getLocation());

                    synchronizeEngines(centerModule, ship);
                    synchronizeModuleSystem(centerModule, ship); // Synchronize system activation
                }

                if (leftModule != null && rightModule != null) {
                    float turnRate = ship.getAngularVelocity();
                    float turnAngle = MAX_TURN_ANGLE * (turnRate / ship.getMaxTurnRate());

                    // Clamp the turn angle to the maximum allowed
                    turnAngle = Math.max(-MAX_TURN_ANGLE, Math.min(MAX_TURN_ANGLE, turnAngle));

                    // Smooth the turn rate to reduce jitteriness
                    float smoothedAngle = lastTurnAngle + TURN_SMOOTHING * (turnAngle - lastTurnAngle);
                    lastTurnAngle = smoothedAngle;

                    // Apply turning to left and right modules
                    turnModule(leftModule, smoothedAngle, LEFT_PIVOT, ship.getLocation());
                    turnModule(rightModule, smoothedAngle, RIGHT_PIVOT, ship.getLocation());

                    synchronizeEngines(leftModule, ship);
                    synchronizeEngines(rightModule, ship);

                    synchronizeModuleSystem(leftModule, ship); // Synchronize system activation for left module
                    synchronizeModuleSystem(rightModule, ship); // Synchronize system activation for right module

                    Global.getLogger(this.getClass()).info("Left Module Facing: " + leftModule.getFacing());
                    Global.getLogger(this.getClass()).info("Right Module Facing: " + rightModule.getFacing());
                }
            }
        }
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
                // Synchronize the engines of the modules with the parent ship's engines
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
                }
            }
        }
    }

    private void synchronizeModuleSystem(ShipAPI module, ShipAPI ship) {
        if (module.isAlive()) {
            boolean isParentSystemActive = ship.getSystem().isActive();

            // Activate the module's system if the parent system is active
            if (isParentSystemActive) {
                if (!module.getSystem().isActive()) {
                    module.useSystem();
                }
            } else {
                if (module.getSystem().isActive()) {
                    module.getSystem().deactivate();
                }
            }
        }
    }
}
