package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class AEG_4g_genesicaura extends BaseHullMod {
    private static final float MAX_DAMAGE_THRESHOLD = 4000f;
    private static final float MIN_DAMAGE_THRESHOLD = 100f;
    private static final float MAX_ABSORB_CHANCE = 100f;
    private static final float MIN_ABSORB_CHANCE = 25f;
    private static final float EMP_RESISTANCE = 0.99f;
    private static final float DAMAGE_REDUCTION_NEAR_ENEMY = 0.1f;
    private static final float CR_THRESHOLD = 25f;
    private static final float REGEN_RATE = 0.01f; // Example rate, adjust as needed
    private static final float PULSE_INTERVAL = 2f;
    private static final float RING_MIN_SIZE = 5f;
    private static final float RING_MAX_SIZE = 300f;

    private IntervalUtil pulseInterval = new IntervalUtil(PULSE_INTERVAL, PULSE_INTERVAL);

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship.getFluxTracker().isOverloadedOrVenting()) return;

        float cr = ship.getCurrentCR();
        if (cr > CR_THRESHOLD) {
            // Hull regeneration
            float hullRegen = ship.getMaxHitpoints() * REGEN_RATE * amount;
            ship.setHitpoints(ship.getHitpoints() + hullRegen);

            // Armor regeneration
            float[][] armorGrid = ship.getArmorGrid().getGrid();
            for (int x = 0; x < armorGrid.length; x++) {
                for (int y = 0; y < armorGrid[x].length; y++) {
                    float currentArmor = armorGrid[x][y];
                    float maxArmor = ship.getArmorGrid().getMaxArmorInCell();
                    if (currentArmor < maxArmor) {
                        armorGrid[x][y] = Math.min(currentArmor + maxArmor * REGEN_RATE * amount, maxArmor);
                    }
                }
            }

            // EMP resistance
            ship.getMutableStats().getEmpDamageTakenMult().modifyMult("AEG_4g_genesicaura", EMP_RESISTANCE);

            // Damage reduction near enemy ships
            for (ShipAPI enemy : CombatUtils.getShipsWithinRange(ship.getLocation(), 150f)) {
                if (enemy.getOwner() != ship.getOwner()) {
                    ship.getMutableStats().getHullDamageTakenMult().modifyMult("AEG_4g_genesicaura_near_enemy", DAMAGE_REDUCTION_NEAR_ENEMY);
                    ship.getMutableStats().getArmorDamageTakenMult().modifyMult("AEG_4g_genesicaura_near_enemy", DAMAGE_REDUCTION_NEAR_ENEMY);
                }
            }
        }

        // Pulse visual effect
        pulseInterval.advance(amount);
        if (pulseInterval.intervalElapsed()) {
            MagicRender.singleframe(
                    Global.getSettings().getSprite("fx", "wormhole_ring_bright"),
                    ship.getLocation(),
                    new Vector2f(RING_MAX_SIZE, RING_MAX_SIZE),
                    0,
                    new Color(255, 150, 0, 255),
                    true
            );
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // Apply initial effects here if needed
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        switch (index) {
            case 0: return "" + (int) CR_THRESHOLD + "%";
            case 1: return "" + (int) MAX_ABSORB_CHANCE + "%";
            case 2: return "" + (int) MIN_ABSORB_CHANCE + "%";
            case 3: return "" + (int) MIN_DAMAGE_THRESHOLD;
            case 4: return "" + (int) MAX_DAMAGE_THRESHOLD;
            case 5: return "" + (int) (DAMAGE_REDUCTION_NEAR_ENEMY * 100) + "%";
            default: return null;
        }
    }
}