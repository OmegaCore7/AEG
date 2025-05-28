package data.hullmods;

import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

public class AEG_4G_GoldionModePlugin extends BaseEveryFrameCombatPlugin {

    // === Energy Gauge Settings ===
    private float goldionEnergy = 0f;              // 0.0 to 1.0
    private final float goldionMax = 1.0f;
    private boolean goldionReady = false;

    // === Armor State ===
    private boolean goldionArmorActive = false;
    private float armorDuration = 10f;             // in seconds
    private float armorTimer = 0f;

    // === Input Tracking ===
    private boolean shiftHeld = false;
    private boolean forwardHeld = false;

    // === Reference to player/mech ship ===
    private ShipAPI mech;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) return;

        // === Init reference to mech ===
        if (mech == null) {
            ShipAPI player = engine.getPlayerShip();
            if (player != null && player.getHullSpec().getHullId().equals("YOUR_MECH_HULL_ID")) {
                mech = player;
            } else return;
        }

        // === 1. Update Goldion Energy Gauge ===
        updateGoldionGauge(engine, amount);

        // === 2. Handle Auto Armor Activation ===
        checkGoldionArmorTrigger(engine, amount);

        // === 3. Check for Player Input ===
        checkInput(events);

        // === 4. Handle Goldion Finger / Crusher Activation ===
        if (goldionArmorActive) {
            handleGoldionActions(engine, amount);
        }

        // === 5. Optional: Render Gauge (stub) ===
        renderGauge(engine);
    }

    private void updateGoldionGauge(CombatEngineAPI engine, float amount) {
        if (goldionReady || goldionArmorActive) return;

        // === Example logic: Gain energy over time or from damage dealt ===
        goldionEnergy += amount * 0.02f; // TODO: customize logic
        if (goldionEnergy >= goldionMax) {
            goldionEnergy = goldionMax;
            goldionReady = true;
            // Optional: Play sound or effect
        }
    }

    private void checkGoldionArmorTrigger(CombatEngineAPI engine, float amount) {
        if (!goldionReady || goldionArmorActive || mech == null) return;

        float hpRatio = mech.getHitpoints() / mech.getMaxHitpoints();
        if (hpRatio <= 0.2f) {
            // === Activate Goldion Armor ===
            goldionArmorActive = true;
            goldionReady = false;
            armorTimer = armorDuration;
            goldionEnergy = 0f;

            // TODO: Add visuals/sounds for Goldion Armor activation
            engine.addFloatingText(mech.getLocation(), "GOLDION ARMOR ENGAGED!", 30f, Color.YELLOW, mech, 0.5f, 1f);
        }

        // Armor countdown
        if (goldionArmorActive) {
            armorTimer -= amount;
            if (armorTimer <= 0f) {
                goldionArmorActive = false;
                // TODO: Remove visuals/effects
            }
        }
    }

    private void checkInput(List<InputEventAPI> events) {
        shiftHeld = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        forwardHeld = Keyboard.isKeyDown(Keyboard.KEY_W);
    }

    private void handleGoldionActions(CombatEngineAPI engine, float amount) {
        if (!shiftHeld || !forwardHeld || mech == null) return;

        CombatEntityAPI target = engine.getPlayerShip().getShipTarget();

        if (target != null) {
            triggerGoldionFinger(engine, target);
        } else {
            triggerGoldionCrusher(engine);
        }

        // Only allow one trigger per activation
        goldionArmorActive = false;
        armorTimer = 0f;
    }

    private void triggerGoldionFinger(CombatEngineAPI engine, CombatEntityAPI target) {
        // === TODO: Beam effect, heavy damage to target ===
        engine.addFloatingText(target.getLocation(), "GOLDION FINGER!", 40f, Color.ORANGE, target, 0.5f, 1f);
        engine.applyDamage(target, target.getLocation(), 1500f, DamageType.ENERGY, 500f, false, false, mech);

        // TODO: Add beam visual, particle effects, sfx
    }

    private void triggerGoldionCrusher(CombatEngineAPI engine) {
        // === TODO: Large AoE effect around mech ===
        engine.addFloatingText(mech.getLocation(), "GOLDION CRUSHER!", 40f, Color.YELLOW, mech, 0.5f, 1f);

        float radius = 400f;
        Iterator<Object> iter = engine.getAllObjectGrid().getCheckIterator(mech.getLocation(), radius, radius);
        while (iter.hasNext()) {
            Object obj = iter.next();
            if (!(obj instanceof CombatEntityAPI)) continue;

            CombatEntityAPI entity = (CombatEntityAPI) obj;

            if (entity == mech || entity instanceof AsteroidAPI) continue;

            float dist = Vector2f.sub(entity.getLocation(), mech.getLocation(), null).length();
            if (dist <= radius) {
                engine.applyDamage(entity, entity.getLocation(), 1000f, DamageType.FRAGMENTATION, 300f, false, false, mech);
            }
        }

        // TODO: Explosion VFX, shockwave, sfx
    }

    private void renderGauge(CombatEngineAPI engine) {
        // === Stub: Replace with real gauge drawing ===
        // You’ll eventually want to render a gradient gauge on the HUD
    }

    @Override
    public void init(CombatEngineAPI engine) {
        super.init(engine);
    }
}
