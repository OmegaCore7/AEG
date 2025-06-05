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
    //Hammer Render
    private static final Color NEBULA_COLOR = new Color(255, 100, 0, 100);
    private static final Color GLOW_COLOR = new Color(255, 180, 60, 255);
    private static final float HAMMER_WIDTH = 300f;
    private static final float HAMMER_HEIGHT = 600f;
    private static final int NEBULA_PARTICLES = 50;
    private static final int GLOW_ORBS = 10;
    //Animation
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

        if (!Global.getCombatEngine().isPaused() &&
                ship.getSelectedGroupAPI() != null &&
                ship.getSelectedGroupAPI().getActiveWeapon() == weapon) {

            // Use weapon location as base (or fireOffset if needed)
            Vector2f origin = weapon.getLocation();
            float angle = weapon.getCurrAngle();  // includes ship facing + mouse aim

            renderHammer(engine, weapon, chargeLevel);
        }


        animateSwing(amount);
        // === Animation frame control based on state ===
        if (ship != null && ship.getSelectedGroupAPI() != null) {
            boolean isSelected = ship.getSelectedGroupAPI().getActiveWeapon() == weapon;
            boolean isIdle = chargeLevel <= 0f;
            boolean isFiring = weapon.isFiring();

                // Crusher Mode: Keep using existing logic
                if (isSelected && isIdle) {
                    weapon.getAnimation().setFrame(1);
                } else if (isFiring) {
                    weapon.getAnimation().setFrame(1);
                } else {
                    weapon.getAnimation().setFrame(0);
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


        float global = ship.getFacing();
        float aim = MathUtils.getShortestRotation(global, weapon.getCurrAngle());

        // Common values
        float sineA = MagicAnim.smoothNormalizeRange(chargeLevel, 0.25f, 1f);
        float sinceG = MagicAnim.smoothNormalizeRange(chargeLevel, 0.0f, 0.25f) * reverse;


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

            // Step 1: Torso rotation for swing motion
            if (torso != null) {
                torso.setCurrAngle(global - (sineA * TORSO_OFFSET) + (sinceG * TORSO_OFFSET) + aim * 0.3f);
            }

            // Step 2: Pauldron follows torso
            if (pauldronR != null && torso != null) {
                pauldronR.setCurrAngle(torso.getCurrAngle() + (TORSO_OFFSET * 0.3f * sineA)); // Example multiplier
            }

            // Step 3: Arm follows pauldron, with offset if desired
        if (armR != null && pauldronR != null) {
            float desiredArmAngle = pauldronR.getCurrAngle();

            float minAngle = global - 90f;
            float maxAngle = global + 10f;

            float clampedArmAngle = clampAngle(desiredArmAngle, minAngle, maxAngle);

            boolean isClamped = clampedArmAngle != desiredArmAngle;
            armR.setCurrAngle(clampedArmAngle);

            if (isClamped) {
                // Raise the arm when it hits forward limit to simulate "extension"
                armR.getSprite().setCenterY(originalRArmPos - 10f);  // visually up
            } else {
                // Normal swing animation
                armR.getSprite().setCenterY(originalRArmPos + (8 * sineA) - (8 * sinceG));
            }
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
            wGlow.setCurrAngle(pauldronR.getCurrAngle());

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

        // Reset for next swing cycle
        if (chargeLevel < 1f) {
            hasSwung = false;
        }
    }
    private float clampAngle(float angle, float min, float max) {
        angle = normalizeAngle(angle);
        min = normalizeAngle(min);
        max = normalizeAngle(max);

        // If min <= max, clamp straightforwardly
        if (min <= max) {
            if (angle < min) return min;
            if (angle > max) return max;
            return angle;
        } else {
            // If min > max, it means the range crosses 0° (e.g., min=350, max=10)
            if (angle > max && angle < min) {
                // Clamp to whichever is closer
                float distToMin = shortestRotationDistance(angle, min);
                float distToMax = shortestRotationDistance(angle, max);
                return (distToMin < distToMax) ? min : max;
            }
            return angle;
        }
    }

    private float normalizeAngle(float angle) {
        angle = angle % 360f;
        if (angle < 0) angle += 360f;
        return angle;
    }

    private float shortestRotationDistance(float from, float to) {
        float diff = to - from;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        return Math.abs(diff);
    }




     //save for now - This looks a spear Changed the name from renderHammer to keep around
        public static void renderHammer(CombatEngineAPI engine, WeaponAPI weapon, float swingProgress) {
            Vector2f fireOffset = weapon.getFirePoint(0);
            if (fireOffset == null) return;

            Vector2f worldOffset = weapon.getLocation();
            worldOffset = fireOffset != null ? fireOffset : worldOffset;

            float angle = weapon.getCurrAngle();
            float swingDistance = swingProgress * HAMMER_HEIGHT;

            // Calculate hammerhead position and size based on swing
            Vector2f hammerPosition = MathUtils.getPointOnCircumference(worldOffset, swingDistance, angle);
            float hammerHeight = HAMMER_HEIGHT * Math.max(0.5f, 1f - swingProgress);
            float hammerWidth = HAMMER_WIDTH; // stays constant

            // Render nebula particles inside the hammerhead
            renderNebula(engine, hammerPosition, angle, hammerWidth, hammerHeight);

            // Render glow orbs inside the hammerhead
            renderGlowOrbs(engine, hammerPosition, angle, hammerWidth, hammerHeight);

            // Render the glowing rectangular hammerhead
            renderHammerhead(engine, hammerPosition, angle, hammerWidth, hammerHeight);
        }

        private static void renderNebula(CombatEngineAPI engine, Vector2f position, float angle, float width, float height) {
            for (int i = 0; i < NEBULA_PARTICLES; i++) {
                float xOffset = (float) Math.random() * width - width / 2;
                float yOffset = (float) Math.random() * height - height / 2;
                Vector2f offset = new Vector2f();
                Vector2f point = MathUtils.getPointOnCircumference(offset, yOffset, angle);
                point = MathUtils.getPointOnCircumference(point, xOffset, angle + 90f);
                Vector2f particlePosition = Vector2f.add(position, point, null);
                engine.addSwirlyNebulaParticle(particlePosition, new Vector2f(), 20f, 1.5f, 0.5f, 0.8f, 1.0f, NEBULA_COLOR, false);
            }
        }

        private static void renderGlowOrbs(CombatEngineAPI engine, Vector2f position, float angle, float width, float height) {
            for (int i = 0; i < GLOW_ORBS; i++) {
                float xOffset = (float) Math.random() * width - width / 2;
                float yOffset = (float) Math.random() * height - height / 2;
                Vector2f offset = new Vector2f();
                Vector2f point = MathUtils.getPointOnCircumference(offset, yOffset, angle);
                point = MathUtils.getPointOnCircumference(point, xOffset, angle + 90f);
                Vector2f orbPosition = Vector2f.add(position, point, null);
                engine.addSmoothParticle(orbPosition, new Vector2f(), 10f, 1.0f, 0.5f, GLOW_COLOR);
            }
        }
        private static void renderHammerhead(CombatEngineAPI engine, Vector2f position, float angle, float width, float height) {
            for (float i = -0.5f; i <= 0.5f; i += 0.25f) {
                Vector2f verticalOffset = MathUtils.getPointOnCircumference(new Vector2f(), i * height, angle);

                Vector2f centerLine = Vector2f.add(position, verticalOffset, null);

                Vector2f edgeL = MathUtils.getPointOnCircumference(centerLine, -width / 2, angle + 90f);
                Vector2f edgeR = MathUtils.getPointOnCircumference(centerLine, width / 2, angle + 90f);

                engine.addSmoothParticle(edgeL, new Vector2f(), 10f, 1.0f, 0.5f, GLOW_COLOR);
                engine.addSmoothParticle(edgeR, new Vector2f(), 10f, 1.0f, 0.5f, GLOW_COLOR);
            }
        }
    }

