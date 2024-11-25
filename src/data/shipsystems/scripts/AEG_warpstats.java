package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Random;

public class AEG_warpstats extends BaseShipSystemScript {
	private static final float WARP_SPEED = 3000f;
	private static final float MANEUVERABILITY_BOOST = 2f;
	private static final float MANEUVERABILITY_DURATION = 2f;
	private static final Color PARTICLE_COLOR = new Color(105, 255, 105, 255);
	private final Random random = new Random();

	@Override
	public void apply(final MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) return;

		if (state == State.IN) {
			// Charge-up phase: create particles flowing into the engines
			createChargeUpParticles(ship);
		} else if (state == State.ACTIVE) {
			// Warp phase: move the ship to the target ship's location with an offset
			ShipAPI target = ship.getShipTarget();
			if (target == null) {
				target = findNearestEnemy(ship);
			}

			if (target != null) {
				Vector2f targetLocation = getWarpTargetLocation(ship, target);
				Vector2f direction = Vector2f.sub(targetLocation, ship.getLocation(), new Vector2f());
				direction.normalise();
				direction.scale(WARP_SPEED * effectLevel);

				ship.getVelocity().set(direction);
				ship.setCollisionClass(CollisionClass.NONE); // Pass through objects

				// Apply visual effects
				applyVisualEffects(ship);
			} else {
				// No target found, reset cooldown
				ship.getSystem().setCooldownRemaining(ship.getSystem().getCooldown());
			}
		} else if (state == State.OUT) {
			// Post-warp phase: boost maneuverability and reset collision class
			ship.setCollisionClass(CollisionClass.SHIP);
			stats.getMaxTurnRate().modifyMult(id, MANEUVERABILITY_BOOST);
			stats.getTurnAcceleration().modifyMult(id, MANEUVERABILITY_BOOST);
			Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
				private float elapsed = 0f;

				@Override
				public void advance(float amount, List<InputEventAPI> events) {
					elapsed += amount;
					if (elapsed >= MANEUVERABILITY_DURATION) {
						stats.getMaxTurnRate().unmodify(id);
						stats.getTurnAcceleration().unmodify(id);
						Global.getCombatEngine().removePlugin(this);
					}
				}
			});
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("charging warp drive", false);
		} else if (index == 1) {
			return new StatusData("warping to target", false);
		} else if (index == 2) {
			return new StatusData("boosted maneuverability", false);
		}
		return null;
	}

	private void createChargeUpParticles(ShipAPI ship) {
		Vector2f location = ship.getLocation();
		for (int i = 0; i < 50; i++) {
			float angle = random.nextFloat() * 360f;
			float distance = random.nextFloat() * ship.getCollisionRadius();
			float size = random.nextFloat() * 5f + 2f;
			float duration = random.nextFloat() + 0.5f;
			Vector2f velocity = new Vector2f((float) Math.cos(Math.toRadians(angle)) * distance, (float) Math.sin(Math.toRadians(angle)) * distance);
			Global.getCombatEngine().addHitParticle(location, velocity, size, 1f, duration, PARTICLE_COLOR);
		}
	}

	private void applyVisualEffects(ShipAPI ship) {
		float effect = 1.0f; // Full effect level for visual effects
		ship.setJitterUnder(
				ship,
				Color.CYAN,
				0.5f * effect,
				5,
				5 + 5f * effect,
				5 + 10f * effect
		);
		if (Math.random() > 0.9f) {
			ship.addAfterimage(new Color(0, 200, 255, 64), 0, 0, -ship.getVelocity().x, -ship.getVelocity().y, 5 + 50 * effect, 0, 0, 2 * effect, false, false, false);
		}
	}

	private Vector2f getWarpTargetLocation(ShipAPI ship, ShipAPI target) {
		Vector2f targetLocation = new Vector2f(target.getLocation());
		Vector2f offset = Vector2f.sub(targetLocation, ship.getLocation(), new Vector2f());
		offset.normalise();
		offset.scale(target.getCollisionRadius() + ship.getCollisionRadius() + 300f); // Offset to warp 300f behind the target
		Vector2f.add(targetLocation, offset, targetLocation);
		return targetLocation;
	}

	private ShipAPI findNearestEnemy(ShipAPI ship) {
		ShipAPI nearestEnemy = null;
		float minDistance = Float.MAX_VALUE;
		for (ShipAPI enemy : Global.getCombatEngine().getShips()) {
			if (enemy.getOwner() != ship.getOwner() && enemy.isAlive()) {
				float distance = MathUtils.getDistance(ship, enemy);
				if (distance < minDistance) {
					minDistance = distance;
					nearestEnemy = enemy;
				}
			}
		}
		return nearestEnemy;
	}
}