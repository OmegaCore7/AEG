package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.Random;

public class AEG_DomainExpansionVisuals {

    private static final String DISTORTION_SPRITE = "graphics/fx/wormhole_ring_bright3.png";
    private static final Random random = new Random();
    private boolean ringRendered = false;

    public void renderDomain(CombatEngineAPI engine, ShipAPI ship, final float currentRadius, final float maxRadius, boolean isActive) {
        if (!isActive) {
            ringRendered = false;
            return;
        }

        final Vector2f shipLocation = ship.getLocation();
        // 1. Persistent background
        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "AEG_zero"),
                shipLocation,
                new Vector2f(),
                new Vector2f(1200, 1200),
                new Vector2f(0f, 0f),
                0f, 0f,
                new Color(255, 255, 255, 200 + random.nextInt(55)),
                false, // NOT additive
                0f,
                1f, // Re-render each frame to persist
                0f
        );

        // 2. Expanding ring visual (always redraws)
        if (currentRadius <= maxRadius) {
            MagicRender.battlespace(
                    Global.getSettings().getSprite(DISTORTION_SPRITE),
                    shipLocation,
                    new Vector2f(),
                    new Vector2f(currentRadius * 2f, currentRadius * 2f),
                    new Vector2f(),
                    0f, 0f,
                    new Color(255, 200 + random.nextInt(55), 50 - random.nextInt(50), 100 + random.nextInt(155)), // Yellow-orange ring, semi-transparent
                    true, // Additive for brightness
                    0f,
                    0.1f, // fade quickly
                    0.05f
            );
        }
    }
}
