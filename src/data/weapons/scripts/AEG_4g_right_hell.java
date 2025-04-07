package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import org.magiclib.util.MagicRender;

public class AEG_4g_right_hell implements EveryFrameWeaponEffectPlugin {

    private static final Color CHARGEUP_COLOR = new Color(255, 0, 0, 255); // Red color for charge-up
    private static final Color ACTIVE_COLOR = new Color(0, 255, 100, 255); // Green color for active
    private static final Color PARTICLE_COLOR = new Color(255, 50, 50, 255); // Ripple effect color
    private boolean runOnce = false;
    private boolean rippleEffectTriggered = false;  // Flag to ensure ripple effect is triggered only once

    // Updated firing offset coordinates (relative to the weapon)
    private static final Vector2f FIRING_OFFSET = new Vector2f(60, -45);

    private float chargeupElapsed = 0f; // Track elapsed charge-up time

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip() == null) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        if (!runOnce) {
            init(weapon);
            runOnce = true;
        }

        ShipSystemAPI system = ship.getSystem();

        // Reset ripple effect flag when system is off
        if (system == null || (!system.isChargeup() && !system.isActive())) {
            rippleEffectTriggered = false;  // Reset after charge-up/active state
            return; // Return if the system is not activated
        }

        if (system.isChargeup()) {
            chargeupElapsed += amount;
            handleChargeupMovements(weapon, ship, chargeupElapsed);
        } else if (system.isActive()) {
            lockWeaponAngles(weapon, ship);
            chargeupElapsed = 0f; // Reset charge-up elapsed time
        } else {
            resetWeaponState(weapon);
            chargeupElapsed = 0f; // Reset charge-up elapsed time
        }

        // Handle weapon frame and color based on system state
        if (system.isChargeup()) {
            weapon.getSprite().setColor(CHARGEUP_COLOR); // Red during charge-up
            weapon.getAnimation().setFrame(1); // Charge-up frame
        } else if (system.isActive()) {
            weapon.getSprite().setColor(ACTIVE_COLOR); // Green when active
            weapon.getAnimation().setFrame(1); // Active frame (same as charge-up)
        } else {
            weapon.getSprite().setColor(Color.WHITE); // Default white when system is off
            weapon.getAnimation().setFrame(0); // Idle frame
        }

        // If system is in charge-up and ripple hasn't been triggered, do so
        if (system.isChargeup() && !rippleEffectTriggered) {
            triggerRippleEffect(weapon); // Use the fixed firing point relative to the weapon and ship facing
            rippleEffectTriggered = true;  // Set flag to prevent re-triggering
        }
    }

    private void handleChargeupMovements(WeaponAPI weapon, ShipAPI ship, float elapsed) {
        // Logic to move the arm and shoulder during charge-up
        float shipFacing = ship.getFacing();
        float chargeupDuration = 4.0f; // Total charge-up duration

        // Calculate the progress of the charge-up phase
        float progress = (elapsed - 0.5f) / (chargeupDuration - 0.5f); // Adjust for 0.5-second pause

        if (progress < 0) {
            progress = 0; // Ensure progress doesn't go negative
        }

        // Interpolate angles based on progress
        float weaponAngle = interpolateAngle(progress, shipFacing + 20, shipFacing + 40, shipFacing + 61);
        float shoulderAngle = interpolateAngle(progress, shipFacing + 5, shipFacing + 10, shipFacing + 16);

        // Hold at angle 0 for the first 0.5 seconds
        if (elapsed < 0.5f) {
            weaponAngle = shipFacing;
            shoulderAngle = shipFacing;
        }

        setWeaponAngle(weapon, weaponAngle);
        setShoulderAngle(ship, shoulderAngle);
    }

    private float interpolateAngle(float progress, float startAngle, float midAngle, float endAngle) {
        // Interpolate angles with pitstops and back-and-forth motion
        if (progress < 0.25f) {
            return lerp(startAngle, midAngle, progress / 0.25f);
        } else if (progress < 0.5f) {
            return lerp(midAngle, startAngle, (progress - 0.25f) / 0.25f);
        } else if (progress < 0.75f) {
            return lerp(startAngle, endAngle, (progress - 0.5f) / 0.25f);
        } else {
            return lerp(endAngle, startAngle, (progress - 0.75f) / 0.25f);
        }
    }

    private float lerp(float start, float end, float t) {
        return start + t * (end - start);
    }

    private void lockWeaponAngles(WeaponAPI weapon, ShipAPI ship) {
        // Lock angles when the system is active
        setWeaponAngle(weapon, ship.getFacing() + 61);
        setShoulderAngle(ship, ship.getFacing() + 16);
    }

    private void resetWeaponState(WeaponAPI weapon) {
        // Reset weapon state when the system is off
        weapon.getSprite().setColor(Color.WHITE);
        weapon.getAnimation().setFrame(0);
    }

    private void setWeaponAngle(WeaponAPI weapon, float angle) {
        // Logic to set the weapon angle
        weapon.setCurrAngle(angle);
    }

    private void setShoulderAngle(ShipAPI ship, float angle) {
        // Logic to set the shoulder angle (assuming shoulder is a weapon slot)
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().getId().equals("WS0004")) {
                w.setCurrAngle(angle);
                break;
            }
        }
    }

    private void triggerRippleEffect(WeaponAPI weapon) {
        // Get the weapon's location and ship's facing
        Vector2f weaponLocation = weapon.getLocation();
        ShipAPI ship = weapon.getShip();
        float shipFacing = ship.getFacing();

        // Calculate the firing point using the weapon's firing offset and ship's facing
        Vector2f firingPoint = new Vector2f(
                weaponLocation.x + FIRING_OFFSET.x * (float) Math.cos(Math.toRadians(shipFacing)) - FIRING_OFFSET.y * (float) Math.sin(Math.toRadians(shipFacing)),
                weaponLocation.y + FIRING_OFFSET.x * (float) Math.sin(Math.toRadians(shipFacing)) + FIRING_OFFSET.y * (float) Math.cos(Math.toRadians(shipFacing))
        );

        // Create the ripple effect at the calculated firing point
        float shieldRadius = ship.getShieldRadiusEvenIfNoShield();

        // First ripple effect (near the weapon's location)
        MagicRender.battlespace(
                Global.getSettings().getSprite("graphics/fx/wormhole_ring_bright3.png"),
                firingPoint,
                new Vector2f(0f, 0f),
                new Vector2f(shieldRadius, shieldRadius),
                new Vector2f(shieldRadius + 200f, shieldRadius + 200f),
                360f,
                0f,
                new Color(255, 0, 0, 255),
                true,
                0.2f, // Brightness duration
                0f,
                0.5f // Transparency
        );

        // Second ripple effect (further out from the weapon)
        MagicRender.battlespace(
                Global.getSettings().getSprite("graphics/fx/explosion_ring0.png"),
                firingPoint,
                new Vector2f(0f, 0f),
                new Vector2f(shieldRadius * 0.8f, shieldRadius * 0.8f),
                new Vector2f(shieldRadius + 80f, shieldRadius + 80f),
                360f,
                0f,
                new Color(255, 100, 100, 255),
                true,
                0.3f, // Ripple effect duration
                0f,
                0.5f // Alpha value
        );

        // Third ripple effect (even further out from the weapon)
        MagicRender.battlespace(
                Global.getSettings().getSprite("graphics/fx/starburst_glow1.png"),
                firingPoint,
                new Vector2f(0f, 0f),
                new Vector2f(shieldRadius * 0.6f, shieldRadius * 0.6f),
                new Vector2f(shieldRadius + 60f, shieldRadius + 60f),
                360f,
                0f,
                new Color(255, 100, 100, 255),
                true,
                0.4f, // Ripple effect duration
                0f,
                0.6f // Alpha value
        );

        // Final ripple effect (largest)
        MagicRender.battlespace(
                Global.getSettings().getSprite("graphics/fx/star_halo.png"),
                firingPoint,
                new Vector2f(0f, 0f),
                new Vector2f(50f, 50f),
                new Vector2f(200f, 200f),
                360f,
                0f,
                new Color(255, 50, 50, 255),
                true,
                0.5f, // Ripple effect duration
                0f,
                0.5f // Alpha value
        );
    }

    private void init(WeaponAPI weapon) {
        // Initialization logic if needed
    }
}