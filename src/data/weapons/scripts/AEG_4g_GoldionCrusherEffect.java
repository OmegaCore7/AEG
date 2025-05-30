package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicAnim;

public class AEG_4g_GoldionCrusherEffect implements EveryFrameWeaponEffectPlugin {

    private boolean initialized = false;
    private WeaponAPI weapon;
    private ShipAPI ship;

    // Limb weapon slots
    private WeaponAPI head, armL, armR, pauldronL, pauldronR, torso, wGlow, hGlow;
    private float originalRArmPos = 0f;
    private float reverse = 1f;
    private float overlap = 0f;

    private final float TORSO_OFFSET = -45f, LEFT_ARM_OFFSET = -60f, RIGHT_ARM_OFFSET = -25f, MAX_OVERLAP = 10f;

    private float pauseTimer = 0f;
    private boolean isPaused = false;
    private static final float PAUSE_DURATION = 0.33f;

    private float hammerCharge = 0f;
    private float chargeLevel = 0f;
    private float maxChargeTime = 1.5f;

    private boolean impactTriggered = false;

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
                case "WS0003":
                    armL = w;
                    break;
                case "WS0002":
                    pauldronL = w;
                    break;
                case "WS0006":
                    wGlow = w;
                    break;
                case "WS0007":
                    hGlow = w;
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

        animateSwing(amount);
    }

    private void animateSwing(float amount) {
        float global = ship.getFacing();
        float aim = MathUtils.getShortestRotation(global, weapon.getCurrAngle());

        float sineA = MagicAnim.smoothNormalizeRange(chargeLevel, 0.25f, 1f);
        float sinceG = MagicAnim.smoothNormalizeRange(chargeLevel, 0.0f, 0.25f) * reverse;

        if (chargeLevel > 0.33f && sinceG > 0) {
            reverse -= amount + 0.08f;
        }
        if (chargeLevel <= 0) {
            reverse = 1f;
        }

        if (ship.getEngineController().isAccelerating()) {
            overlap = Math.min(MAX_OVERLAP, overlap + ((MAX_OVERLAP - overlap) * amount * 5));
        } else if (ship.getEngineController().isDecelerating() || ship.getEngineController().isAcceleratingBackwards()) {
            overlap = Math.max(-MAX_OVERLAP, overlap + ((-MAX_OVERLAP + overlap) * amount * 5));
        } else {
            overlap -= (overlap / 2) * amount * 3;
        }

        if (armR != null) {
            armR.getSprite().setCenterY(originalRArmPos - (8 * sineA) + (8 * sinceG));
        }

        if (torso != null) {
            torso.setCurrAngle(global - (sineA * TORSO_OFFSET) - (sinceG * -TORSO_OFFSET) - aim * 0.3f);
        }

        if (weapon != null) {
            weapon.setCurrAngle(weapon.getCurrAngle() + (sineA * (TORSO_OFFSET / 7) * 0.7f) - (sinceG * TORSO_OFFSET * 0.5f));
        }

        if (armR != null && pauldronR != null) {
            armR.setCurrAngle(pauldronR.getCurrAngle());
        }
        if (pauldronR != null) {
            pauldronR.setCurrAngle(torso.getCurrAngle() + MathUtils.getShortestRotation(torso.getCurrAngle(), armR.getCurrAngle()) * 0.6f);
        }

        if (armL != null) {
            armL.setCurrAngle(global - ((aim + LEFT_ARM_OFFSET) * sineA) - ((overlap + aim * 0.25f) * (1 - sineA)));
        }

        if (pauldronL != null) {
            pauldronL.setCurrAngle(torso.getCurrAngle() - MathUtils.getShortestRotation(torso.getCurrAngle(), armL.getCurrAngle()) * 0.6f);
        }

        if (wGlow != null) {
            wGlow.setCurrAngle(weapon.getCurrAngle());
        }

        if (hGlow != null && head != null) {
            hGlow.setCurrAngle(head.getCurrAngle());
        }
    }
}
