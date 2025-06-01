package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicAnim;
import org.lwjgl.input.Keyboard;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
public class AEG_4g_GoldionCrusherEffect implements EveryFrameWeaponEffectPlugin {
    private static final Color IMPACT_COLOR = new Color(255, 80, 0, 255);
    private static final float IMPACT_RADIUS = 500f;
    private static final float HAMMER_LENGTH = 800f;
    private boolean hasSwung = false;
    private enum HammerState { IDLE, WINDUP, HOLD, SWING }
    private HammerState hammerState = HammerState.IDLE;
    private float swingTimer = 0f;
    private boolean initialized = false;
    private WeaponAPI weapon;
    private ShipAPI ship;

    // Limb weapon slots
    private WeaponAPI head, armL, armR, pauldronL, pauldronR, torso, wGlow, hGlow;
    private float originalRArmPos = 0f;
    private float overlap = 0f;

    private final float TORSO_OFFSET = -45f, LEFT_ARM_OFFSET = -60f, MAX_OVERLAP = 10f;

    private float pauseTimer = 0f;
    private boolean isPaused = false;
    private static final float PAUSE_DURATION = 0.33f;

    private float hammerCharge = 0f;
    private float chargeLevel = 0f;
    private float maxChargeTime = 1.5f;
    private float reverse = 1f;
    private void initLimbs() {
        if (ship == null) return;
        for (WeaponAPI w : ship.getAllWeapons()) {
            switch (w.getSlot().getId()) {
                case "WS0001":
                    torso = w;
                    break;
                case "WS0004":
                    pauldronR = w;
                    break;
                case "WS0005":
                    head = w;
                    break;
                case "WS0008":
                    armR = w;
                    originalRArmPos = armR.getSprite().getCenterY();
                    break;
                case "WS0006":
                    wGlow = w;
                    break;
                case "WS0003":
                    pauldronL = w;
                    break;
            }
        }
    }
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        this.weapon = weapon;
        this.ship = weapon.getShip();

        if (!initialized) {
            initLimbs();
            initialized = true;
        }

        // Prevent animation unless selected
        if (ship.getSelectedGroupAPI() == null || ship.getSelectedGroupAPI().getActiveWeapon() != weapon) {
            return;
        }
        animateSwing(amount);
        // === Animation frame control based on state ===
        if (ship != null && ship.getSelectedGroupAPI() != null) {
            boolean isSelected = ship.getSelectedGroupAPI().getActiveWeapon() == weapon;
            boolean isIdle = chargeLevel <= 0f;
            boolean isFiring = weapon.isFiring();

            if (isSelected && isIdle) {
                weapon.getAnimation().setFrame(1); // Idle but selected
            } else if (isFiring) {
                weapon.getAnimation().setFrame(1); // Charging/firing
            } else if (!isSelected) {
                weapon.getAnimation().setFrame(0); // Not selected
            }
        }
        if (Global.getCombatEngine().isPaused()) return;

        if (isPaused) {
            pauseTimer += amount;
            if (pauseTimer >= PAUSE_DURATION) {
                isPaused = false;
                pauseTimer = 0f;
            }
            return;
        }

        if (weapon.getChargeLevel() >= 0.25f && weapon.getChargeLevel() < 0.25f + amount) {
            isPaused = true;
            // Store Shift key status at firing moment
            boolean shiftPressed = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            weapon.getShip().setCustomData("AEG_goldion_shiftFired", shiftPressed);
        }

        // Handle charging logic
        if (weapon.isFiring()) {
            hammerCharge += amount;
            chargeLevel = Math.min(hammerCharge / maxChargeTime, 1f);
        } else {
            hammerCharge = 0f;
            chargeLevel = 0f;
            reverse = 1f;
        }

    }
    private void animateSwing(float amount) {
        boolean shiftHeld = false;
        if (ship.getCustomData().containsKey("AEG_goldion_shiftFired")) {
            shiftHeld = (Boolean) ship.getCustomData().get("AEG_goldion_shiftFired");
        }

        float global = ship.getFacing();
        float aim = MathUtils.getShortestRotation(global, weapon.getCurrAngle());

        // Common values
        float sineA = MagicAnim.smoothNormalizeRange(chargeLevel, 0.25f, 1f);
        float sinceG = MagicAnim.smoothNormalizeRange(chargeLevel, 0.0f, 0.25f) * reverse;

        // ====== CRUSHER MODE: SHIFT NOT HELD ======
        if (!shiftHeld) {
        // Adjust swing direction correction
        if (chargeLevel > 0.33f && sinceG > 0) {
            reverse -= amount + 0.08f;
        }
        if (chargeLevel <= 0) {
            reverse = 1f;
        }

        // Overlap effect based on ship movement
        if (ship.getEngineController().isAccelerating()) {
            overlap = Math.min(MAX_OVERLAP, overlap + ((MAX_OVERLAP - overlap) * amount * 5));
        } else if (ship.getEngineController().isDecelerating() || ship.getEngineController().isAcceleratingBackwards()) {
            overlap = Math.max(-MAX_OVERLAP, overlap + ((-MAX_OVERLAP + overlap) * amount * 5));
        } else {
            overlap -= (overlap / 2) * amount * 3;
        }

        // Right arm vertical animation (pull back and swing)
        if (armR != null) {
            armR.getSprite().setCenterY(originalRArmPos + (8 * sineA) - (8 * sinceG));
        }

        // Torso rotation for swing motion
        if (torso != null) {
            torso.setCurrAngle(global - (sineA * TORSO_OFFSET) + (sinceG * TORSO_OFFSET) + aim * 0.3f);
        }

        // Weapon (hammer) angle swing logic
        if (weapon != null) {
            weapon.setCurrAngle(weapon.getCurrAngle()
                    + (sineA * (TORSO_OFFSET / 7) * 0.7f)
                    - (sinceG * TORSO_OFFSET * 0.5f));
        }

        // Sync arm and shoulder
        if (armR != null && pauldronR != null) {
            armR.setCurrAngle(pauldronR.getCurrAngle());
        }
        if (pauldronR != null && torso != null && armR != null) {
            pauldronR.setCurrAngle(torso.getCurrAngle()
                    + MathUtils.getShortestRotation(torso.getCurrAngle(), armR.getCurrAngle()) * 0.6f);
        }
        // Left arm animation
        if (armL != null) {
            armL.setCurrAngle(global - ((aim + LEFT_ARM_OFFSET) * sineA) - ((overlap + aim * 0.25f) * (1 - sineA)));
        }

        // Left pauldron mirrors right pauldron's relative motion
        if (pauldronL != null) {
            pauldronL.setCurrAngle(torso.getCurrAngle()
                    + MathUtils.getShortestRotation(torso.getCurrAngle(), armR.getCurrAngle()) * 0.6f);
        }
        if (wGlow != null) {
            wGlow.setCurrAngle(weapon.getCurrAngle());
        }
        // Head glow tracking
        if (hGlow != null && head != null) {
            hGlow.setCurrAngle(head.getCurrAngle());
        }
        // Trigger FX once at swing peak
        if (chargeLevel >= 1f && !hasSwung) {
            hasSwung = true;

            // Calculate hammer tip position based on ship location, hammer length, and current weapon angle
            Vector2f tip = MathUtils.getPointOnCircumference(ship.getLocation(), HAMMER_LENGTH, weapon.getCurrAngle());
            Vector2f vel = ship.getVelocity();
        }
        }else{
        }

        // Reset for next swing cycle
        if (chargeLevel < 1f) {
            hasSwung = false;
        }
    }

}

