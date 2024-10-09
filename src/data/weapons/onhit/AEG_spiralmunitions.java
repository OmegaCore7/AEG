package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class AEG_spiralmunitions implements EveryFrameWeaponEffectPlugin, OnHitEffectPlugin {
    private IntervalUtil interval = new IntervalUtil(0.1f, 0.1f);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || engine.isPaused()) return;
        if (weapon == null) return;

        interval.advance(amount);
        if (interval.intervalElapsed()) {
            for (DamagingProjectileAPI proj : engine.getProjectiles()) {
                if (proj.getWeapon() == weapon) {
                    attractProjectiles(engine, proj);
                }
            }
            // Add charging effect at weapon's firing points
            for (int i = 0; i < weapon.getSpec().getHardpointAngleOffsets().size(); i++) {
                Vector2f firingPoint = weapon.getFirePoint(i);
                if (firingPoint != null) {
                    engine.addHitParticle(firingPoint, new Vector2f(0, 0), 10f, 1f, 0.1f, weapon.getSpec().getGlowColor());
                }
            }
        }
    }

    private void attractProjectiles(CombatEngineAPI engine, DamagingProjectileAPI proj) {
        if (engine == null || proj == null) return;

        for (DamagingProjectileAPI otherProj : engine.getProjectiles()) {
            if (otherProj != proj && Vector2f.sub(proj.getLocation(), otherProj.getLocation(), null).length() < 100f) {
                Vector2f attraction = Vector2f.sub(proj.getLocation(), otherProj.getLocation(), null);
                if (attraction != null) {
                    attraction.scale(0.1f); // Adjust the scale factor as needed
                    otherProj.getVelocity().translate(attraction.x, attraction.y);

                    // Check if the other projectile is close enough to be deleted
                    if (Vector2f.sub(proj.getLocation(), otherProj.getLocation(), null).length() < 10f) {
                        engine.removeEntity(otherProj);
                    }
                }
            }
        }
    }

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (proj == null || target == null || point == null || engine == null) return;

        int attractedCount = 0;
        for (DamagingProjectileAPI otherProj : engine.getProjectiles()) {
            if (otherProj != proj && Vector2f.sub(proj.getLocation(), otherProj.getLocation(), null).length() < 100f) {
                attractedCount++;
            }
        }
        float additionalDamage = attractedCount * 50f; // Adjust the damage multiplier as needed
        engine.applyDamage(target, point, additionalDamage, proj.getDamageType(), 100f, false, true, proj.getSource());
    }
}
