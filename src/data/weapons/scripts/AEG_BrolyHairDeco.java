package data.weapons.scripts;
import com.fs.starfarer.api.combat.*;

public class AEG_BrolyHairDeco implements EveryFrameWeaponEffectPlugin {
    private int frameIndex = 0;
    private float frameDuration = 0.1f; // Duration for each frame
    private float timeSinceLastFrame = 0f;
    private boolean initialSequencePlayed = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        WeaponAPI headWeapon = null;
        for (WeaponAPI w : weapon.getShip().getAllWeapons()) {
            if ("AEG_broly_head".equals(w.getId())) {
                headWeapon = w;
                break;
            }
        }

        if (headWeapon != null) {
            weapon.setCurrAngle(headWeapon.getCurrAngle());
        }

        timeSinceLastFrame += amount;
        if (timeSinceLastFrame >= frameDuration) {
            timeSinceLastFrame = 0f;
            frameIndex++;
            if (!initialSequencePlayed) {
                if (frameIndex > 12) {
                    frameIndex = 6;
                    initialSequencePlayed = true;
                }
            } else {
                if (frameIndex > 12) {
                    frameIndex = 6;
                }
            }
            weapon.getAnimation().setFrame(frameIndex);
        }
    }
}