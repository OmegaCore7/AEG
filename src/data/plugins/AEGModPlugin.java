package data.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import exerelin.campaign.SectorManager;
import data.scripts.world.AEGNameGen;

public class AEGModPlugin extends BaseModPlugin {

    @Override
    public void onNewGame() {
        super.onNewGame();

        // The code below requires that Nexerelin is added as a library (not a dependency, it's only needed to compile the mod).
        boolean isNexerelinEnabled = Global.getSettings().getModManager().isModEnabled("nexerelin");

        if (!isNexerelinEnabled || SectorManager.getManager().isCorvusMode()) {
            new AEGNameGen().generate(Global.getSector());
        }
    }
}