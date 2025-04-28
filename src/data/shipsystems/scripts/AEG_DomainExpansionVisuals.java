package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.Random;

public class AEG_DomainExpansionVisuals {

    private static final String DISTORTION_SPRITE = "graphics/fx/wormhole_ring_bright2.png";
    private static final Random random = new Random();

    public void renderDomain(CombatEngineAPI engine, ShipAPI ship, final float currentRadius, boolean isActive) {
        if (!isActive) {
            return;
        }

        final Vector2f shipLocation = ship.getLocation();

        // === 1. Giant shimmer dome using a transparent sprite ===
        MagicRender.battlespace(
                Global.getSettings().getSprite(DISTORTION_SPRITE),
                shipLocation,
                new Vector2f(),
                new Vector2f(currentRadius * 2f, currentRadius * 2f),
                new Vector2f(),
                0f, 0f,
                new Color(
                        255,                     // red
                        140 + random.nextInt(110), // yellow/orange
                        20 + random.nextInt(50),  // just a hint of red/orange
                        70 + random.nextInt(185)   // alpha
                ),
                true,
                0f,
                0.2f,
                0.1f
        );

        // === 2. Subtle ambient fog particles inside the dome ===
        if (random.nextFloat() < 0.3f) { // control density
            Vector2f offset = MathUtils.getRandomPointInCircle(shipLocation, currentRadius);
            engine.addNebulaParticle(
                    offset,
                    new Vector2f(), // no velocity
                    150f + random.nextFloat() * 150f,
                    1.8f + random.nextInt(5),
                    0.3f, 0.6f,
                    2f + random.nextFloat() * 2f,
                    new Color(
                            255,
                            160 + random.nextInt(70), // Warm golden
                            40 + random.nextInt(40),  // Burnt orange
                            100 + random.nextInt(155) // Visible but not overpowering
                    )
            );
        }

        // === 3. Ripple distortion shader around the domain ===
        if (Global.getSettings().getModManager().isModEnabled("shaderLib")) {
            RippleDistortion ripple = new RippleDistortion(
                    shipLocation,         // Position
                    new Vector2f(0f, 0f)  // Velocity (stationary ripple)
            );
            ripple.setSize(currentRadius);
            ripple.setIntensity(15f - random.nextInt(7)); // adjust to taste
            ripple.setFrameRate(60f);
            ripple.setLifetime(0.5f);
            ripple.fadeInIntensity(0.3f);
            ripple.fadeOutIntensity(0.2f);
            ripple.setLocation(shipLocation); // Ensure it's centered properly

            DistortionShader.addDistortion(ripple);
        }
    }
    public static void spawnEdgeLightning(CombatEngineAPI engine, ShipAPI origin, float radius) {
        Vector2f center = origin.getLocation();
        for (int i = 0; i < 6; i++) {
            float angle = (float) Math.random() * 360f;
            Vector2f from = MathUtils.getPointOnCircumference(center, radius, angle);
            float offsetAngle = angle + 10f + (float) Math.random() * 20f;
            Vector2f to = MathUtils.getPointOnCircumference(center, radius, offsetAngle);

            engine.spawnEmpArcVisual(
                    from, null,
                    to, null,
                    10f,
                    new Color(255, 180, 60, 255),
                    new Color(255, 100, 20, 150)
            );
        }
    }
    public static void spawnEnemyGroundEffect(CombatEngineAPI engine, ShipAPI origin, float radius) {
        Vector2f center = origin.getLocation();
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() == origin.getOwner()) continue;
            if (!ship.isAlive()) continue;
            if (MathUtils.getDistance(center, ship.getLocation()) > radius) continue;

            engine.addNebulaParticle(
                    ship.getLocation(),
                    new Vector2f(),
                    120f + (float) Math.random() * 80f,
                    1.5f,
                    0.4f, 0.8f,
                    2f,
                    new Color(255, 140 + (int)(Math.random() * 60), 40 + (int)(Math.random() * 40), 80 + (int)(Math.random() * 60))
            );
        }
    }


}
