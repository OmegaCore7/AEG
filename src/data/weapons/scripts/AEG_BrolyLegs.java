package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class AEG_BrolyLegs implements EveryFrameWeaponEffectPlugin {

    private boolean runOnce = false, lockNloaded = false;
    private WeaponAPI shoulderL, shoulderR, armL, armR, bshellpd, bshellEc, gblasterR, gblasterL, cblaster, shellDeco, head, lssjHair, omegablaster;
    public int frame = 7;
    private IntervalUtil interval = new IntervalUtil(0.08f, 0.08f);
    private IntervalUtil interval2;
    private Color ogColor;

    public void init(WeaponAPI weapon, float amount) {
        runOnce = true;
        // Need to grab another weapon so some effects are properly rendered, like lighting from glib
        for (WeaponAPI w : weapon.getShip().getAllWeapons()) {
            switch (w.getSlot().getId()) {
                case "WS0001": // AEG_broly_shoulder_l
                    if (shoulderL == null) {
                        shoulderL = w;
                    }
                    break;
                case "WS0002": // AEG_broly_shoulder_r
                    if (shoulderR == null) {
                        shoulderR = w;
                    }
                    break;
                case "WS0003": // AEG_broly_arm_l
                    if (armL == null) {
                        armL = w;
                    }
                    break;
                case "WS0004": // AEG_broly_arm_r
                    if (armR == null) {
                        armR = w;
                    }
                    break;
                case "WS0005": // AEG_broly_bshellpd
                    if (bshellpd == null) {
                        bshellpd = w;
                    }
                    break;
                case "WS0006": // AEG_broly_bshell_ec
                    if (bshellEc == null) {
                        bshellEc = w;
                    }
                    break;
                case "WS0007": // AEG_broly_gblaster_r
                    if (gblasterR == null) {
                        gblasterR = w;
                    }
                    break;
                case "WS0008": // AEG_broly_gblaster_l
                    if (gblasterL == null) {
                        gblasterL = w;
                    }
                    break;
                case "WS0009": // AEG_broly_cblaster
                    if (cblaster == null) {
                        cblaster = w;
                    }
                    break;
                case "WS0010": // AEG_broly_shell_deco
                    if (shellDeco == null) {
                        shellDeco = w;
                    }
                    break;
                case "WS0011": // AEG_broly_head
                    if (head == null) {
                        head = w;
                    }
                    break;
                case "WS0012": // AEG_broly_lssj_hair00
                    if (lssjHair == null) {
                        lssjHair = w;
                    }
                    break;
                case "WS0013": // AEG_broly_omegablaster
                    if (omegablaster == null) {
                        omegablaster = w;
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
        SpriteAPI spr = Global.getSettings().getSprite("graphics/ships/broly/legs/Aeg_broly_legs" + frameStr + ".png");

        Color color = new Color(defColor.getRed() * 2, defColor.getGreen() * 2, defColor.getBlue() * 2, defColor.getAlpha());
        color = new Color(255, 255, 255);
        color = new Color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, (color.getAlpha() / 255f) * ship.getCombinedAlphaMult());

        MagicRender.singleframe(
                spr,
                new Vector2f(weapon.getLocation().getX(), weapon.getLocation().getY()),
                new Vector2f(spr.getWidth(), spr.getHeight()),
                ship.getFacing() - 90f,
                color,
                false,
                CombatEngineLayers.BELOW_SHIPS_LAYER
        );
    }
}