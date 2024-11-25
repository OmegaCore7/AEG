package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Random;

public class AEG_BlasterShell extends BaseHullMod {
    private static final float DAMAGE_MULT = 1.2f;
    private static final Color SHIELD_COLOR = new Color(105, 255, 105, 255); // Default shield color set to green
    private static final Color[] PARTICLE_COLORS = {
            new Color(0, 255, 0, 225), // Green
            new Color(144, 238, 144, 225), // Light Green
            new Color(173, 216, 230, 225) // Light Teal
    };
    private boolean isActive = false;
    private float timeSinceLastFiring = 0f;
    private static final float DEACTIVATION_DELAY = 2f;
    private final Random random = new Random();

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getShieldUnfoldRateMult().modifyMult(id, 10f); // Fast unfold rate
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) return;

        // Ensure shield is always green and has a fast unfold rate
        if (ship.getShield() != null) {
            ship.getShield().setRingColor(SHIELD_COLOR);
            ship.getShield().setInnerColor(SHIELD_COLOR);
            ship.getMutableStats().getShieldUnfoldRateMult().modifyMult("AEG_BlasterShell", 2f);
        }

        boolean anyWeaponFiring = false;
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (isSpecificWeaponSlot(weapon) && weapon.isFiring()) {
                anyWeaponFiring = true;
                if (!isActive) {
                    activateHullMod(ship);
                    isActive = true;
                }
                break; // No need to check further if one weapon is already firing
            }
        }

        if (anyWeaponFiring) {
            timeSinceLastFiring = 0f;
            createShieldParticles(ship);
        } else {
            timeSinceLastFiring += amount;
            if (timeSinceLastFiring >= DEACTIVATION_DELAY && isActive) {
                deactivateHullMod(ship);
                isActive = false;
            }
        }
    }

    private boolean isSpecificWeaponSlot(WeaponAPI weapon) {
        // Check if the weapon ID matches the specified weapon slots
        return weapon.getSlot().getId().equals("WS0005") || weapon.getSlot().getId().equals("WS0006");
    }

    private void activateHullMod(final ShipAPI ship) {
        ship.getMutableStats().getShieldDamageTakenMult().modifyMult("AEG_BlasterShell_shield", 0.1f);
        ship.getMutableStats().getShieldUpkeepMult().modifyMult("AEG_BlasterShell_shield", 0f);
        ship.setJitterUnder(this, SHIELD_COLOR, 1f, 10, 5f, 10f);

        ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult("AEG_BlasterShell_damage", DAMAGE_MULT);
        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("AEG_BlasterShell_damage", DAMAGE_MULT);
        ship.getMutableStats().getMissileWeaponDamageMult().modifyMult("AEG_BlasterShell_damage", DAMAGE_MULT);
    }

    private void deactivateHullMod(final ShipAPI ship) {
        ship.getMutableStats().getShieldDamageTakenMult().unmodify("AEG_BlasterShell_shield");
        ship.getMutableStats().getShieldUpkeepMult().unmodify("AEG_BlasterShell_shield");
        ship.getMutableStats().getBallisticWeaponDamageMult().unmodify("AEG_BlasterShell_damage");
        ship.getMutableStats().getEnergyWeaponDamageMult().unmodify("AEG_BlasterShell_damage");
        ship.getMutableStats().getMissileWeaponDamageMult().unmodify("AEG_BlasterShell_damage");
    }

    private void createShieldParticles(ShipAPI ship) {
        if (ship.getShield() == null) return;

        Vector2f shieldCenter = ship.getShield().getLocation();
        float shieldRadius = ship.getShield().getRadius();

        int particleCount = random.nextInt(9) + 2; // 2 to 10 particles per second
        for (int i = 0; i < particleCount; i++) {
            float angle = random.nextFloat() * 360f;
            float distance = random.nextFloat() * shieldRadius;
            float size = random.nextFloat() * 5f + 2f; // Particle size between 2 and 7
            float duration = random.nextFloat() + 0.5f; // Duration between 0.5 and 1.5 seconds
            float speed = distance / duration;

            Vector2f velocity = new Vector2f((float) Math.cos(Math.toRadians(angle)) * speed, (float) Math.sin(Math.toRadians(angle)) * speed);
            Color particleColor = PARTICLE_COLORS[random.nextInt(PARTICLE_COLORS.length)];
            Global.getCombatEngine().addHitParticle(shieldCenter, velocity, size, 1f, duration, particleColor);
        }
    }
}