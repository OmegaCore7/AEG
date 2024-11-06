package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class AEG_OmegaBlasterOnHit implements OnHitEffectPlugin {

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point,
                      boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target == null || !(target instanceof ShipAPI)) return;

        ShipAPI ship = (ShipAPI) target;
        float damage = projectile.getDamageAmount();

        // Create green explosion and deal damage
        engine.spawnExplosion(point, new Vector2f(0, 0), Misc.setAlpha(new Color(0, 255, 0), 255),
                100f, 1f);
        engine.applyDamage(ship, point, damage, DamageType.ENERGY, 0, false, false, projectile.getSource());

        // Create ring of electricity and deal EMP damage
        for (int i = 0; i < 360; i += 30) {
            float angle = (float) Math.toRadians(i);
            Vector2f offset = new Vector2f((float) Math.cos(angle) * 50, (float) Math.sin(angle) * 50);
            Vector2f spawnPoint = Vector2f.add(point, offset, null);
            engine.spawnEmpArc(projectile.getSource(), point, ship, ship,
                    DamageType.ENERGY, 0, damage, 1000f, null, 10f,
                    new Color(0, 255, 0), new Color(0, 255, 255));
        }
    }
}