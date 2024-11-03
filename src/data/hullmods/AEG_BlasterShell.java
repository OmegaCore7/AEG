package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Random;

public class AEG_BlasterShell extends BaseHullMod {
    private static final float DAMAGE_MULT = 1.2f;
    private static final float RANGE_BONUS = 20f;
    private static final Color SHIELD_COLOR = new Color(105, 255, 105, 255);
    private static final Color DEFAULT_SHIELD_COLOR = Color.WHITE;
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
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) return;

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
        // Add your logic to check if the weapon is in a specific slot
        return true;
    }

    private void activateHullMod(final ShipAPI ship) {
        if (ship.getShield() == null || !ship.getShield().isOn()) {
            ship.getMutableStats().getShieldDamageTakenMult().modifyMult("AEG_BlasterShell_shield", 0.1f);
            ship.getMutableStats().getShieldUpkeepMult().modifyMult("AEG_BlasterShell_shield", 0f);
            ship.setJitterUnder(this, SHIELD_COLOR, 1f, 10, 5f, 10f);
        } else {
            ship.getShield().toggleOn();
            ship.getShield().setRingColor(SHIELD_COLOR);
            ship.getShield().setInnerColor(SHIELD_COLOR);
        }

        ship.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent("AEG_BlasterShell_range", RANGE_BONUS);
        ship.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent("AEG_BlasterShell_range", RANGE_BONUS);
        ship.getMutableStats().getMissileWeaponRangeBonus().modifyPercent("AEG_BlasterShell_range", RANGE_BONUS);
        ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult("AEG_BlasterShell_damage", DAMAGE_MULT);
        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("AEG_BlasterShell_damage", DAMAGE_MULT);
        ship.getMutableStats().getMissileWeaponDamageMult().modifyMult("AEG_BlasterShell_damage", DAMAGE_MULT);
    }

    private void deactivateHullMod(final ShipAPI ship) {
        ship.getMutableStats().getShieldDamageTakenMult().unmodify("AEG_BlasterShell_shield");
        ship.getMutableStats().getShieldUpkeepMult().unmodify("AEG_BlasterShell_shield");
        ship.getMutableStats().getBallisticWeaponRangeBonus().unmodify("AEG_BlasterShell_range");
        ship.getMutableStats().getEnergyWeaponRangeBonus().unmodify("AEG_BlasterShell_range");
        ship.getMutableStats().getMissileWeaponRangeBonus().unmodify("AEG_BlasterShell_range");
        ship.getMutableStats().getBallisticWeaponDamageMult().unmodify("AEG_BlasterShell_damage");
        ship.getMutableStats().getEnergyWeaponDamageMult().unmodify("AEG_BlasterShell_damage");
        ship.getMutableStats().getMissileWeaponDamageMult().unmodify("AEG_BlasterShell_damage");

        if (ship.getShield() != null) {
            ship.getShield().setRingColor(DEFAULT_SHIELD_COLOR);
            ship.getShield().setInnerColor(DEFAULT_SHIELD_COLOR);
        }
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
