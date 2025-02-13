package data.weapons.scripts;

import com.fs.starfarer.api.AnimationAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.magiclib.util.MagicAnim;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicFakeBeam;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;

public class AEG_4g_right_willknifeEffect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin, OnHitEffectPlugin {

    private boolean runOnce = false, lockNloaded = false;
    private ShipSystemAPI system;
    private ShipAPI ship;
    private SpriteAPI sprite;
    private AnimationAPI anim, aGlow, aGlow2;
    private WeaponAPI head, armL, armR, pauldronL, pauldronR, torso, wGlow, hGlow, cannon;

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
    private static final Color MUZZLE_FLASH_COLOR = new Color(111,250,236,50);
    private static final Color MUZZLE_FLASH_COLOR_ALT = new Color(111,250,236,100);
    private static final Color MUZZLE_FLASH_COLOR_GLOW = new Color(255, 255, 255, 50);
    private static final float MUZZLE_FLASH_DURATION = 0.15f;
    private static final float MUZZLE_FLASH_SIZE = 10.0f;

    private float overlap = 0, heat = 0, originalRArmPos = 0f;
    private final float TORSO_OFFSET = -45, LEFT_ARM_OFFSET = -60, RIGHT_ARM_OFFSET = -25, MAX_OVERLAP = 10;

    private float pauseTimer = 0f;
    private boolean isPaused = false;
    private static final float PAUSE_DURATION = 0.33f;

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
                case "WS0008":
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

        if (isPaused) {
            pauseTimer += amount;
            if (pauseTimer >= PAUSE_DURATION) {
                isPaused = false;
                pauseTimer = 0f;
            }
            return; // Skip the rest of the advance method while paused
        }

        if (weapon.getChargeLevel() >= 0.25f && weapon.getChargeLevel() < 0.25f + amount) {
            isPaused = true;
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

        // Reversed Torso Motion
        if (torso != null) {
            torso.setCurrAngle(global - (sineA * TORSO_OFFSET) - (sinceG * -TORSO_OFFSET) - aim * 0.3f);
        }

        if (weapon != null) {
            weapon.setCurrAngle(weapon.getCurrAngle() + (sineA * (TORSO_OFFSET / 7) * 0.7f) - (sinceG * TORSO_OFFSET * 0.5f));
        }

        // Adjust Right Arm and Pauldron to Follow Reversed Torso Motion
        if (armR != null && pauldronR != null) {
            armR.setCurrAngle(pauldronR.getCurrAngle());
        }
        if (pauldronR != null) {
            pauldronR.setCurrAngle(torso.getCurrAngle() + MathUtils.getShortestRotation(torso.getCurrAngle(), armR.getCurrAngle()) * 0.6f);
        }

        // Adjust Left Arm and Pauldron to Follow Reversed Torso Motion
        if (armL != null) {
            armL.setCurrAngle(global - ((aim + LEFT_ARM_OFFSET) * sineA) - ((overlap + aim * 0.25f) * (1 - sineA)));
        }

        if (pauldronL != null) {
            pauldronL.setCurrAngle(torso.getCurrAngle() - MathUtils.getShortestRotation(torso.getCurrAngle(), armL.getCurrAngle()) * 0.6f);
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
        Vector2f origin = new Vector2f(weapon.getLocation());
        Vector2f offset = new Vector2f(TURRET_OFFSET, -15f);
        VectorUtils.rotate(offset, weapon.getCurrAngle(), offset);
        Vector2f.add(offset, origin, origin);
        float shipFacing = weapon.getCurrAngle();
        Vector2f shipVelocity = weapon.getShip().getVelocity();
        shipVelocity = MathUtils.getPointOnCircumference(ship.getVelocity(), (float) Math.random() * 20f, weapon.getCurrAngle() + 90f - (float) Math.random() * 180f);

        // Trigger particle and glow effects immediately
        engine.spawnExplosion(origin, shipVelocity, MUZZLE_FLASH_COLOR, MUZZLE_FLASH_SIZE, MUZZLE_FLASH_DURATION);
        engine.addSmoothParticle(origin, shipVelocity, MUZZLE_FLASH_SIZE * 3f, 1f, MUZZLE_FLASH_DURATION * 2f, MUZZLE_FLASH_COLOR_GLOW);

        // Alternate color for variety
        if (Math.random() > 0.75) {
            engine.spawnExplosion(origin, shipVelocity, MUZZLE_FLASH_COLOR_ALT, MUZZLE_FLASH_SIZE * 0.5f, MUZZLE_FLASH_DURATION);
        }
    }
}