package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_4g_GoldionCrusherVisuals implements BeamEffectPlugin {

    private static final Color BUILDUP_COLOR = new Color(255, 200, 50, 200);
    private static final Color IMPACT_COLOR = new Color(255, 80, 0, 255);
    private static final float HAMMER_LENGTH = 85f;
    private static final float IMPACT_RADIUS = 150f;

    private boolean hasFired = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine == null || beam == null || beam.getWeapon() == null || beam.getWeapon().getShip() == null) return;

        Vector2f origin = beam.getFrom(); // beam starting point
        float angle = beam.getWeapon().getCurrAngle();
        ShipAPI ship = beam.getWeapon().getShip();
        float charge = beam.getWeapon().getChargeLevel();

        // Charge-up visuals
        renderHammerCharge(engine, origin, angle, charge);

        // One-time impact FX when fully charged
        if (charge >= 1f && !hasFired) {
            hasFired = true;
            triggerImpactFX(engine, origin, angle, ship);
        }

        // Reset when beam stops
        if (!beam.getWeapon().isFiring()) {
            hasFired = false;
        }
    }

    private void renderHammerCharge(CombatEngineAPI engine, Vector2f origin, float angle, float chargeLevel) {
        if (chargeLevel <= 0f) return;

        Vector2f tip = MathUtils.getPointOnCircumference(origin, HAMMER_LENGTH, angle);
        Color trailColor = new Color(BUILDUP_COLOR.getRed(), BUILDUP_COLOR.getGreen(), BUILDUP_COLOR.getBlue(),
                (int) (BUILDUP_COLOR.getAlpha() * chargeLevel));

        engine.addSmoothParticle(tip, new Vector2f(0, 0), 60f + 40f * chargeLevel, 1f, 0.1f, trailColor);
        engine.addHitParticle(tip, new Vector2f(), 25f + 25f * chargeLevel, 0.8f, 0.15f, trailColor);
    }

    private void triggerImpactFX(CombatEngineAPI engine, Vector2f origin, float angle, ShipAPI ship) {
        Vector2f tip = MathUtils.getPointOnCircumference(origin, HAMMER_LENGTH, angle);
        Vector2f vel = ship.getVelocity();

        engine.spawnExplosion(tip, vel, IMPACT_COLOR, IMPACT_RADIUS, 0.25f);
        engine.addSmoothParticle(tip, vel, IMPACT_RADIUS * 2f, 1.5f, 0.3f, IMPACT_COLOR);
        engine.addHitParticle(tip, vel, 90f, 1.2f, 0.2f, IMPACT_COLOR);

        Global.getSoundPlayer().playSound("rifttorpedo_explosion", 1f, 1.5f, tip, new Vector2f());
        Global.getSoundPlayer().playSound("realitydisruptor_emp_impact", 1f, 1.5f, tip, new Vector2f());

        WaveDistortion wave = new WaveDistortion(tip, new Vector2f());
        wave.setIntensity(30f);
        wave.setSize(200f);
        wave.setLifetime(0.4f);
        wave.setArc(0f, 360f);
        wave.fadeOutIntensity(0.5f);
        DistortionShader.addDistortion(wave);
    }
}
