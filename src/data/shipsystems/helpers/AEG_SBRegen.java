package data.shipsystems.helpers;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.IntervalUtil;

public class AEG_SBRegen {

    private final ShipAPI ship;
    private final IntervalUtil interval;

    public AEG_SBRegen(ShipAPI ship) {
        this.ship = ship;
        this.interval = new IntervalUtil(0.1f, 0.1f);
    }

    public void advance(float amount) {
        if (!ship.getSystem().isActive()) {
            interval.advance(amount);
            if (interval.intervalElapsed()) {
                // Regenerate hitpoints
                ship.setHitpoints(Math.min(ship.getMaxHitpoints(), ship.getHitpoints() + 10f));

                // Regenerate armor
                for (int i = 0; i < ship.getArmorGrid().getGrid().length; i++) {
                    for (int j = 0; j < ship.getArmorGrid().getGrid()[i].length; j++) {
                        float currentArmor = ship.getArmorGrid().getArmorValue(j, i);
                        float maxArmor = ship.getArmorGrid().getMaxArmorInCell();
                        ship.getArmorGrid().setArmorValue(j, i, Math.min(currentArmor + maxArmor * 0.025f, maxArmor));
                    }
                }
            }
        }
    }
}
