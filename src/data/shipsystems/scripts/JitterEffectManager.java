package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class JitterEffectManager {

    private static final Map<Long, JitterCopy> jitterCopies = new HashMap<>();
    private static final Random random = new Random();

    public static void addJitterCopy(ShipAPI ship, Color jitterColor, float duration, float jitterRange) {
        Vector2f location = new Vector2f(ship.getLocation());
        SpriteAPI sprite = Global.getSettings().getSprite(ship.getHullSpec().getSpriteName());
        JitterCopy jitterCopy = new JitterCopy(
                Misc.random.nextLong(),
                sprite,
                location,
                ship.getFacing(),
                jitterColor,
                duration,
                jitterRange
        );

        jitterCopies.put(jitterCopy.id, jitterCopy);
        System.out.println("Added jitter copy: " + jitterCopy.id);
    }

    public static void advance(float amount) {
        List<Long> jitterCopiesToRemove = new ArrayList<>();
        for (Map.Entry<Long, JitterCopy> entry : jitterCopies.entrySet()) {
            JitterCopy jitterCopy = entry.getValue();
            jitterCopy.lifetime += amount;
            if (jitterCopy.lifetime > jitterCopy.duration) {
                jitterCopiesToRemove.add(jitterCopy.id);
            }
        }
        for (Long id : jitterCopiesToRemove) {
            jitterCopies.remove(id);
        }
    }

    public static void render(CombatEngineLayers layer, ViewportAPI view) {
        if (layer == CombatEngineLayers.BELOW_SHIPS_LAYER) {
            for (JitterCopy jitterCopy : jitterCopies.values()) {
                renderJitterCopy(jitterCopy, view);
            }
        }
    }

    private static void renderJitterCopy(JitterCopy jitterCopy, ViewportAPI view) {
        if (!view.isNearViewport(jitterCopy.location, view.getVisibleWidth())) return;

        SpriteAPI sprite = jitterCopy.sprite;
        sprite.setAngle(jitterCopy.facing - 90f);
        sprite.setColor(jitterCopy.color);
        sprite.setAlphaMult(1 - jitterCopy.lifetime / jitterCopy.duration);
        sprite.setAdditiveBlend();

        Vector2f jitteredLocation = getRandomPointInCircle(jitterCopy.location, jitterCopy.jitterRange);
        sprite.renderAtCenter(jitteredLocation.x, jitteredLocation.y);
        System.out.println("Rendered jitter copy: " + jitterCopy.id);
    }

    private static Vector2f getRandomPointInCircle(Vector2f center, float radius) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = random.nextDouble() * radius;
        float x = (float) (center.x + Math.cos(angle) * distance);
        float y = (float) (center.y + Math.sin(angle) * distance);
        return new Vector2f(x, y);
    }

    private static class JitterCopy {
        long id;
        SpriteAPI sprite;
        Vector2f location;
        float facing;
        Color color;
        float duration;
        float jitterRange;
        float lifetime = 0f;

        JitterCopy(long id, SpriteAPI sprite, Vector2f location, float facing, Color color, float duration, float jitterRange) {
            this.id = id;
            this.sprite = sprite;
            this.location = location;
            this.facing = facing;
            this.color = color;
            this.duration = duration;
            this.jitterRange = jitterRange;
        }
    }
}
