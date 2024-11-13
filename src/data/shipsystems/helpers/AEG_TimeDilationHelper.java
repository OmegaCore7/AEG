package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;

public class AEG_TimeDilationHelper {

    public static void applyTimeDilation(ShipAPI ship, float duration, float timeMult) {
        Global.getCombatEngine().getTimeMult().modifyMult(ship.getId(), timeMult);
        Global.getCombatEngine().addPlugin(new TimeDilationPlugin(ship, duration, timeMult));
    }

    private static class TimeDilationPlugin extends BaseEveryFrameCombatPlugin {
        private final ShipAPI ship;
        private final float duration;
        private final float timeMult;
        private float elapsed;

        public TimeDilationPlugin(ShipAPI ship, float duration, float timeMult) {
            this.ship = ship;
            this.duration = duration;
            this.timeMult = timeMult;
            this.elapsed = 0f;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (Global.getCombatEngine().isPaused()) {
                return;
            }

            elapsed += amount;
            if (elapsed >= duration) {
                Global.getCombatEngine().getTimeMult().unmodify(ship.getId());
                Global.getCombatEngine().removePlugin(this);
            }
        }
    }
}
