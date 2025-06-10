package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
public class AEG_4g_right_brokenmagnumEffect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private boolean runOnce = false;
    private ShipAPI ship;
    private WeaponAPI torso, shoulderR, armR;
    private float originalRArmPos = 0f;
    private final float TORSO_OFFSET = -45, RIGHT_ARM_OFFSET = -25;

    public void init() {
        runOnce = true;

        for (WeaponAPI w : ship.getAllWeapons()) {
            switch (w.getSlot().getId()) {
                case "WS0001":
                    if (torso == null) {
                        torso = w;
                    }
                    break;
                case "WS0004":
                    if (shoulderR == null) {
                        shoulderR = w;
                    }
                    break;
                case "WS0011":
                    if (armR == null) {
                        armR = w;
                        originalRArmPos = armR.getSprite().getCenterY();
                    }
                    break;
            }
        }
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ship = weapon.getShip();

        if (!runOnce) {
            init();
        }

        // Check if the right broken magnum weapon (WS0011) is selected
        if (ship.getSelectedGroupAPI().getActiveWeapon() != armR) {
            weapon.getAnimation().setFrame(2); // Set to frame 3 if the weapon is not selected
            return; // Do nothing if the right broken magnum weapon is not selected
        }

        float chargeLevel = weapon.getChargeLevel();
        float sineA = MathUtils.clamp(chargeLevel, 0f, 1f);

        // Vertical motion for the right arm
        armR.getSprite().setCenterY(originalRArmPos - (20 * sineA));

        // Torso rotation
        if (torso != null) {
            torso.setCurrAngle(ship.getFacing() - (sineA * TORSO_OFFSET));
        }

        // Shoulder rotation
        if (shoulderR != null) {
            shoulderR.setCurrAngle(torso.getCurrAngle() + MathUtils.getShortestRotation(torso.getCurrAngle(), armR.getCurrAngle()) * 0.6f);
        }

        // Frame switching logic
        if (chargeLevel > 0 && weapon.getAmmo() == 1) {
            weapon.getAnimation().setFrame(0); // Use frame 00 during charge-up and ammo = 1
        } else if (chargeLevel <= 0 && weapon.getAmmo() != 1) {
            weapon.getAnimation().setFrame(1); // Use frame 01 during charge-down and ammo != 1
        } else if (chargeLevel <= 0 && weapon.getAmmo() == 0) {
            weapon.getAnimation().setFrame(1); // Use frame 01 if charge-down has ended and ammo = 0
        } else if (chargeLevel <= 0 && weapon.getAmmo() == 1) {
            weapon.getAnimation().setFrame(0); // Return to frame 00 if charge-down has ended and ammo = 1
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        weapon.getAnimation().setFrame(1);

        ShipAPI ship = weapon.getShip();

        // Check if Goldion Mode is active
        boolean isGoldionActive = Boolean.TRUE.equals(ship.getCustomData().get("goldion_active"));
        if (!isGoldionActive) return;

        // Spawn 20 golden orbs in a multi-helix formation
        engine.addPlugin(new AEG_4g_right_helixBall(ship, weapon.getLocation(), weapon.getCurrAngle(), engine));
    }

}