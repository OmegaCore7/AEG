package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Random;

public class AEG_warpstats extends BaseShipSystemScript {
	private static final float WARP_SPEED = 50f;
	private static final float MANEUVERABILITY_BUFF = 100f;
	private static final float SPEED_BUFF = 100f;
	private static final float BUFF_DURATION = 3f;
	private final Random random = new Random();

	@Override
	public void apply(final MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
		final ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) return;

		final Color engineColor = ship.getEngineController().getFlameColorShifter().getBase();

		if (state == State.ACTIVE) {
			// Warp phase: apply warp effects and move the ship
			ship.setCollisionClass(CollisionClass.NONE); // Pass through objects
			Vector2f direction = new Vector2f((float) Math.cos(Math.toRadians(ship.getFacing())), (float) Math.sin(Math.toRadians(ship.getFacing())));
			direction.scale(WARP_SPEED * effectLevel);
			Vector2f newVelocity = Vector2f.add(ship.getVelocity(), direction, new Vector2f());
			ship.getVelocity().set(newVelocity);
			applyVisualEffects(ship, engineColor);
		} else if (state == State.OUT) {
			// Post-warp phase: reset collision class and apply buffs
			ship.setCollisionClass(CollisionClass.SHIP);
			stats.getMaxSpeed().modifyFlat(id, SPEED_BUFF);
			stats.getAcceleration().modifyFlat(id, MANEUVERABILITY_BUFF);
			stats.getDeceleration().modifyFlat(id, MANEUVERABILITY_BUFF);
			stats.getTurnAcceleration().modifyFlat(id, MANEUVERABILITY_BUFF);
			stats.getMaxTurnRate().modifyFlat(id, MANEUVERABILITY_BUFF);
			Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
				private float elapsed = 0f;

				@Override
				public void advance(float amount, List<InputEventAPI> events) {
					if (Global.getCombatEngine().isPaused()) {
						return;
					}

					elapsed += amount;
					if (elapsed >= BUFF_DURATION) {
						stats.getMaxSpeed().unmodify(id);
						stats.getAcceleration().unmodify(id);
						stats.getDeceleration().unmodify(id);
						stats.getTurnAcceleration().unmodify(id);
						stats.getMaxTurnRate().unmodify(id);
						Global.getCombatEngine().removePlugin(this);
					} else {
						applyCoolingDownEffects(ship, engineColor);
					}
				}

				@Override
				public void init(CombatEngineAPI engine) {
					// No initialization needed
				}
			});
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		// Remove buffs if the system is turned off prematurely
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("warping to target", false);
		} else if (index == 1) {
			return new StatusData("cooling down", false);
		}
		return null;
	}

	private void applyVisualEffects(ShipAPI ship, Color engineColor) {
		float effect = 1.0f; // Full effect level for visual effects
		ship.setJitterUnder(
				ship,
				engineColor,
				0.5f * effect,
				5,
				5 + 5f * effect,
				5 + 10f * effect
		);
		if (Math.random() > 0.9f) {
			ship.addAfterimage(new Color(engineColor.getRed(), engineColor.getGreen(), engineColor.getBlue(), 64), 0, 0, -ship.getVelocity().x, -ship.getVelocity().y, 5 + 50 * effect, 0, 0, 2 * effect, false, false, false);
		}
	}

	private void applyCoolingDownEffects(ShipAPI ship, Color engineColor) {
		for (int i = 0; i < 10; i++) {
			float angle = random.nextFloat() * 360f;
			float distance = random.nextFloat() * 50f;
			float size = 5f + random.nextFloat() * 5f;
			float duration = random.nextFloat() + 0.5f;
			Vector2f startPosition = new Vector2f(
					ship.getLocation().x + (float) Math.cos(Math.toRadians(angle)) * distance,
					ship.getLocation().y + (float) Math.sin(Math.toRadians(angle)) * distance
			);
			Vector2f velocity = new Vector2f(
					(float) Math.cos(Math.toRadians(angle)) * 100f,
					(float) Math.sin(Math.toRadians(angle)) * 100f
			);
			Global.getCombatEngine().addHitParticle(startPosition, velocity, size, 1f, duration, engineColor);
		}
	}
}