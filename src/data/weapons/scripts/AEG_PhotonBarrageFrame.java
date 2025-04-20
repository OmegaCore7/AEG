package data.weapons.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class AEG_PhotonBarrageFrame implements EveryFrameScript {

    private static final Random RAND = new Random();
    private static final float BASE_SPEED = 1000f;
    private final Set<DamagingProjectileAPI> processed = new HashSet<>();

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) return;

        List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
        for (DamagingProjectileAPI proj : projectiles) {
            if (proj.isFading() || proj.didDamage() || processed.contains(proj)) continue;

            // Filter only your custom projectile type
            if (!"AEG_bp_shot".equals(proj.getProjectileSpecId())) continue;

            processed.add(proj);

            // Randomize angle ±15°
            float newAngle = proj.getFacing() + (RAND.nextFloat() - 0.5f) * 30f;
            Vector2f newVel = Misc.getUnitVectorAtDegreeAngle(newAngle);
            float speedFactor = 0.7f + RAND.nextFloat() * 0.3f;
            newVel.scale(BASE_SPEED * speedFactor);

            proj.setFacing(newAngle);
            proj.getVelocity().set(newVel);

            // Nebula puff with depth via color/alpha variation
            float size = 20f + RAND.nextFloat() * 20f;
            Color puffColor = new Color(
                    255,
                    150 + RAND.nextInt(80),
                    50 + RAND.nextInt(40),
                    160 + RAND.nextInt(95)
            );

            engine.addNebulaParticle(
                    proj.getLocation(),
                    proj.getVelocity(),
                    size,
                    1.5f,
                    0.2f,
                    0.4f,
                    0.6f,
                    puffColor
            );
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }
}
