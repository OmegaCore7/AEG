package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

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
            if (ship == null || ship.getHullSpec() == null) continue;
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
        // Define the variant ID of the new ship

        // Calculate spawn location 50f in front of the main ship
        float spawnDistance = 175f;
        float facingRadians = (float) Math.toRadians(mainShip.getFacing());
        float offsetX = (float) Math.cos(facingRadians) * spawnDistance;
        float offsetY = (float) Math.sin(facingRadians) * spawnDistance;
        Vector2f spawnLocation = new Vector2f(
                mainShip.getLocation().x + offsetX,
                mainShip.getLocation().y + offsetY
        );

        // Create the new fleet member directly from the variant ID
        FleetMemberAPI coreFleetMember = Global.getFactory().createFleetMember(FleetMemberType.SHIP, CORE_MODULE_VARIANT);
        coreFleetMember.setOwner(mainShip.getOwner());
        coreFleetMember.setCaptain(mainShip.getCaptain());

        // Ensure the fleet member is not mothballed
        coreFleetMember.getRepairTracker().setMothballed(false);

        // Spawn the new core ship in combat
        ShipAPI newCore = engine.getFleetManager(mainShip.getOwner()).spawnFleetMember(
                coreFleetMember,
                spawnLocation,
                mainShip.getFacing(),
                0f
        );

        // Ensure the new ship is fully operational
        newCore.setCurrentCR(1f);

        // Transfer control to the new ship if applicable
        if (engine.getPlayerShip() == mainShip) {
            engine.setPlayerShipExternal(newCore);
        }


        // No ships are locked
        mainShip.setAlphaMult(1f);
        coreModule.setAlphaMult(0f);
        coreModule.setCollisionClass(CollisionClass.NONE);
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }
}