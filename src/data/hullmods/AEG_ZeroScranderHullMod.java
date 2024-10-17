package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class AEG_ZeroScranderHullMod extends BaseHullMod {

    private static final float SWAY_SPEED_MULTIPLIER = 0.9f;
    private static final float HOVER_AMPLITUDE = 5f;
    private static final float HOVER_FREQUENCY = 1f;
    private static final int TRAIL_COUNT = 5;
    private static final float TRAIL_SPACING = 10f;
    private static final float TRAIL_FADE_TIME = 0.5f;
    private static final String TRAIL_IMAGE_PATH = "graphics/ships/zero/zero_scrander.png";

    private IntervalUtil hoverInterval = new IntervalUtil(0.1f, 0.1f);
    private List<Vector2f> trailPositions = new ArrayList<>();
    private List<Float> trailAlphas = new ArrayList<>();

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) return;

        CombatEngineAPI engine = Global.getCombatEngine();
        WeaponAPI scrander = findWeaponBySlot(ship, "WS0001");

        if (scrander == null) return;

        hoverInterval.advance(amount);

        if (hoverInterval.intervalElapsed()) {
            updateHover(scrander, amount);
            updateTrail(engine, scrander);
        }

        updateSway(scrander, ship, amount);
        addGlowEffect(engine, scrander);
    }

    private WeaponAPI findWeaponBySlot(ShipAPI ship, String slotId) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSlot().getId().equals(slotId)) {
                return weapon;
            }
        }
        return null;
    }

    private void updateHover(WeaponAPI scrander, float amount) {
        float hoverOffset = (float) Math.sin(Global.getCombatEngine().getTotalElapsedTime(false) * HOVER_FREQUENCY) * HOVER_AMPLITUDE;
        scrander.getLocation().y += hoverOffset * amount;
    }

    private void updateSway(WeaponAPI scrander, ShipAPI ship, float amount) {
        Vector2f shipVelocity = ship.getVelocity();
        Vector2f swayVelocity = new Vector2f(shipVelocity.x * SWAY_SPEED_MULTIPLIER, shipVelocity.y * SWAY_SPEED_MULTIPLIER);
        Vector2f.add(scrander.getLocation(), swayVelocity, scrander.getLocation());
    }

    private void updateTrail(CombatEngineAPI engine, WeaponAPI scrander) {
        Vector2f currentPos = new Vector2f(scrander.getLocation());
        trailPositions.add(0, currentPos);
        trailAlphas.add(0, 1f);

        if (trailPositions.size() > TRAIL_COUNT) {
            trailPositions.remove(trailPositions.size() - 1);
            trailAlphas.remove(trailAlphas.size() - 1);
        }

        for (int i = 0; i < trailPositions.size(); i++) {
            Vector2f pos = trailPositions.get(i);
            float alpha = trailAlphas.get(i);
            Color color;
            if (i % 3 == 0) {
                color = new Color(255, 0, 0, (int) (alpha * 255)); // Red
            } else if (i % 3 == 1) {
                color = new Color(255, 255, 0, (int) (alpha * 255)); // Yellow
            } else {
                color = new Color(255, 140, 0, (int) (alpha * 255)); // Sunset (Orange)
            }
            engine.addHitParticle(pos, new Vector2f(), TRAIL_SPACING, alpha, TRAIL_FADE_TIME, color);
            trailAlphas.set(i, alpha - (1f / TRAIL_COUNT));
        }
    }

    private void addGlowEffect(CombatEngineAPI engine, WeaponAPI scrander) {
        Vector2f location = scrander.getLocation();
        engine.addHitParticle(location, new Vector2f(), 20f, 1f, 0.5f, Color.CYAN);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // Apply any initial effects here
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "hover and follow the main ship's movement";
        return null;
    }
}
