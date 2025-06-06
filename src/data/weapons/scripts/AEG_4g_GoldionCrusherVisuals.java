package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_4g_GoldionCrusherVisuals implements BeamEffectPlugin {

    private static final Color BUILDUP_COLOR = new Color(255, 200, 50, 200);
    private static final Color IMPACT_COLOR = new Color(255, 80, 0, 255);

    private static final float IMPACT_RADIUS = 500f;

    private boolean hasFired = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine == null || beam == null || beam.getWeapon() == null || beam.getWeapon().getShip() == null) return;

        WeaponAPI weapon = beam.getWeapon();
        ShipAPI ship = weapon.getShip();
        Vector2f origin = beam.getFrom();
        float angle = weapon.getCurrAngle();
        float charge = weapon.getChargeLevel();

        // === Crusher Mode
            //beam.setWidth(0f); // Beam is invisible in Crusher mode

            // No FX, no AOE, no visuals for now
            if (!weapon.isFiring()) {
                hasFired = false;
            }
    }
    //Here just for Impact options for use
    private void triggerImpactFX(CombatEngineAPI engine, BeamAPI beam, ShipAPI ship, float beamWidth) {
        Vector2f beamEnd = beam.getTo();
        Vector2f vel = ship.getVelocity();

        if (beamWidth > 0f) {
            engine.spawnExplosion(beamEnd, vel, IMPACT_COLOR, IMPACT_RADIUS, 0.25f);
            engine.addSmoothParticle(beamEnd, vel, IMPACT_RADIUS * 2f, 1.5f, 0.3f, IMPACT_COLOR);
            engine.addHitParticle(beamEnd, vel, 90f, 1.2f, 0.2f, IMPACT_COLOR);
        }

        Global.getSoundPlayer().playSound("rifttorpedo_explosion", 1f, 1.5f, beamEnd, new Vector2f());
        Global.getSoundPlayer().playSound("realitydisruptor_emp_impact", 1f, 1.5f, beamEnd, new Vector2f());

        WaveDistortion wave = new WaveDistortion(beamEnd, new Vector2f());
        wave.setIntensity(30f);
        wave.setSize(500f);
        wave.setLifetime(0.4f);
        wave.setArc(0f, 360f);
        wave.fadeOutIntensity(0.5f);
        DistortionShader.addDistortion(wave);
    }
}
