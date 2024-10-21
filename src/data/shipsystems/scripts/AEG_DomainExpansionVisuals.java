package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

public class AEG_DomainExpansionVisuals {

    private static final float DOMAIN_RADIUS = 2000f;
    private static final int PARTICLE_COUNT = 300;
    private static final float PARTICLE_SIZE = 10f;
    private static final float PARTICLE_LIFETIME = 2.0f;
    private static final Color[] PARTICLE_COLORS = {
            new Color(255, 255, 192, 255), // Bright yellow
            new Color(255, 199, 91, 255), // Orange
            new Color(194, 130, 0, 255)   // Red
    };

    private final Random random = new Random();

    public void createDomainVisuals(CombatEngineAPI engine, ShipAPI ship, boolean isActive, float currentRadius) {
        Vector2f shipLocation = ship.getLocation();

        // Create particles for the outer ring
        for (int i = 0; i < PARTICLE_COUNT / 2; i++) {
            float angle = random.nextFloat() * 360f;
            float x = (float) Math.cos(Math.toRadians(angle)) * DOMAIN_RADIUS;
            float y = (float) Math.sin(Math.toRadians(angle)) * DOMAIN_RADIUS;
            Vector2f particleLocation = new Vector2f(shipLocation.x + x, shipLocation.y + y);

            if (MathUtils.getDistance(shipLocation, particleLocation) <= currentRadius) {
                Color particleColor = PARTICLE_COLORS[random.nextInt(PARTICLE_COLORS.length)];

                engine.addHitParticle(
                        particleLocation,
                        new Vector2f(0, 0), // No velocity
                        PARTICLE_SIZE * 2, // Larger size for outer ring
                        1.0f, // Brightness
                        PARTICLE_LIFETIME,
                        particleColor
                );
            }
        }

        // Create particles moving outward from the ship
        for (int i = 0; i < PARTICLE_COUNT / 2; i++) {
            float angle = random.nextFloat() * 360f;
            float distance = random.nextFloat() * DOMAIN_RADIUS;
            float x = (float) Math.cos(Math.toRadians(angle)) * distance;
            float y = (float) Math.sin(Math.toRadians(angle)) * distance;
            Vector2f particleLocation = new Vector2f(shipLocation.x + x, shipLocation.y + y);

            if (MathUtils.getDistance(shipLocation, particleLocation) <= currentRadius) {
                Color particleColor = PARTICLE_COLORS[random.nextInt(PARTICLE_COLORS.length)];
                float transparency = 1.0f - (distance / DOMAIN_RADIUS);

                Vector2f velocity = new Vector2f(x / PARTICLE_LIFETIME, y / PARTICLE_LIFETIME);

                engine.addHitParticle(
                        particleLocation,
                        velocity, // Velocity outward
                        PARTICLE_SIZE * (1.0f + (distance / DOMAIN_RADIUS)), // Size increases with distance
                        transparency, // Brightness
                        PARTICLE_LIFETIME,
                        particleColor
                );
            }
        }

        // Create radiant pulsar light effect
        createPulsarLightEffect(engine, ship, currentRadius);
    }

    private void createPulsarLightEffect(CombatEngineAPI engine, ShipAPI ship, float currentRadius) {
        Vector2f shipLocation = ship.getLocation();
        float angle = random.nextFloat() * 360f;
        float distance = random.nextFloat() * DOMAIN_RADIUS;
        float x = (float) Math.cos(Math.toRadians(angle)) * distance;
        float y = (float) Math.sin(Math.toRadians(angle)) * distance;
        Vector2f targetLocation = new Vector2f(shipLocation.x + x, shipLocation.y + y);

        if (MathUtils.getDistance(shipLocation, targetLocation) <= currentRadius) {
            engine.addHitParticle(
                    targetLocation,
                    new Vector2f(0, 0), // No velocity
                    PARTICLE_SIZE * 2, // Larger size for pulsar effect
                    1.0f, // Brightness
                    PARTICLE_LIFETIME,
                    new Color(255, 255, 255, 255) // White color for pulsar light
            );
        }
    }
}
