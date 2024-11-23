package data.hullmods;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipCommand;

public class AEG_SpiralEffectedMissile implements MissileAIPlugin, GuidedMissileAI {
    private CombatEntityAPI target;
    private final MissileAPI missile;

    public AEG_SpiralEffectedMissile(MissileAPI missile, CombatEntityAPI target) {
        this.missile = missile;
        setTarget(target);
    }

    @Override
    public void advance(float amount) {
        missile.giveCommand(ShipCommand.ACCELERATE);
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }
}