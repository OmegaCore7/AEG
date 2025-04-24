package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class AEG_CausalityListener implements DamageTakenModifier {
    private final ShipAPI ship;
    private final CombatEngineAPI engine;
    private static final float REFLECT_RADIUS = 2500f;
    private static final float HEAL_PERCENTAGE = 0.5f;
    private static final float REFLECT_PERCENTAGE = 0.5f;

    public AEG_CausalityListener(ShipAPI ship, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (target != ship) return null;

        float hullLevel = ship.getHullLevel();
        if (hullLevel <= 0.20f) {
            float damageAmount = damage.getDamage();
            float healAmount = damageAmount * HEAL_PERCENTAGE;
            float reflectAmount = damageAmount * REFLECT_PERCENTAGE;

            ship.setHitpoints(ship.getHitpoints() + healAmount);

            for (ShipAPI other : engine.getShips()) {
                if (other.getOwner() != ship.getOwner() && MathUtils.getDistance(ship, other) <= REFLECT_RADIUS) {
                    engine.applyDamage(other, point, reflectAmount, damage.getType(), 0f, false, false, ship);
                }
            }

            damage.setDamage(0f);
            return "causality_defense";
        }

        return null;
    }
}