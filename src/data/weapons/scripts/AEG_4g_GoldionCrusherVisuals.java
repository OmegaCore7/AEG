package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_4g_GoldionCrusherVisuals {

    private static final Color BUILDUP_COLOR = new Color(255, 200, 50, 200);
    private static final Color IMPACT_COLOR = new Color(255, 80, 0, 255);
    private static final float HAMMER_LENGTH = 85f;
    private static final float HAMMER_WIDTH = 35f;
    private static final float IMPACT_RADIUS = 150f;

    public static void renderHammerCharge(CombatEngineAPI engine, Vector2f origin, float angle, float chargeLevel) {
        if (chargeLevel <= 0f) return;

        // Offset for hammerhead tip
        Vector2f tip = MathUtils.getPointOnCircumference(origin, HAMMER_LENGTH, angle);
        Color trailColor = new Color(BUILDUP_COLOR.getRed(), BUILDUP_COLOR.getGreen(), BUILDUP_COLOR.getBlue(),
                (int) (BUILDUP_COLOR.getAlpha() * chargeLevel));

        // Glowing buildup trail
        engine.addSmoothParticle(tip, new Vector2f(0, 0), 60f + 40f * chargeLevel, 1f, 0.1f, trailColor);
        engine.addHitParticle(tip, new Vector2f(), 25f + 25f * chargeLevel, 0.8f, 0.15f, trailColor);
    }

    public static void triggerImpactFX(CombatEngineAPI engine, Vector2f origin, float angle, ShipAPI ship) {
        Vector2f tip = MathUtils.getPointOnCircumference(origin, HAMMER_LENGTH, angle);
        Vector2f vel = ship.getVelocity();

        // Slam explosion & shockwave
        engine.spawnExplosion(tip, vel, IMPACT_COLOR, IMPACT_RADIUS, 0.25f);
        engine.addSmoothParticle(tip, vel, IMPACT_RADIUS * 2f, 1.5f, 0.3f, IMPACT_COLOR);
        engine.addHitParticle(tip, vel, 90f, 1.2f, 0.2f, IMPACT_COLOR);

        // Optional: EMP arcs or screen shake

        Global.getSoundPlayer().playSound("rifttorpedo_explosion", 1f, 1.5f, tip, new Vector2f());
        Global.getSoundPlayer().playSound("realitydisruptor_emp_impact", 1f, 1.5f, tip, new Vector2f());

        // 🔄 ShaderLib wave distortion
        WaveDistortion wave = new WaveDistortion(tip, new Vector2f());
        wave.setIntensity(30f);      // shock strength
        wave.setSize(200f);          // radius of ripple
        wave.setLifetime(0.4f);      // duration
        wave.setArc(0f, 360f);       // full circle
        wave.fadeOutIntensity(0.5f); // smooth fade
        DistortionShader.addDistortion(wave);
    }
}
