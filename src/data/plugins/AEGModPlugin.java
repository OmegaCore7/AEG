package data.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import exerelin.campaign.SectorManager;
import data.scripts.world.AEGNameGen;
import org.dark.shaders.util.ShaderLib;

public class AEGModPlugin extends BaseModPlugin {
    public static SpriteAPI ringSprite;
    @Override
    public void onApplicationLoad() throws Exception {
        ShaderLib.init();
    }

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