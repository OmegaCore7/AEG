package data.weapons.scripts;

import com.fs.starfarer.api.AnimationAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.magiclib.util.MagicAnim;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;

public class AEG_4g_rightpunch implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin, OnHitEffectPlugin {

    private boolean runOnce = false, lockNloaded = false;
    private ShipSystemAPI system;
    private ShipAPI ship;
    private SpriteAPI sprite;
    private AnimationAPI anim, aGlow, aGlow2;
    private WeaponAPI head, armR, pauldronR, torso, wGlow, hGlow, cannon;

    private float delay = 0.1f;
    private float timer = 0;
    private int lastWeaponAmmo = 0;
    private float swingLevel = 0f;
    private float swingLevel2 = 0f;
    private boolean swinging = false;
    private float reverse = 1f;
    private boolean cooldown = false;
    private static final Vector2f ZERO = new Vector2f();
    private IntervalUtil animInterval = new IntervalUtil(0.012f, 0.012f);
    public float TURRET_OFFSET = 30f;
    private int limbInit = 0;

    private float overlap = 0, heat = 0, originalRArmPos = 0f;
    private final float TORSO_OFFSET = -45, RIGHT_ARM_OFFSET = -25, MAX_OVERLAP = 10;

    public void init() {
        runOnce = true;

        for (WeaponAPI w : ship.getAllWeapons()) {
            switch (w.getSlot().getId()) {
                case "WS0001":
                    if (torso == null) {
                        torso = w;
                        limbInit++;
                    }
                    break;
                case "WS0004":
                    if (pauldronR == null) {
                        pauldronR = w;
                        limbInit++;
                    }
                    break;
                case "WS0005":
                    if (head == null) {
                        head = w;
                        limbInit++;
                    }
                    break;
                case "WS0007":
                    if (armR == null) {
                        armR = w;
                        limbInit++;
                        originalRArmPos = armR.getSprite().getCenterY();
                    }
                    break;
            }
        }
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ship = weapon.getShip();
        sprite = ship.getSpriteAPI();
        system = ship.getSystem();
        anim = weapon.getAnimation();

        init();

        // Check if the right punch weapon (WS0007) is selected
        if (ship.getSelectedGroupAPI().getActiveWeapon() != armR) {
            return; // Do nothing if the right punch weapon is not selected
        }

        if (ship.getEngineController().isAccelerating()) {
            overlap = Math.min(MAX_OVERLAP, overlap + ((MAX_OVERLAP - overlap) * amount * 5));
        } else if (ship.getEngineController().isDecelerating() || ship.getEngineController().isAcceleratingBackwards()) {
            overlap = Math.max(-MAX_OVERLAP, overlap + ((-MAX_OVERLAP + overlap) * amount * 5));
        } else {
            overlap -= (overlap / 2) * amount * 3;
        }

        float global = ship.getFacing();
        float aim = MathUtils.getShortestRotation(global, weapon.getCurrAngle());
        float sineA = MagicAnim.smoothNormalizeRange(weapon.getChargeLevel(), 0.25f, 1f);
        float sinceG = MagicAnim.smoothNormalizeRange(weapon.getChargeLevel(), 0.0f, 0.25f) * reverse;

        if (weapon.getChargeLevel() > 0.33 && sinceG > 0) {
            reverse -= amount + 0.08;
        }
        if (weapon.getChargeLevel() <= 0) {
            reverse = 1f;
        }

        weapon.getSprite().setCenterY(originalRArmPos - (8 * sineA) + (8 * sinceG));

        // Torso Motion
        if (torso != null) {
            torso.setCurrAngle(global - (sineA * TORSO_OFFSET) - (sinceG * -TORSO_OFFSET) - aim * 0.3f);
        }

        if (weapon != null) {
            weapon.setCurrAngle(weapon.getCurrAngle() + (sineA * (TORSO_OFFSET / 7) * 0.7f) - (sinceG * TORSO_OFFSET * 0.5f));
        }

        // Adjust Right Arm and Pauldron to Follow Torso Motion
        if (armR != null && pauldronR != null) {
            armR.setCurrAngle(pauldronR.getCurrAngle());
        }
        if (pauldronR != null) {
            pauldronR.setCurrAngle(torso.getCurrAngle() + MathUtils.getShortestRotation(torso.getCurrAngle(), armR.getCurrAngle()) * 0.6f);
        }

        if (wGlow != null) {
            wGlow.setCurrAngle(weapon.getCurrAngle());
        }

        if (hGlow != null) {
            hGlow.setCurrAngle(head.getCurrAngle());
        }
    }

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        // Implement onHit logic here
    }

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        // Removed muzzle flash and particle/glow effects
    }
}