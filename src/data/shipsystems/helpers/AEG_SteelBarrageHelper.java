package data.shipsystems.helpers;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector2f;

public class AEG_SteelBarrageHelper {

    private static final float RAM_FORCE = 450f;
    private static final float RAM_DAMAGE = 2000f;
    private static final float COLLISION_THRESHOLD = 75f;
    public static final float[] MANEUVER_STEPS = {500f, 200f, 300f, -500f, -200f};

    public static void performManeuvers(ShipAPI ship, ShipAPI target, int maneuverStep) {
        Vector2f direction = Vector2f.sub(target.getLocation(), ship.getLocation(), null);
        direction.normalise();
        Vector2f perpendicular = new Vector2f(-direction.y, direction.x);

        float shipSizeFactor = ship.getCollisionRadius() / 100f;

        Vector[] maneuvers = {
                new Vector2f(perpendicular).scale(MANEUVER_STEPS[0] * shipSizeFactor),
                new Vector2f(direction).scale(MANEUVER_STEPS[1] * shipSizeFactor),
                new Vector2f(direction).scale(MANEUVER_STEPS[2] * shipSizeFactor),
                new Vector2f(perpendicular).scale(MANEUVER_STEPS[3] * shipSizeFactor),
                new Vector2f(direction).scale(MANEUVER_STEPS[4] * shipSizeFactor)
        };

        if (maneuverStep < maneuvers.length) {
            Vector2f.add(ship.getVelocity(), (Vector2f) maneuvers[maneuverStep], ship.getVelocity());
        }
    }

    public static void applyRammingForceAndDamage(ShipAPI ship, ShipAPI target, String id, float effectLevel) {
        Vector2f diff = Vector2f.sub(target.getLocation(), ship.getLocation(), null);
        diff.normalise();
        diff.scale(RAM_FORCE * effectLevel);
        ship.getVelocity().set(diff);

        float distance = Vector2f.sub(target.getLocation(), ship.getLocation(), null).length();
        if (distance <= COLLISION_THRESHOLD) {
            if (target.getShield() != null && target.getShield().isOn()) {
                target.getFluxTracker().increaseFlux(RAM_DAMAGE * 2f * effectLevel, true);
            } else {
                float armorValue = target.getArmorGrid().getArmorRating() * target.getArmorGrid().getMaxArmorInCell();
                if (armorValue > 0) {
                    float effectiveDamage = RAM_DAMAGE * effectLevel * (1f - armorValue / (armorValue + RAM_DAMAGE));
                    float minDamage = RAM_DAMAGE * 0.1f * effectLevel;
                    effectiveDamage = Math.max(effectiveDamage, minDamage);

                    int x = (int) target.getLocation().x;
                    int y = (int) target.getLocation().y;
                    target.getArmorGrid().setArmorValue(x, y, Math.max(0f, armorValue - effectiveDamage));
                } else {
                    target.getMutableStats().getHullDamageTakenMult().modifyMult(id, 1f + (RAM_DAMAGE * effectLevel / target.getMaxHitpoints()));
                }
            }
        }
    }
}
