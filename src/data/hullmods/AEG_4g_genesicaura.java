package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

public class AEG_4g_genesicaura extends BaseHullMod implements DamageListener {
    private static final float EMP_RESISTANCE = 0.99f;
    private static final float DAMAGE_REDUCTION_NEAR_ENEMY = 0.1f;
    private static final float BASE_REGEN_RATE = 0.05f; // Base regeneration rate
    private static final float CHECK_INTERVAL = 10f; // Interval for switching between hull and armor regeneration
    private static final float ANIMATION_INTERVAL = 2f; // Interval for playing the animation
    private float timeSinceLastCheck = 0f;
    private float timeSinceLastAnimation = 0f;
    private boolean regeneratingArmor = false;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship.getFluxTracker().isOverloadedOrVenting()) return;

        timeSinceLastCheck += amount;
        timeSinceLastAnimation += amount;

        if (timeSinceLastCheck >= CHECK_INTERVAL) {
            timeSinceLastCheck = 0f;
            if (ship.getHitpoints() / ship.getMaxHitpoints() >= 0.9f) {
                regeneratingArmor = true;
            } else {
                regeneratingArmor = false;
            }
        }

        if (timeSinceLastAnimation >= ANIMATION_INTERVAL) {
            timeSinceLastAnimation = 0f;
            playLightningEffect(ship);
        }

        if (regeneratingArmor) {
            regenerateArmor(ship, amount);
        } else {
            regenerateHull(ship, amount);
        }

        // EMP resistance
        ship.getMutableStats().getEmpDamageTakenMult().modifyMult("AEG_4g_genesicaura", EMP_RESISTANCE);

        // Damage reduction near enemy ships
        boolean enemyNearby = false;
        for (ShipAPI enemy : CombatUtils.getShipsWithinRange(ship.getLocation(), 200f)) {
            if (enemy.getOwner() != ship.getOwner()) {
                enemyNearby = true;
                ship.getMutableStats().getHullDamageTakenMult().modifyMult("AEG_4g_genesicaura_near_enemy", DAMAGE_REDUCTION_NEAR_ENEMY);
                ship.getMutableStats().getArmorDamageTakenMult().modifyMult("AEG_4g_genesicaura_near_enemy", DAMAGE_REDUCTION_NEAR_ENEMY);
                ship.getMutableStats().getKineticDamageTakenMult().modifyMult("AEG_4g_genesicaura_near_enemy", DAMAGE_REDUCTION_NEAR_ENEMY);
                ship.getMutableStats().getHighExplosiveDamageTakenMult().modifyMult("AEG_4g_genesicaura_near_enemy", DAMAGE_REDUCTION_NEAR_ENEMY);
                ship.getMutableStats().getEnergyDamageTakenMult().modifyMult("AEG_4g_genesicaura_near_enemy", DAMAGE_REDUCTION_NEAR_ENEMY);
                break;
            }
        }
        if (!enemyNearby) {
            ship.getMutableStats().getHullDamageTakenMult().unmodify("AEG_4g_genesicaura_near_enemy");
            ship.getMutableStats().getArmorDamageTakenMult().unmodify("AEG_4g_genesicaura_near_enemy");
            ship.getMutableStats().getKineticDamageTakenMult().unmodify("AEG_4g_genesicaura_near_enemy");
            ship.getMutableStats().getHighExplosiveDamageTakenMult().unmodify("AEG_4g_genesicaura_near_enemy");
            ship.getMutableStats().getEnergyDamageTakenMult().unmodify("AEG_4g_genesicaura_near_enemy");
        }
    }

    private void regenerateHull(ShipAPI ship, float amount) {
        if (ship.getHitpoints() < ship.getMaxHitpoints()) {
            float hullRegenRate = BASE_REGEN_RATE * (1 - ship.getHitpoints() / ship.getMaxHitpoints());
            float hullRegen = ship.getMaxHitpoints() * hullRegenRate * amount;
            ship.setHitpoints(Math.min(ship.getHitpoints() + hullRegen, ship.getMaxHitpoints())); // Ensure healing to max hitpoints
        }
    }

    private void regenerateArmor(ShipAPI ship, float amount) {
        float[][] armorGrid = ship.getArmorGrid().getGrid();
        for (int x = 0; x < armorGrid.length; x++) {
            for (int y = 0; y < armorGrid[x].length; y++) {
                float currentArmor = armorGrid[x][y];
                float maxArmor = ship.getArmorGrid().getMaxArmorInCell();
                float armorRegenRate = BASE_REGEN_RATE * (1 - currentArmor / maxArmor);
                if (currentArmor < maxArmor) {
                    armorGrid[x][y] = Math.min(currentArmor + maxArmor * armorRegenRate * amount, maxArmor);
                }
            }
        }
    }

    private void playLightningEffect(ShipAPI ship) {
        Color color;
        if (ship.getSystem().isActive()) {
            color = new Color(0, 255, 0, 255); // Green when the ship system is active
        } else {
            color = regeneratingArmor ? new Color(255, 69, 0, 255) : new Color(255, 165, 0, 255); // Orangish Red for armor, Orangish Yellow for hull
        }

        for (int i = 0; i < 2; i++) {
            Vector2f startPoint = getRandomPointOnShip(ship);
            Vector2f endPoint = getRandomPointOnShip(ship);
            Global.getCombatEngine().spawnEmpArcVisual(
                    startPoint,
                    ship,
                    endPoint,
                    ship,
                    10f, // Thickness of the bolt
                    color,
                    color
            );
        }
    }

    private Vector2f getRandomPointOnShip(ShipAPI ship) {
        float x = ship.getLocation().x + (new Random().nextFloat() - 0.5f) * ship.getCollisionRadius();
        float y = ship.getLocation().y + (new Random().nextFloat() - 0.5f) * ship.getCollisionRadius();
        return new Vector2f(x, y);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // Apply initial effects here if needed
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(this);
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        // No changes needed here for projectile absorption
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        switch (index) {
            case 0: return "" + (int) (DAMAGE_REDUCTION_NEAR_ENEMY * 100) + "%";
            default: return null;
        }
    }
}