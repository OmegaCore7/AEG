package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import org.magiclib.util.MagicRender;

public class AEG_4g_left_heaven implements EveryFrameWeaponEffectPlugin {

    private static final Color CHARGEUP_COLOR = new Color(255, 255, 0, 255); // Yellow color for charge-up
    private static final Color ACTIVE_COLOR = new Color(0, 255, 0, 255); // Green color for active
    private static final Color PARTICLE_COLOR = new Color(255, 255, 50, 255); // Ripple effect color
    private boolean runOnce = false;
    private boolean rippleEffectTriggered = false;  // Flag to ensure ripple effect is triggered only once

    // Updated firing offset coordinates (relative to the weapon)
    private static final Vector2f FIRING_OFFSET = new Vector2f(60, 45);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip() == null) {
            return;
        }

        if (!runOnce) {
            init(weapon);
            runOnce = true;
        }

        ShipAPI ship = weapon.getShip();

        if (ship.getSystem() != null) {
            ShipSystemAPI system = ship.getSystem();

            // Handle weapon frame and color based on system state
            if (system.isChargeup()) {
                weapon.getSprite().setColor(CHARGEUP_COLOR); // Yellow during charge-up
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

            // Reset ripple effect flag when system is off
            if (!system.isChargeup() && !system.isActive()) {
                rippleEffectTriggered = false;  // Reset after charge-up/active state
            }
        } else {
            weapon.getSprite().setColor(Color.WHITE); // Reset to white when system is off
            weapon.getAnimation().setFrame(0); // Idle frame
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
                Global.getSettings().getSprite("graphics/fx/shields64.png"),
                firingPoint,
                new Vector2f(0f, 0f),
                new Vector2f(shieldRadius, shieldRadius),
                new Vector2f(shieldRadius + 200f, shieldRadius + 200f),
                360f,
                0f,
                PARTICLE_COLOR,
                true,
                0.2f, // Brightness duration
                0f,
                0.5f // Transparency
        );

        // Second ripple effect (further out from the weapon)
        MagicRender.battlespace(
                Global.getSettings().getSprite("graphics/fx/starburst_glow1.png"),
                firingPoint,
                new Vector2f(0f, 0f),
                new Vector2f(shieldRadius * 0.8f, shieldRadius * 0.8f),
                new Vector2f(shieldRadius + 80f, shieldRadius + 80f),
                360f,
                0f,
                new Color(255, 200, 100, 255),
                true,
                0.3f, // Ripple effect duration
                0f,
                0.5f // Alpha value
        );

        // Third ripple effect (even further out from the weapon)
        MagicRender.battlespace(
                Global.getSettings().getSprite("graphics/fx/wormhole_ring.png"),
                firingPoint,
                new Vector2f(0f, 0f),
                new Vector2f(shieldRadius * 0.6f, shieldRadius * 0.6f),
                new Vector2f(shieldRadius + 60f, shieldRadius + 60f),
                360f,
                0f,
                new Color(255, 200, 100, 255),
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
                new Color(255, 255, 50, 255),
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