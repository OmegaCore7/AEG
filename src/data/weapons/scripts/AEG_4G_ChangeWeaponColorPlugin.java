package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import java.awt.Color;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class AEG_4G_ChangeWeaponColorPlugin implements EveryFrameWeaponEffectPlugin {

    private static final Color ACTIVE_COLOR = new Color(0, 255, 100, 255); // Green color for active
    private static final Color NEBULA_COLOR = new Color(0, 255, 100, 255); // Green color for particles
    private boolean systemActive = false;
    private float timeElapsed = 0f; // Track elapsed time for system activation
    private IntervalUtil lightningInterval = new IntervalUtil(0.5f, 0.5f); // Interval for lightning effects

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip() == null) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        ShipSystemAPI system = ship.getSystem();

        if (system == null || (!system.isChargeup() && !system.isActive())) {
            systemActive = false; // Reset system active flag
            timeElapsed = 0f; // Reset elapsed time
            weapon.getSprite().setColor(Color.WHITE); // Default white when system is off
            return; // Return if the system is not activated
        }

        timeElapsed += amount;
        lightningInterval.advance(amount);

        // Apply effects during charge-up phase
        if (timeElapsed > 0f && timeElapsed < 4f) {
            // Dynamic particle effect during charge-up
            float ballSize = 2f + (20f * (timeElapsed / 4f)); // Ball grows over time, max size 22f
            Vector2f ballLocation = transformRelativeToShip(ship, new Vector2f(70, 0));

            // Create swirling particles to form the ball
            for (int i = 0; i < 5; i++) {
                float angle = (float) (Math.random() * 2 * Math.PI);
                float distance = (float) (Math.random() * ballSize);
                Vector2f particlePoint = transformRelativeToShip(ship, new Vector2f(
                        70 + distance * (float) Math.cos(angle),
                        distance * (float) Math.sin(angle)
                ));
                float particleSize = 2f + (float)(Math.random() * 5f);
                engine.addHitParticle(particlePoint, new Vector2f(), particleSize, 1, 0.5f, NEBULA_COLOR);
            }

            // Absorbing particles moving towards the center point (70, 0)
            for (int i = 0; i < 4; i++) {
                float angle = (float) (Math.random() * 2 * Math.PI);
                float distance = 23f;
                Vector2f particlePoint = transformRelativeToShip(ship, new Vector2f(
                        70 + distance * (float) Math.cos(angle),
                        distance * (float) Math.sin(angle)
                ));
                Vector2f velocity = new Vector2f(
                        (ballLocation.x - particlePoint.x) / 4f,
                        (ballLocation.y - particlePoint.y) / 4f
                ); // Particles move towards the ball
                float particleSize = 2f + (float)(Math.random() * 5f);
                float transparency = 0.1f + (0.9f * (1 - timeElapsed / 4f)); // More transparent further away
                Color particleColor = new Color(
                        (int)(NEBULA_COLOR.getRed() * transparency + NEBULA_COLOR.getRed() * (1 - transparency)),
                        (int)(NEBULA_COLOR.getGreen() * transparency + NEBULA_COLOR.getGreen() * (1 - transparency)),
                        (int)(NEBULA_COLOR.getBlue() * transparency + NEBULA_COLOR.getBlue() * (1 - transparency)),
                        255
                );
                engine.addNebulaParticle(particlePoint, velocity, particleSize, 1, 0.5f, 0.5f, 1f, particleColor);
            }

            // Lightning strikes to the front of the ship, gradually moving towards (70, 0)
            if (lightningInterval.intervalElapsed()) {
                float progress = timeElapsed / 4f;
                Vector2f startPoint = transformRelativeToShip(ship, new Vector2f(70, -65 * (1 - progress)));
                Vector2f endPoint = transformRelativeToShip(ship, new Vector2f(70, 65 * (1 - progress)));
                Vector2f centerPoint = transformRelativeToShip(ship, new Vector2f(70, 0));
                float lightningLength = 70f - (65f * progress); // Shortens over time
                engine.spawnEmpArc(ship, startPoint, ship, ship, DamageType.ENERGY, 0, 0, 100f, null, 10f, new Color(255,0,0), new Color(255,200,200,255));
                engine.spawnEmpArc(ship, endPoint, ship, ship, DamageType.ENERGY, 0, 0, 100f, null, 10f, new Color(255,255,0), new Color(255,255,200,255));
                engine.spawnEmpArc(ship, centerPoint, ship, ship, DamageType.ENERGY, 0, 0, 100f, null, 10f, new Color(0,255,100), new Color(200,255,200,255));
            }
        }

        // Apply color change effects
        if (system.isActive()) {
            if (!systemActive) {
                systemActive = true;
                timeElapsed = 0f; // Reset the timer when the system becomes active
            }
            if (timeElapsed >= 4f) {
                weapon.getSprite().setColor(ACTIVE_COLOR); // Green when active
            }
        } else {
            weapon.getSprite().setColor(Color.WHITE); // Default white when system is off
            systemActive = false; // Reset the state when the system is off
            timeElapsed = 0f; // Reset the timer when the system is off
        }
    }

    private Vector2f transformRelativeToShip(ShipAPI ship, Vector2f relative) {
        float facing = ship.getFacing() * (float) Math.PI / 180f;
        float cos = (float) Math.cos(facing);
        float sin = (float) Math.sin(facing);
        return new Vector2f(
                ship.getLocation().x + relative.x * cos - relative.y * sin,
                ship.getLocation().y + relative.x * sin + relative.y * cos
        );
    }
}