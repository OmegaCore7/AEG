package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class AEG_4g_HelixOrbManager implements EveryFrameCombatPlugin {

    private final CombatEngineAPI engine;
    private final ShipAPI infusedShip;

    private float spawnTimer = 0f;
    private final float SPAWN_INTERVAL = 1f; // spawn orb every 1 second

    public AEG_4g_HelixOrbManager(CombatEngineAPI engine, ShipAPI infusedShip) {
        this.engine = engine;
        this.infusedShip = infusedShip;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine.isPaused() || infusedShip == null || !infusedShip.isAlive()) return;

        // Check if infusion is still active
        boolean infusionActive = engine.getCustomData().containsKey("goldion_infusion_" + infusedShip.getId());
        if (!infusionActive) {
            engine.removePlugin(this);
            return;
        }

        spawnTimer += amount;
        if (spawnTimer >= SPAWN_INTERVAL) {
            spawnTimer = 0f;
            spawnOrb();
        }
    }

    private void spawnOrb() {
        Vector2f spawnLoc = new Vector2f(infusedShip.getLocation());
        float angle = (float) Math.random() * 360f;
        int playerOwnerId = engine.getPlayerShip().getOwner(); // Get the player owner ID
        engine.addPlugin(new AEG_4g_right_helixBall(infusedShip, spawnLoc, angle, engine, playerOwnerId));
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {}
    @Override
    public void renderInUICoords(ViewportAPI viewport) {}
    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {}
    @Override
    public void init(CombatEngineAPI engine) {}

}