package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class AEG_4g_legs implements EveryFrameWeaponEffectPlugin {

    private boolean runOnce = false, lockNloaded = false;
    private WeaponAPI shoulderL, shoulderR, armL, armR, bshellpd, bshellEc, gblasterR, gblasterL, cblaster, shellDeco, head, lssjHair, omegablaster;
    public int frame = 7;
    private IntervalUtil interval = new IntervalUtil(0.08f, 0.08f);
    private IntervalUtil interval2;
    private Color ogColor;
    private float chargeUpTime = 0;
    private static final float CHARGE_UP_DURATION = 4.0f;
    private boolean colorChanged = false;

    public void init(WeaponAPI weapon, float amount) {
        runOnce = true;
        // Need to grab another weapon so some effects are properly rendered, like lighting from glib
        for (WeaponAPI w : weapon.getShip().getAllWeapons()) {
            switch (w.getSlot().getId()) {
                case "WS0001": // AEG_4g_torso
                    if (shoulderL == null) {
                        shoulderL = w;
                    }
                    break;
                case "WS0002": // AEG_4g_legs
                    if (shoulderR == null) {
                        shoulderR = w;
                    }
                    break;
                case "WS0003": // AEG_4g_left_shoulder
                    if (armL == null) {
                        armL = w;
                    }
                    break;
                case "WS0004": // AEG_4g_right_shoulder
                    if (armR == null) {
                        armR = w;
                    }
                    break;
                case "WS0005": // AEG_4g_head
                    if (bshellpd == null) {
                        bshellpd = w;
                    }
                    break;
                case "WS0006": // AEG_4g_left_punch
                    if (bshellEc == null) {
                        bshellEc = w;
                    }
                    break;
                case "WS0007": // AEG_4g_right_punch
                    if (gblasterR == null) {
                        gblasterR = w;
                    }
                    break;
            }
        }
        ogColor = new Color(weapon.getSprite().getColor().getRed() / 255f, weapon.getSprite().getColor().getGreen() / 255f, weapon.getSprite().getColor().getBlue() / 255f);
        // Interval to prevent legs being drawn constantly due to time dilation
        interval2 = new IntervalUtil(amount, amount);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        weapon.getSprite().setColor(new Color(0f, 0f, 0f, 0f));
        if (ship.getOwner() == -1) {
            weapon.getAnimation().setFrame(6);
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
        Color defColor = ship.getSpriteAPI().getAverageColor();

        if (ship.getSystem().isActive()) {
            chargeUpTime += amount;
            if (chargeUpTime >= CHARGE_UP_DURATION && !colorChanged) {
                colorChanged = true;
            }
        } else {
            chargeUpTime = 0;
            colorChanged = false;
        }

        if (isRightKneeDrillActive(ship)) {
            renderFrame(weapon, "graphics/ships/4g/legs/4g_legs_06.png", colorChanged ? Color.GREEN : Color.WHITE);
            return;
        }

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
        String frameStr = "0" + frame;
        if (frame >= 10) {
            frameStr = String.valueOf(frame);
        }
        String spritePath = "graphics/ships/4g/legs/4g_legs_" + frameStr + ".png";
        Color renderColor = colorChanged ? Color.GREEN : Color.WHITE;

        renderFrame(weapon, spritePath, renderColor);
    }

    private void renderFrame(WeaponAPI weapon, String spritePath, Color color) {
        SpriteAPI spr = Global.getSettings().getSprite(spritePath);
        MagicRender.singleframe(
                spr,
                new Vector2f(weapon.getLocation().getX(), weapon.getLocation().getY()),
                new Vector2f(spr.getWidth(), spr.getHeight()),
                weapon.getShip().getFacing() - 90f,
                color,
                false,
                CombatEngineLayers.BELOW_SHIPS_LAYER
        );
    }

    private boolean isRightKneeDrillActive(ShipAPI ship) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if ("AEG_4g_rightkneedrill".equals(weapon.getId()) && weapon.isFiring()) {
                return true;
            }
        }
        return false;
    }
}