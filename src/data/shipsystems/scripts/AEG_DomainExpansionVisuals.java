package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

public class AEG_DomainExpansionVisuals {

    private static final float DOMAIN_RADIUS = 2000f;
    private static final int PARTICLE_COUNT = 300;
    private static final float PARTICLE_SIZE = 10f;
    private static final float PARTICLE_LIFETIME = 2.0f;
    private static final Color[] PARTICLE_COLORS = {
            new Color(255, 255, 0, 255), // Bright yellow
            new Color(255, 165, 0, 255), // Orange
            new Color(255, 69, 0, 255)   // Red
    };

    private final Random random = new Random();

    public void createDomainVisuals(CombatEngineAPI engine, ShipAPI ship, boolean isActive) {
        Vector2f shipLocation = ship.getLocation();

        // Create particles for the ring
        for (int i = 0; i < PARTICLE_COUNT / 2; i++) {
            float angle = random.nextFloat() * 360f;
            float x = (float) Math.cos(Math.toRadians(angle)) * DOMAIN_RADIUS;
            float y = (float) Math.sin(Math.toRadians(angle)) * DOMAIN_RADIUS;
            Vector2f particleLocation = new Vector2f(shipLocation.x + x, shipLocation.y + y);

            Color particleColor = PARTICLE_COLORS[random.nextInt(PARTICLE_COLORS.length)];

            engine.addHitParticle(
                    particleLocation,
                    new Vector2f(0, 0), // No velocity
                    PARTICLE_SIZE,
                    1.0f, // Brightness
                    PARTICLE_LIFETIME,
                    particleColor
            );
        }

        // Create particles being absorbed into the ring
        for (int i = 0; i < PARTICLE_COUNT / 2; i++) {
            float angle = random.nextFloat() * 360f;
            float distance = random.nextFloat() * DOMAIN_RADIUS;
            float x = (float) Math.cos(Math.toRadians(angle)) * distance;
            float y = (float) Math.sin(Math.toRadians(angle)) * distance;
            Vector2f particleLocation = new Vector2f(shipLocation.x + x, shipLocation.y + y);

            Color particleColor = PARTICLE_COLORS[random.nextInt(PARTICLE_COLORS.length)];
            float transparency = 1.0f - (distance / DOMAIN_RADIUS);

            engine.addHitParticle(
                    particleLocation,
                    new Vector2f(0, 0), // No velocity
                    PARTICLE_SIZE,
                    transparency, // Brightness
                    PARTICLE_LIFETIME,
                    particleColor
            );
        }
    }
}
