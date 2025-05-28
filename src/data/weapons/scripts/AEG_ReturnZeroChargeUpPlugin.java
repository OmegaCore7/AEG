package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class AEG_ReturnZeroChargeUpPlugin implements EveryFrameCombatPlugin {
    private ShipAPI ship;
    private boolean wasVenting = false;
    private boolean wasOverloaded = false;
    private final WeaponAPI weapon;
    private float elapsed = 0f;
    private boolean[] stagesTriggered = new boolean[4];
    private final IntervalUtil arcInterval = new IntervalUtil(0.1f, 0.2f); // add this at the top
    public AEG_ReturnZeroChargeUpPlugin(WeaponAPI weapon) {
        this.weapon = weapon;
        this.ship = weapon.getShip();
    }

    @Override public void init(CombatEngineAPI engine) {}
    @Override public void renderInWorldCoords(ViewportAPI viewport) {}
    @Override public void renderInUICoords(ViewportAPI viewport) {}
    @Override public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {}

    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) return;

        if (weapon.getShip() == null) {
            engine.removePlugin(this);
            return;
        }
// Check for new vent/overload state
        boolean venting = ship.getFluxTracker().isVenting();
        boolean overloaded = ship.getFluxTracker().isOverloaded();
        boolean justStartedVenting = venting && !wasVenting;
        boolean justStartedOverloaded = overloaded && !wasOverloaded;

        wasVenting = venting;
        wasOverloaded = overloaded;

        if (justStartedVenting || justStartedOverloaded) {
            abortCharge(engine);
            return;
        }


        Vector2f center = weapon.getLocation(); // dynamically updated every frame
        ShipAPI owner = weapon.getShip();

        elapsed += amount;

        // PHASE 1 (0-5s): Subtle resonance & glow
        if (elapsed < 5f) {
            if (!stagesTriggered[0]) {
                engine.addFloatingText(center, "Initiating Photon Core Synchronization...", 24f, Color.YELLOW, owner, 0.5f, 1f);
                Global.getSoundPlayer().playUISound("ui_noise_static", 1f, 1f); // reactor hum proxy
                stagesTriggered[0] = true;
            }
            spawnRotatingNebula(engine, center, elapsed, 150f - MathUtils.getRandom().nextInt(50), 0.3f, Color.orange);
        }

        // PHASE 2 (5–10s): Lightning intensifies, new rings
        else if (elapsed < 10f) {
            if (!stagesTriggered[1]) {
                engine.addFloatingText(center, "Core Containment Field Engaged", 28f, Color.YELLOW, owner, 0.5f, 1f);
                Global.getSoundPlayer().playSound("system_high_energy_focus_activate", 1f, 1.2f, center, new Vector2f());
                stagesTriggered[1] = true;
            }

            spawnRotatingNebula(engine, center, elapsed, 200f - MathUtils.getRandom().nextInt(50), 0.5f, new Color(
                    255,
                    140 + MathUtils.getRandom().nextInt(60),  // mid-to-bright yellow-orange
                    30 + MathUtils.getRandom().nextInt(30),   // keeps it warm, not too red
                    160 + MathUtils.getRandom().nextInt(80)   // still glowing, but not fully opaque
            ));
            if (Math.random() < 0.2f) {
                spawnRandomArc(engine, center, 400f);
            }
        }

        // PHASE 3 (10–17s): Corona expands, visual crescendo
        else if (elapsed < 17f) {
            if (!stagesTriggered[2]) {
                engine.addFloatingText(center, "WARNING: Photon Pressure Unstable", 30f, Color.ORANGE, owner, 0.5f, 1f);
                Global.getSoundPlayer().playSound("explosion_flak", 0.7f, 0.8f, center, new Vector2f());
                stagesTriggered[2] = true;
            }

            float radius = 300f + 150f * (float) Math.sin(elapsed * 2f);
            engine.addHitParticle(center, new Vector2f(), radius, 1f, 0.1f, new Color(255, 150, 60, 255));
            if (Math.random() < 0.3f) {
                spawnRandomArc(engine, center, 500f - MathUtils.getRandom().nextInt(150));
            }
        }

        // PHASE 4 (17–20s): Collapse moment, singularity prep
        else if (elapsed < 20f) {
            if (!stagesTriggered[3]) {
                engine.addFloatingText(center, "Power Levels Exceeding Threshold...", 36f, new Color(255,75 + MathUtils.getRandom().nextInt(50),0), owner, 0.5f, 1f);
                Global.getSoundPlayer().playSound("system_temporalshell", 1.1f, 1.3f, center, new Vector2f());
                stagesTriggered[3] = true;
            }

            // Time distortion visual
            spawnRotatingNebula(engine, center, elapsed, 300f - MathUtils.getRandom().nextInt(150), 0.7f,new Color(
                            255,
                            180 + MathUtils.getRandom().nextInt(40),   // warm yellow-orange core
                            60 + MathUtils.getRandom().nextInt(40),    // subtle red base
                            100 + MathUtils.getRandom().nextInt(80)    // slightly more opaque
                    ));
            // Electrical Burst + Bloom
            float shockSize = 350f + MathUtils.getRandom().nextFloat() * 100f;
            spawnShockwavePulse(engine, center, shockSize);
            // Bloom Glows
            engine.addHitParticle(center, new Vector2f(), shockSize * 1.2f, 0.8f, 0.4f, new Color(255, 140,40, 120));


            if (elapsed >= 17f && elapsed < 20f && arcInterval.intervalElapsed()) {
                float angle = (float) (Math.random() * 360f);
                float radians = (float) Math.toRadians(angle);
                float distance = 400f + MathUtils.getRandom().nextFloat() * 200f;

                Vector2f target = new Vector2f(
                        center.x + (float)Math.cos(radians) * distance,
                        center.y + (float)Math.sin(radians) * distance
                );

                engine.spawnEmpArcVisual(
                        center,
                        null,
                        target,
                        null,
                        75f + MathUtils.getRandom().nextInt(75),
                        new Color(255, 100 + MathUtils.getRandom().nextInt(155), 0, 255 - MathUtils.getRandom().nextInt(100)),
                        new Color(255, 200 + MathUtils.getRandom().nextInt(55), 50 + MathUtils.getRandom().nextInt(55), 255 - MathUtils.getRandom().nextInt(55))
                );
            }
        }

        // After 20s: remove this plugin and allow weapon fire
        else {
            engine.addFloatingText(center, "RETURN TO ZERO!", 42f, new Color(255, MathUtils.getRandom().nextInt(50),0,255 ), owner, 0.5f, 1f);
            // Dimensional rupture or temporal tension build
            Global.getSoundPlayer().playSound("system_temporalshell", 1.2f, 1.4f, center, new Vector2f());
            // EMP-charged detonation
            Global.getSoundPlayer().playSound("system_orion_device_explosion", 1.25f, 1.5f, center, new Vector2f());
            // Core implosion burst
            Global.getSoundPlayer().playSound("riftbeam_rift", 1.1f, 1.6f, center, new Vector2f());
            engine.removePlugin(this);
        }
    }

    //Charge interrupted or aborted helper
    private void abortCharge(CombatEngineAPI engine) {
        Vector2f center = weapon.getLocation();
        Global.getSoundPlayer().playSound("system_burn_drive_deactivate", 1f, 1.2f, center, new Vector2f());
        engine.addFloatingText(center, "CHARGE-UP ABORTED!", 32f, Color.RED, weapon.getShip(), 0.5f, 1f);
        engine.addHitParticle(center, new Vector2f(), 100f, 1f, 0.75f, new Color(255, 50, 50, 200));

        for (int i = 0; i < 4; i++) {
            Vector2f target = MathUtils.getRandomPointInCircle(center, 200f);
            engine.spawnEmpArcVisual(center, null, target, null, 25f,
                    new Color(255, 100, 0, 200),
                    new Color(255, 180, 60, 180));
        }

        engine.removePlugin(this);
    }
    private void spawnRotatingNebula(CombatEngineAPI engine, Vector2f center, float time, float radius, float chance, Color baseColor) {
        if (Math.random() > chance) return;

        float angle = (float) (time * 2f + Math.random() * Math.PI * 2);
        float x = center.x + radius * (float) Math.cos(angle);
        float y = center.y + radius * (float) Math.sin(angle);
        Vector2f loc = new Vector2f(x, y);

        float intensity = 0.8f + MathUtils.getRandom().nextFloat() * 0.4f;
        float size = 30f + MathUtils.getRandom().nextFloat() * 40f;

        // Clamp each component to 255
        int r = Math.min(255, (int)(baseColor.getRed() * intensity));
        int g = Math.min(255, (int)(baseColor.getGreen() * intensity * 0.8f));
        int b = Math.min(255, (int)(baseColor.getBlue() * intensity * 0.6f));

        Color faded = new Color(r, g, b, 180);

        engine.addNebulaParticle(loc, new Vector2f(), size, 1.5f, 0.2f, 0.4f + MathUtils.getRandom().nextFloat() * 0.3f, 0.5f, faded);
    }

    private void spawnRandomArc(CombatEngineAPI engine, Vector2f center, float range) {
        Vector2f target = MathUtils.getRandomPointInCircle(center, range);
        engine.spawnEmpArcVisual(
                target, null,
                center, null,
                15f + MathUtils.getRandom().nextInt(70),
                new Color(255, 75 + MathUtils.getRandom().nextInt(75), 0, 255 - MathUtils.getRandom().nextInt(100)),
                new Color(255, 200 + MathUtils.getRandom().nextInt(50), 50, 255 - MathUtils.getRandom().nextInt(55))
        );
    }

    private void spawnShockwavePulse(CombatEngineAPI engine, Vector2f center, float size) {
        int layers = 5;
        for (int i = 0; i < layers; i++) {
            float layerSize = size * (0.5f + (i / (float) layers));
            float brightness = 1f - (i / (float) layers);
            Color layerColor = new Color(
                    255,
                    (int)(150 * brightness + 50),
                    (int)(30 * brightness),
                    (int)(180 * brightness)
            );
            engine.addNebulaParticle(
                    center,
                    new Vector2f(),
                    layerSize,
                    1.5f,
                    0.1f,
                    0.2f + MathUtils.getRandom().nextFloat() * 0.2f,
                    0.4f + MathUtils.getRandom().nextFloat() * 0.3f,
                    layerColor
            );
        }

        // Optional corona burst
        if (Math.random() < 0.7f) {
            for (int i = 0; i < 12; i++) {
                float angle = (float) (Math.random() * 360f);
                float radians = (float) Math.toRadians(angle);
                float distance = size * 0.75f + MathUtils.getRandom().nextFloat() * 100f;

                Vector2f point = new Vector2f(
                        center.x + (float) Math.cos(radians) * distance,
                        center.y + (float) Math.sin(radians) * distance
                );

                Color coronaColor = new Color(255, 200 + MathUtils.getRandom().nextInt(55), 50, 180);
                engine.addHitParticle(point, new Vector2f(), 40f + MathUtils.getRandom().nextFloat() * 20f, 1f, 0.6f, coronaColor);
            }
        }
    }
}
