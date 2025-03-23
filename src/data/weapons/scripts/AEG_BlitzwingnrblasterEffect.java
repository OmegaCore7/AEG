package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class AEG_BlitzwingnrblasterEffect implements EveryFrameWeaponEffectPlugin {
    private static final int MAX_FRAMES = 10;
    private static final float FRAME_DURATION = 0.1f; // Duration of each frame in seconds
    private static final float CHARGE_UP_DURATION = 1.0f; // Duration of charge-up before firing

    private int currentFrame = 0;
    private float frameTimer = 0f;
    private boolean playingForward = false;
    private boolean firing = false;
    private float burstTimer = 0f;
    private boolean burstActive = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        ShipSystemAPI system = ship.getSystem();
        MutableShipStatsAPI stats = ship.getMutableStats();

        if (system.getState() == ShipSystemAPI.SystemState.IN) {
            playingForward = true;
        } else if (system.getState() == ShipSystemAPI.SystemState.OUT) {
            playingForward = false;
        }

        if (weapon.isFiring()) {
            firing = true;
            playingForward = true;
        } else if (firing) {
            firing = false;
            playingForward = false;
        }

        frameTimer += amount;
        if (frameTimer >= FRAME_DURATION) {
            frameTimer -= FRAME_DURATION;
            if (playingForward) {
                if (currentFrame < MAX_FRAMES - 1) {
                    currentFrame++;
                }
            } else {
                if (currentFrame > 0) {
                    currentFrame--;
                }
            }
        }

        // Hold the last frame when the system is active
        if (system.getState() == ShipSystemAPI.SystemState.ACTIVE) {
            currentFrame = MAX_FRAMES - 1;
        }

        weapon.getAnimation().setFrame(currentFrame);

        // Lock the angle at 0 when the system is not active
        if (system.getState() == ShipSystemAPI.SystemState.IDLE || system.getState() == ShipSystemAPI.SystemState.COOLDOWN) {
            weapon.setCurrAngle(ship.getFacing());
        }

        // Handle burst effect during charge-up
        if (system.getState() == ShipSystemAPI.SystemState.IN) {
            burstTimer += amount;
            if (burstTimer >= CHARGE_UP_DURATION) {
                burstActive = true;
                burstTimer = 0f;
            }
        } else {
            burstActive = false;
            burstTimer = 0f;
        }

        if (burstActive) {
            burstTimer += amount;
            if (burstTimer >= FRAME_DURATION) {
                burstTimer -= FRAME_DURATION;
                createBurstEffect(weapon, ship);
            }
        }
    }

    private void createBurstEffect(WeaponAPI weapon, ShipAPI ship) {
        Vector2f weaponPos = weapon.getLocation();
        Vector2f firingOffset = new Vector2f(weaponPos.x + 50, weaponPos.y); // Adjust offset as needed

        // Calculate the direction based on the ship's facing and weapon's angle
        float angle = weapon.getCurrAngle();
        Vector2f direction = new Vector2f((float) Math.cos(Math.toRadians(angle)), (float) Math.sin(Math.toRadians(angle)));
        direction.scale(50); // Adjust scale as needed

        Vector2f endPos = Vector2f.add(weaponPos, direction, null);
    }
}