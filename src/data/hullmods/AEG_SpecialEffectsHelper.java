package data.hullmods;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class AEG_SpecialEffectsHelper {

    private static final float THRESHOLD_1 = 1.0f; // 100%
    private static final float THRESHOLD_2 = 1.5f; // 150%
    private static final float THRESHOLD_3 = 2.0f; // 200%

    private CombatEngineAPI engine;

    public AEG_SpecialEffectsHelper(CombatEngineAPI engine) {
        this.engine = engine;
    }

    public void applyEffects(ShipAPI ship, float powerGauge) {
        if (ship == null || !ship.isAlive()) return;

        WeaponAPI shoulderL = getWeaponBySlot(ship, "WS0001");
        WeaponAPI shoulderR = getWeaponBySlot(ship, "WS0002");

        if (powerGauge >= THRESHOLD_1) {
            playEffects(shoulderL);
            playEffects(shoulderR);
        }
        if (powerGauge >= THRESHOLD_2) {
            playEnhancedEffects(shoulderL);
            playEnhancedEffects(shoulderR);
        }
        if (powerGauge >= THRESHOLD_3) {
            playUltimateEffects(shoulderL);
            playUltimateEffects(shoulderR);
        }
    }

    private WeaponAPI getWeaponBySlot(ShipAPI ship, String slotId) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSlot().getId().equals(slotId)) {
                return weapon;
            }
        }
        return null;
    }

    private void playEffects(WeaponAPI weapon) {
        if (weapon == null) return;

        Vector2f location = weapon.getLocation();
        Vector2f velocity = weapon.getShip().getVelocity();

        // Particle effects
        for (int i = 0; i < 10; i++) {
            Vector2f particleLocation = MathUtils.getRandomPointInCircle(location, 20f);
            engine.addHitParticle(particleLocation, velocity, 10f, 1f, 1f, Color.ORANGE);
        }

        // Explosion effects
        engine.spawnExplosion(location, velocity, Color.ORANGE, 50f, 1f);
    }

    private void playEnhancedEffects(WeaponAPI weapon) {
        if (weapon == null) return;

        Vector2f location = weapon.getLocation();
        Vector2f velocity = weapon.getShip().getVelocity();

        // Nebula effects
        for (int i = 0; i < 5; i++) {
            Vector2f nebulaLocation = MathUtils.getRandomPointInCircle(location, 30f);
            engine.addNebulaParticle(nebulaLocation, velocity, 20f, 2f, 0.5f, 0.5f, 2f, Color.BLUE);
        }

        // Shockwave effects
        engine.spawnExplosion(location, velocity, Color.CYAN, 100f, 1.5f);
    }

    private void playUltimateEffects(WeaponAPI weapon) {
        if (weapon == null) return;

        Vector2f location = weapon.getLocation();
        Vector2f velocity = weapon.getShip().getVelocity();

        // Ultimate particle effects
        for (int i = 0; i < 20; i++) {
            Vector2f particleLocation = MathUtils.getRandomPointInCircle(location, 40f);
            engine.addHitParticle(particleLocation, velocity, 15f, 1f, 1.5f, Color.RED);
        }

        // Ultimate explosion effects
        engine.spawnExplosion(location, velocity, Color.RED, 150f, 2f);

        // Ultimate shockwave effects
        engine.spawnExplosion(location, velocity, Color.MAGENTA, 200f, 2.5f);
    }
}