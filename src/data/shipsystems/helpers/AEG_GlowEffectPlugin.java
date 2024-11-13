package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class AEG_GlowEffectPlugin extends BaseCombatLayeredRenderingPlugin {

    private final WeaponAPI weapon;
    private final SpriteAPI glowSprite;

    public AEG_GlowEffectPlugin(WeaponAPI weapon, String glowFilePath) {
        this.weapon = weapon;
        this.glowSprite = Global.getSettings().getSprite(glowFilePath);
    }

    @Override
    public void advance(float amount) {
        // Update logic if needed
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (layer == CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER) {
            Vector2f location = weapon.getLocation();
            glowSprite.setAngle(weapon.getCurrAngle());
            glowSprite.setSize(weapon.getSprite().getWidth(), weapon.getSprite().getHeight());
            glowSprite.setColor(new Color(105, 255, 105, 255));
            glowSprite.renderAtCenter(location.x, location.y);
        }
    }

    @Override
    public float getRenderRadius() {
        return weapon.getSprite().getWidth() / 2f;
    }
}