package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AEG_PurgePlugin extends BaseEveryFrameCombatPlugin {
    private CombatEngineAPI engine;

    private static final String TARGET_SHIP_ID = "AEG_Incarnation";
    private static final String CORE_MODULE_VARIANT = "AEG_Incarnation_Core_Equiped";
    private static final float PURGE_THRESHOLD = 0.50f;

    private final Map<ShipAPI, Long> lastPressTime = new HashMap<>();
    private static final long DOUBLE_TAP_THRESHOLD_MS = 300;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) return;

        for (ShipAPI ship : engine.getShips()) {
            if (!TARGET_SHIP_ID.equals(ship.getHullSpec().getHullId())) continue;

            boolean doubleTapped = isKeyDoubleTapped(ship, Keyboard.getKeyIndex("X"));
            boolean lowHealth = ship.getHullLevel() < PURGE_THRESHOLD;

            if ((lowHealth || doubleTapped) && !ship.isHulk()) {
                handlePurge(ship);
            }
        }
    }

    private boolean isKeyDoubleTapped(ShipAPI ship, int key) {
        if (engine.getPlayerShip() != ship) return false;

        boolean keyPressed = Keyboard.isKeyDown(key);
        long currentTime = System.currentTimeMillis();

        if (!keyPressed) {
            lastPressTime.remove(ship);
            return false;
        }

        if (lastPressTime.containsKey(ship)) {
            long lastTime = lastPressTime.get(ship);
            if (currentTime - lastTime <= DOUBLE_TAP_THRESHOLD_MS) {
                lastPressTime.remove(ship);
                return true;
            }
        }

        lastPressTime.put(ship, currentTime);
        return false;
    }

    private void handlePurge(ShipAPI ship) {
        List<ShipAPI> childModules = ship.getChildModulesCopy();

        if (childModules == null || childModules.isEmpty()) return;

        for (ShipAPI module : childModules) {
            if (CORE_MODULE_VARIANT.equals(module.getVariant().getHullVariantId())) {
                transitionToCoreModule(ship, module);
                break;
            }
        }
    }
    private void transitionToCoreModule(ShipAPI mainShip, ShipAPI coreModule) {
        // Set up the new fleet member for the core module
        FleetMemberAPI coreFleetMember = Global.getFactory().createFleetMember(FleetMemberType.SHIP, coreModule.getVariant().clone());
        coreFleetMember.setOwner(mainShip.getOwner());
        coreFleetMember.setCaptain(mainShip.getCaptain());
        coreFleetMember.getRepairTracker().setCR(mainShip.getCurrentCR()); // Set CR from the parent ship
        coreFleetMember.getStatus().setHullFraction(coreModule.getHullLevel());

        // Spawn the core module as a new ship
        ShipAPI newCore = engine.getFleetManager(mainShip.getOwner()).spawnFleetMember(
                coreFleetMember,
                coreModule.getLocation(),
                coreModule.getFacing(),
                0f
        );

        // Restore the new ship's CR and ensure it is combat-ready
        newCore.setCRAtDeployment(mainShip.getCurrentCR()); // Ensure the new ship starts with the correct CR
        newCore.getMutableStats().getCRLossPerSecondPercent().modifyFlat("AEG_PurgePlugin", 0); // Prevent immediate CR loss

        // Make only the specified module transparent and disable it
        coreModule.setControlsLocked(true);
        coreModule.setAlphaMult(0f); // Make the module invisible
        coreModule.setCollisionClass(CollisionClass.NONE); // Prevent interactions

        // Transfer player control if applicable
        if (engine.getPlayerShip() == mainShip) {
            engine.setPlayerShipExternal(newCore);
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }
}
