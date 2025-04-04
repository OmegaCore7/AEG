package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import java.awt.Color;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class AEG_4G_ChangeWeaponColorPlugin implements EveryFrameWeaponEffectPlugin {

    private static final Color ACTIVE_COLOR = new Color(0, 255, 0, 255); // Green color for active
    private static final Color NEBULA_COLOR = new Color(0, 255, 0, 255); // Green color for particles
    private static final Color TEAL_COLOR = new Color(0, 255, 255, 255); // Teal color for particles
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
            float ballSize = 10f + (40f * (timeElapsed / 4f)); // Ball grows over time, max size 50f
            Vector2f ballLocation = new Vector2f(ship.getLocation().x, ship.getLocation().y + 70);

            // Create swirling particles to form the ball
            for (int i = 0; i < 50; i++) {
                float angle = (float) (Math.random() * 2 * Math.PI);
                float distance = (float) (Math.random() * ballSize);
                Vector2f particlePoint = new Vector2f(
                        ballLocation.x + distance * (float) Math.cos(angle),
                        ballLocation.y + distance * (float) Math.sin(angle)
                );
                float particleSize = 5f + (float)(Math.random() * 10f);
                engine.addHitParticle(particlePoint, new Vector2f(), particleSize, 1, 0.5f, NEBULA_COLOR);
            }

            // Particles flowing into the ball
            for (int i = 0; i < 10; i++) {
                float angle = (float) (Math.random() * 2 * Math.PI);
                Vector2f particlePoint = new Vector2f(
                        ballLocation.x + 75f * (float) Math.cos(angle),
                        ballLocation.y + 75f * (float) Math.sin(angle)
                );
                Vector2f velocity = new Vector2f(
                        (ballLocation.x - particlePoint.x) / 4f,
                        (ballLocation.y - particlePoint.y) / 4f
                ); // Particles move towards the ball
                float particleSize = 5f + (float)(Math.random() * 10f);
                float transparency = 0.1f + (0.9f * (1 - timeElapsed / 4f)); // More transparent further away
                Color particleColor = new Color(
                        (int)(NEBULA_COLOR.getRed() * transparency + TEAL_COLOR.getRed() * (1 - transparency)),
                        (int)(NEBULA_COLOR.getGreen() * transparency + TEAL_COLOR.getGreen() * (1 - transparency)),
                        (int)(NEBULA_COLOR.getBlue() * transparency + TEAL_COLOR.getBlue() * (1 - transparency)),
                        255
                );
                engine.addNebulaParticle(particlePoint, velocity, particleSize, 1, 0.5f, 0.5f, 1f, particleColor);
            }

            // Lightning strikes between (70, 70) and (-70, 70)
            if (lightningInterval.intervalElapsed()) {
                Vector2f startPoint = new Vector2f(ship.getLocation().x + 60, ship.getLocation().y + 70);
                Vector2f endPoint = new Vector2f(ship.getLocation().x - 60, ship.getLocation().y + 70);
                float lightningLength = 70f - (65f * (timeElapsed / 4f)); // Shortens over time
                engine.spawnEmpArc(ship, startPoint, ship, ship, DamageType.ENERGY, 0, 0, 1000f, null, 10f, new Color(255,0,0), new Color(255,200,200,255));
                engine.spawnEmpArc(ship, endPoint, ship, ship, DamageType.ENERGY, 0, 0, 1000f, null, 10f, new Color(255,255,0), new Color(255,255,200,255));
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
}