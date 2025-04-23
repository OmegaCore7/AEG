package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

public class AEG_PhotonImpactEffect implements OnHitEffectPlugin {

    Random rand = new Random();


    @Override
    public void onHit(DamagingProjectileAPI projectile,
                      CombatEntityAPI target,
                      Vector2f point,
                      boolean shieldHit,
                      ApplyDamageResultAPI damageResult,
                      CombatEngineAPI engine) {

        // Flash of photonic light
        engine.addHitParticle(
                point,
                projectile.getVelocity(),
                100f - rand.nextInt(60),
                1.0f + rand.nextInt(2),
                0.2f + rand.nextInt(1),
                new Color(255, 255 - rand.nextInt(50), 150 - rand.nextInt(60), 255 - rand.nextInt(50))
        );

        // Explosion (triggered no matter what)
        engine.spawnExplosion(
                point,
                projectile.getVelocity(),
                new Color(255 - rand.nextInt(50), 150 - rand.nextInt(50), 50 - rand.nextInt(50), 255 - rand.nextInt(50)),
                200f - rand.nextInt(100),
                0.3f + rand.nextInt(1)
        );

        // Scatter lightning arcs (unconditionally)
        int arcCount = 3 + rand.nextInt(3); // 3 to 5 arcs
        for (int i = 0; i < arcCount; i++) {
            Vector2f arcPoint = MathUtils.getPointOnCircumference(point, rand.nextFloat() * 30f, rand.nextFloat() * 360f);

            engine.spawnEmpArc(
                    projectile.getSource(),
                    arcPoint,
                    target != null ? target : projectile, // Fallback in case target is null
                    target != null ? target : projectile,
                    DamageType.ENERGY,
                    150f - rand.nextInt(100),
                    150f - rand.nextInt(100),
                    800f + rand.nextInt(400),
                    "tachyon_lance_emp_impact",
                    20f - rand.nextFloat() * 10f,
                    new Color(255, 180 - rand.nextInt(50), 50 - rand.nextInt(50), 255 - rand.nextInt(50)),
                    new Color(255, 225 - rand.nextInt(50), 100 - rand.nextInt(50), 255 - rand.nextInt(50))
            );
        }

        // Crackling photonic particles
        engine.addSmoothParticle(
                MathUtils.getRandomPointInCircle(point, 25f),
                new Vector2f(),
                10f + rand.nextFloat() * 15f,
                1.0f + rand.nextInt(2),
                0.3f + rand.nextInt(1),
                new Color(255, 250, 100, 100 + rand.nextInt(155))
        );
    }
}
