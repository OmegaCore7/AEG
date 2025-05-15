package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class AEG_CausalityListener implements DamageTakenModifier {
    private ShipAPI lastAttacker = null;
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

// Capture attacker if possible
        if (param instanceof DamagingProjectileAPI) {
            DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
            if (proj.getSource() instanceof ShipAPI) {
                lastAttacker = proj.getSource();
            }
        } else if (param instanceof BeamAPI) {
            BeamAPI beam = (BeamAPI) param;
            if (beam.getSource() instanceof ShipAPI) {
                lastAttacker = beam.getSource();
            }
        }

        float hullLevel = ship.getHullLevel();
        if (hullLevel <= 0.20f) {
            engine.addFloatingText(ship.getLocation(), "Causality Defense!", 24f, java.awt.Color.MAGENTA, ship, 1f, 1f);
            if (lastAttacker != null &&
                    lastAttacker.getLocation() != null &&
                    ship.getLocation() != null) {

                engine.spawnEmpArcVisual(
                        ship.getLocation(), ship,                           // FROM: your ship
                        lastAttacker.getLocation(), lastAttacker,           // TO: the attacker
                        5f + MathUtils.getRandom().nextInt(15),
                        new java.awt.Color(255, 150 - MathUtils.getRandom().nextInt(65), 50 - MathUtils.getRandom().nextInt(50), 255 - MathUtils.getRandom().nextInt(85)),
                        new java.awt.Color(255, 200 - MathUtils.getRandom().nextInt(65), 100 - MathUtils.getRandom().nextInt(65), 255 - MathUtils.getRandom().nextInt(65))
                );
            }

// Optional: Play a unique sound
            Global.getSoundPlayer().playSound("system_phase_cloak_activate", 1f, 1f, ship.getLocation(), ship.getVelocity());
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