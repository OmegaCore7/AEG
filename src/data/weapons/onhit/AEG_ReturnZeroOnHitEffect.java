package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AEG_ReturnZeroOnHitEffect implements OnHitEffectPlugin {

    private static final float BLACKHOLE_DURATION = 3.0f;
    private static final float PULL_RADIUS = 600f;
    private static final float PULL_STRENGTH = 300f;
    private static final Color RING_COLOR = new Color(50, 50, 255, 80);

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        engine.addPlugin(new BlackHoleEffectPlugin(point));
    }

    public static class BlackHoleEffectPlugin implements EveryFrameCombatPlugin {

        private final Vector2f location;
        private float elapsed = 0f;
        private final List<Ring> rings = new ArrayList<>();

        public BlackHoleEffectPlugin(Vector2f loc) {
            this.location = new Vector2f(loc);
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (Global.getCombatEngine().isPaused()) return;

            elapsed += amount;
            if (elapsed > BLACKHOLE_DURATION) {
                Global.getCombatEngine().removePlugin(this);
                return;
            }

            // Apply pull to ships
            for (ShipAPI ship : Global.getCombatEngine().getShips()) {
                float dist = Misc.getDistance(location, ship.getLocation());
                if (dist < PULL_RADIUS) {
                    Vector2f pull = Vector2f.sub(location, ship.getLocation(), null);
                    pull.normalise();
                    float strength = (1f - dist / PULL_RADIUS) * PULL_STRENGTH;
                    Vector2f.add(ship.getVelocity(), (Vector2f) pull.scale(strength * amount), ship.getVelocity());
                }
            }

            // Create expanding rings
            rings.add(new Ring(elapsed, location));
            renderRings();
        }

        private void renderRings() {
            Iterator<Ring> iter = rings.iterator();
            while (iter.hasNext()) {
                Ring ring = iter.next();
                float progress = (elapsed - ring.creationTime) / BLACKHOLE_DURATION;
                if (progress > 1f) {
                    iter.remove();
                    continue;
                }

                float size = 100f + progress * 400f;
                float alpha = 1f - progress;
                Color color = new Color(RING_COLOR.getRed(), RING_COLOR.getGreen(), RING_COLOR.getBlue(), (int)(alpha * RING_COLOR.getAlpha()));
                Global.getCombatEngine().addSmoothParticle(location, new Vector2f(), size, 1f, 0.05f, color);
            }
        }

        @Override public void init(CombatEngineAPI engine) {}
        @Override public void renderInWorldCoords(ViewportAPI viewport) {}
        @Override public void renderInUICoords(ViewportAPI viewport) {}

        private static class Ring {
            final float creationTime;
            final Vector2f location;

            Ring(float time, Vector2f loc) {
                creationTime = time;
                location = new Vector2f(loc);
            }
        }
        @Override
        public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
            // No input processing needed for this effect
        }
    }
}
