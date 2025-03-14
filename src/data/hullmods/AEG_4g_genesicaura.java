package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.Random;

public class AEG_4g_genesicaura extends BaseHullMod implements DamageListener {
    private static final float EMP_RESISTANCE = 0.99f;
    private static final float DAMAGE_REDUCTION_NEAR_ENEMY = 0.1f;
    private static final float BASE_REGEN_RATE = 1f; // Base regeneration rate
    private static final float RING_MIN_SIZE = 5f;
    private static final float RING_MAX_SIZE = 300f;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship.getFluxTracker().isOverloadedOrVenting()) return;

        // Hull regeneration
        if (ship.getHitpoints() < ship.getMaxHitpoints()) {
            float hullRegenRate = BASE_REGEN_RATE * (1 - ship.getHitpoints() / ship.getMaxHitpoints());
            float hullRegen = ship.getMaxHitpoints() * hullRegenRate * amount;
            ship.setHitpoints(ship.getHitpoints() + hullRegen);
        } else {
            // Armor regeneration
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

        // EMP resistance
        ship.getMutableStats().getEmpDamageTakenMult().modifyMult("AEG_4g_genesicaura", EMP_RESISTANCE);

        // Damage reduction near enemy ships
        boolean enemyNearby = false;
        for (ShipAPI enemy : CombatUtils.getShipsWithinRange(ship.getLocation(), 200f)) {
            if (enemy.getOwner() != ship.getOwner()) {
                enemyNearby = true;
                ship.getMutableStats().getHullDamageTakenMult().modifyMult("AEG_4g_genesicaura_near_enemy", DAMAGE_REDUCTION_NEAR_ENEMY);
                ship.getMutableStats().getArmorDamageTakenMult().modifyMult("AEG_4g_genesicaura_near_enemy", DAMAGE_REDUCTION_NEAR_ENEMY);
                break;
            }
        }
        if (!enemyNearby) {
            ship.getMutableStats().getHullDamageTakenMult().unmodify("AEG_4g_genesicaura_near_enemy");
            ship.getMutableStats().getArmorDamageTakenMult().unmodify("AEG_4g_genesicaura_near_enemy");
        }

        // Absorb projectiles and missiles
        for (DamagingProjectileAPI projectile : CombatUtils.getProjectilesWithinRange(ship.getLocation(), 600f)) {
            if (shouldAbsorbProjectile(ship, projectile)) {
                absorbProjectile(ship, projectile);
            }
        }
    }

    private boolean shouldAbsorbProjectile(ShipAPI ship, DamagingProjectileAPI projectile) {
        float distance = Vector2f.sub(projectile.getLocation(), ship.getLocation(), null).length();
        float chance = getAbsorbChance(distance);
        return new Random().nextFloat() < chance;
    }

    private void absorbProjectile(ShipAPI ship, DamagingProjectileAPI projectile) {
        Vector2f point = projectile.getLocation();
        projectile.setDamageAmount(0);  // Set damage to 0 to effectively absorb it
        projectile.setHitpoints(0);  // Destroy the projectile

        // Create electrical hit spark effect
        MagicRender.singleframe(
                Global.getSettings().getSprite("graphics/fx/electric_spark.png"),
                point,
                new Vector2f(20f, 20f),  // Adjust size as needed
                0,
                new Color(0, 255, 255, 255),
                true
        );
    }

    private float getAbsorbChance(float distance) {
        if (distance <= 200f) {
            return 0.75f;  // 75% chance within 200 units
        } else if (distance <= 400f) {
            return 0.5f;  // 50% chance within 400 units
        } else if (distance <= 600f) {
            return 0.25f;  // 25% chance within 600 units
        } else {
            return 0f;  // No chance beyond 600 units
        }
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