package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.Global;;
public class AEG_warpstats extends BaseShipSystemScript {
	// Constants for time acceleration
	private static final float MAX_TIME_MULT = 20f;
	private static final boolean UNMODIFY_ON_VENT = true;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
		} else {
			stats.getMaxSpeed().modifyFlat(id, 5000f * effectLevel);
//			stats.getMaxTurnRate().modifyFlat(id, 500f * effectLevel);
//			stats.getTurnAcceleration().modifyFlat(id, 1000f * effectLevel);
			stats.getAcceleration().modifyFlat(id, 5000f * effectLevel);
		}

// Time acceleration code
		ShipAPI ship = getShip(stats);
		if (ship == null) {
			return;
		}

		if (UNMODIFY_ON_VENT && ship.getFluxTracker().isVenting()) {
			stats.getTimeMult().unmodify(id);
			Global.getCombatEngine().getTimeMult().unmodify(id);
			return;
		}

		float timeMult = 1f + (MAX_TIME_MULT - 1f) * effectLevel;
		stats.getTimeMult().modifyMult(id, timeMult);
		if (ship == Global.getCombatEngine().getPlayerShip()) {
			Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / timeMult);
		} else {
			Global.getCombatEngine().getTimeMult().unmodify(id);
		}
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
//		stats.getMaxTurnRate().unmodify(id);
//		stats.getTurnAcceleration().unmodify(id);
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("increased engine power", false);
		}
		return null;
	}


	public float getActiveOverride(ShipAPI ship) {
//		if (ship.getHullSize() == HullSize.FRIGATE) {
//			return 1.25f;
//		}
//		if (ship.getHullSize() == HullSize.DESTROYER) {
//			return 0.75f;
//		}
//		if (ship.getHullSize() == HullSize.CRUISER) {
//			return 0.5f;
//		}
		return -1;
	}

	public float getInOverride(ShipAPI ship) {
		return -1;
	}

	public float getOutOverride(ShipAPI ship) {
		return -1;
	}

	public float getRegenOverride(ShipAPI ship) {
		return -1;
	}

	public int getUsesOverride(ShipAPI ship) {
		if (ship.getHullSize() == HullSize.FRIGATE) {
			return 2;
		}
		if (ship.getHullSize() == HullSize.DESTROYER) {
			return 2;
		}
		if (ship.getHullSize() == HullSize.CRUISER) {
			return 2;
		}
		return -1;
	}

	private ShipAPI getShip(MutableShipStatsAPI stats) {
		if (stats.getEntity() instanceof ShipAPI) {
			return (ShipAPI) stats.getEntity();
		}
		return null;
	}
}


