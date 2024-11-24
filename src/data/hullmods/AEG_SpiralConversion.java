package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AEG_SpiralConversion extends BaseHullMod {
    private static final float ABSORB_RADIUS = 1000f;
    private static final int MAX_CHARGES = 5;
    private static final float RECHARGE_TIME = 10f;
    private static final Color FLASH_COLOR = new Color(105, 255, 105, 255); // Green flash color
    private static final float THUNDERBOLT_DELAY = 1f; // Delay before thunderbolt strikes

    private final Map<ShipAPI, Integer> charges = new HashMap<>();
    private final Map<ShipAPI, IntervalUtil> rechargeTimers = new HashMap<>();
    private final IntervalUtil inputCheckInterval = new IntervalUtil(0.1f, 0.1f); // Check input every 0.1 seconds
    private final Map<ShipAPI, Float> thunderboltTimers = new HashMap<>();
    private final Map<ShipAPI, Float> absorbedDamage = new HashMap<>();
    private final Map<ShipAPI, Vector2f> flashLocations = new HashMap<>();
    private final Map<ShipAPI, Integer> absorbedMissileCount = new HashMap<>(); // Store the number of absorbed missiles

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCombatEngine().isPaused()) return;

        if (!charges.containsKey(ship)) {
            charges.put(ship, MAX_CHARGES);
            rechargeTimers.put(ship, new IntervalUtil(RECHARGE_TIME, RECHARGE_TIME));
        }

        IntervalUtil rechargeTimer = rechargeTimers.get(ship);
        rechargeTimer.advance(amount);

        if (rechargeTimer.intervalElapsed() && charges.get(ship) < MAX_CHARGES) {
            charges.put(ship, charges.get(ship) + 1);
        }

        inputCheckInterval.advance(amount);
        if (inputCheckInterval.intervalElapsed() && charges.get(ship) > 0 && !ship.isPhased() && isActivationKeyPressed()) {
            List<DamagingProjectileAPI> projectilesInRange = CombatUtils.getProjectilesWithinRange(ship.getLocation(), ABSORB_RADIUS);
            if (!projectilesInRange.isEmpty()) {
                absorbProjectiles(ship, projectilesInRange);
                charges.put(ship, charges.get(ship) - 1);
            }
        }

        // Handle thunderbolt strike after delay
        if (thunderboltTimers.containsKey(ship)) {
            float thunderboltTimer = thunderboltTimers.get(ship);
            thunderboltTimer -= amount;
            if (thunderboltTimer <= 0) {
                strikeThunderbolt(ship);
                thunderboltTimers.remove(ship);
                flashLocations.remove(ship);
                absorbedDamage.remove(ship);
                absorbedMissileCount.remove(ship);
            } else {
                thunderboltTimers.put(ship, thunderboltTimer);
            }
        }
    }

    private boolean isActivationKeyPressed() {
        return Keyboard.isKeyDown(Keyboard.KEY_W) && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
    }

    private void absorbProjectiles(ShipAPI ship, List<DamagingProjectileAPI> projectiles) {
        float totalDamage = 0f;
        int projectileCount = projectiles.size(); // Count the number of projectiles absorbed
        for (DamagingProjectileAPI projectile : projectiles) {
            totalDamage += projectile.getDamageAmount();
            Global.getCombatEngine().removeEntity(projectile);

            // Create visual effects for projectile absorption
            Vector2f projectileLocation = projectile.getLocation();
            Global.getCombatEngine().addHitParticle(projectileLocation, new Vector2f(), 15f, 1f, 0.5f, FLASH_COLOR);
            for (int i = 0; i < 10; i++) {
                Vector2f particleVel = MathUtils.getRandomPointInCircle(new Vector2f(), 50f);
                Global.getCombatEngine().addSmoothParticle(projectileLocation, particleVel, 5f, 1f, 0.5f, FLASH_COLOR);
            }
        }

        healShip(ship, totalDamage);

        Vector2f flashLocation = ship.getLocation();
        flashLocations.put(ship, flashLocation);
        thunderboltTimers.put(ship, THUNDERBOLT_DELAY);
        absorbedDamage.put(ship, totalDamage);
        absorbedMissileCount.put(ship, projectileCount); // Store the projectile count

        Global.getCombatEngine().addHitParticle(flashLocation, new Vector2f(), 100f, 1f, 0.1f, FLASH_COLOR);
        Global.getSoundPlayer().playSound("hit_shield_solid_gun", 1f, 1f, flashLocation, ship.getVelocity());
        Global.getSoundPlayer().playSound("shield_burnout", 1f, 1f, flashLocation, ship.getVelocity()); // Replace with actual absorb sound
    }

    private void healShip(ShipAPI ship, float damage) {
        float maxArmor = ship.getArmorGrid().getMaxArmorInCell();
        float armorToHeal = Math.min(damage, maxArmor);
        adjustArmor(ship, armorToHeal);

        float remainingDamage = damage - armorToHeal;
        if (remainingDamage > 0) {
            ship.setHitpoints(ship.getHitpoints() + remainingDamage);
        }
    }

    private void adjustArmor(ShipAPI ship, float armorToHeal) {
        ArmorGridAPI armorGrid = ship.getArmorGrid();
        int gridWidth = armorGrid.getGrid().length;
        int gridHeight = armorGrid.getGrid()[0].length;

        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                float currentArmor = armorGrid.getArmorValue(x, y);
                float maxArmor = armorGrid.getMaxArmorInCell();
                float newArmor = Math.min(currentArmor + armorToHeal, maxArmor);
                armorGrid.setArmorValue(x, y, newArmor);
                armorToHeal -= (newArmor - currentArmor);
                if (armorToHeal <= 0) {
                    return;
                }
            }
        }
    }

    private void strikeThunderbolt(ShipAPI ship) {
        Vector2f flashLocation = flashLocations.get(ship);
        int projectileCount = absorbedMissileCount.get(ship); // Get the number of absorbed projectiles

        for (int i = 0; i < projectileCount; i++) {
            ShipAPI target = findNearestEnemy(ship, flashLocation);

            if (target != null) {
                float damage = absorbedDamage.get(ship);
                Vector2f targetLocation = target.getLocation();
                Global.getCombatEngine().spawnEmpArc(ship, flashLocation, null, target, DamageType.ENERGY, damage, damage, 10000f, null, 100f, new Color(105, 255, 105, 200), new Color(100, 255, 200, 255));
                Global.getSoundPlayer().playSound("system_entropy", 1f, 1f, targetLocation, target.getVelocity());
                Global.getSoundPlayer().playSound("shield_burnout", 1f, 1f, targetLocation, target.getVelocity()); // Replace with actual lightning sound
            }
        }
    }

    private ShipAPI findNearestEnemy(ShipAPI ship, Vector2f location) {
        ShipAPI closestEnemy = null;
        float closestDistance = Float.MAX_VALUE;

        for (ShipAPI enemy : CombatUtils.getShipsWithinRange(location, 1000f)) {
            if (enemy.getOwner() != ship.getOwner() && enemy.isAlive()) {
                float distance = MathUtils.getDistance(location, enemy.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEnemy = enemy;
                }
            }
        }

        return closestEnemy;
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + ABSORB_RADIUS;
        if (index == 1) return "" + MAX_CHARGES;
        if (index == 2) return "" + RECHARGE_TIME;
        return null;
    }
}