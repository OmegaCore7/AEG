package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_4g_GoldionCrusherVisuals implements BeamEffectPlugin {


    private boolean hasFired = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine == null || beam == null || beam.getWeapon() == null || beam.getWeapon().getShip() == null) return;

        WeaponAPI weapon = beam.getWeapon();


        // === Crusher Mode
        //beam.setWidth(0f); // Beam is invisible in Crusher mode

       // Global.getSoundPlayer().playSound("rifttorpedo_explosion", 1f, 1.5f, beamEnd, new Vector2f());
        // Global.getSoundPlayer().playSound("realitydisruptor_emp_impact", 1f, 1.5f, beamEnd, new Vector2f());
    }
}
