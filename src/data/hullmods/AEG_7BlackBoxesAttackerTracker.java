package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.lwjgl.util.vector.Vector2f;

public class AEG_7BlackBoxesAttackerTracker implements DamageTakenModifier {
    private ShipAPI lastAttacker = null;
    private float lastDamageTime = 0f;

    public ShipAPI getLastAttacker() {
        return lastAttacker;
    }

    public float getLastDamageTime() {
        return lastDamageTime;
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (param instanceof DamagingProjectileAPI) {
            DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
            if (proj.getSource() instanceof ShipAPI) {
                lastAttacker = (ShipAPI) proj.getSource();
                lastDamageTime = Global.getCombatEngine().getTotalElapsedTime(false);
            }
        } else if (param instanceof BeamAPI) {
            BeamAPI beam = (BeamAPI) param;
            if (beam.getSource() instanceof ShipAPI) {
                lastAttacker = (ShipAPI) beam.getSource();
                lastDamageTime = Global.getCombatEngine().getTotalElapsedTime(false);
            }
        }
        return null;
    }
}
