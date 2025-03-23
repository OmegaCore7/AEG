package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class AEG_Blitzwing_legs implements EveryFrameWeaponEffectPlugin {

    private boolean runOnce = false;
    private WeaponAPI chestArmor, RarmNullrayBlaster, jetpack, LarmMinigun, mhead;
    public int frame = 5;
    private IntervalUtil interval = new IntervalUtil(0.08f, 0.08f);
    private IntervalUtil interval2;

    public void init(WeaponAPI weapon, float amount) {
        runOnce = true;
        // Need to grab another weapon so some effects are properly rendered
        for (WeaponAPI w : weapon.getShip().getAllWeapons()) {
            switch (w.getSlot().getId()) {
                case "WS0001": // AEG_bw_chestarmor
                    if (chestArmor == null) {
                        chestArmor = w;
                    }
                    break;
                case "WS0002": // AEG_bw_Rarm_nullrayblaster
                    if (RarmNullrayBlaster == null) {
                        RarmNullrayBlaster = w;
                    }
                    break;
                case "WS0003": // AEG_Blitzwing_Jetpack
                    if (jetpack == null) {
                        jetpack = w;
                    }
                    break;
                case "WS0004": // AEG_bw_Larm_minigun
                    if (LarmMinigun == null) {
                        LarmMinigun = w;
                    }
                    break;
                case "WS0006": // AEG_bw_mhead
                    if (mhead == null) {
                        mhead = w;
                    }
                    break;
                case "WS0010": // AEG_Blitzwing_legs
                    if (weapon == null) {
                        weapon = w;
                    }
                    break;
            }
        }
        // Interval to prevent legs being drawn constantly due to time dilation
        interval2 = new IntervalUtil(amount, amount);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        weapon.getSprite().setColor(new Color(0f, 0f, 0f, 0f));
        if (ship.getOwner() == -1) {
            weapon.getAnimation().setFrame(5);
            return;
        }

        if (!Global.getCombatEngine().isEntityInPlay(weapon.getShip())) {
            return;
        }

        if (!MagicRender.screenCheck(0.1f, weapon.getLocation()) || Global.getCombatEngine().isPaused()) {
            return;
        }

        if (!runOnce) {
            init(weapon, amount);
        }

        if (ship.getSystem().isActive()) {
            interval.advance(amount);
            if (ship.getEngineController().isAcceleratingBackwards() || ship.getEngineController().isDecelerating()) {
                if (interval.intervalElapsed()) {
                    if (frame != 0) {
                        frame--;
                    }
                    if (frame < 0) {
                        frame = 0;
                    }
                }
            } else if (ship.getEngineController().isAccelerating()) {
                if (interval.intervalElapsed()) {
                    if (frame != 11) {
                        frame++;
                    }
                    if (frame > 11) {
                        frame = 11;
                    }
                }
            } else {
                if (interval.intervalElapsed()) {
                    if (frame > 6) {
                        frame--;
                    } else if (frame != 6) {
                        frame++;
                    }
                }
            }
        } else {
            frame = 5; // Keep legs hidden in plane mode
        }

        String frameStr = "0" + frame;
        if (frame >= 10) {
            frameStr = String.valueOf(frame);
        }
        String spritePath = "graphics/ships/blitzwing/legs/Blitzwing_legs_" + frameStr + ".png";

        renderFrame(weapon, spritePath);
    }

    private void renderFrame(WeaponAPI weapon, String spritePath) {
        SpriteAPI spr = Global.getSettings().getSprite(spritePath);
        MagicRender.singleframe(
                spr,
                new Vector2f(weapon.getLocation().getX(), weapon.getLocation().getY()),
                new Vector2f(spr.getWidth(), spr.getHeight()),
                weapon.getShip().getFacing() - 90f,
                Color.WHITE,
                false,
                CombatEngineLayers.BELOW_SHIPS_LAYER
        );
    }
}