package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class AEG_4G_GoldionModePlugin extends BaseHullMod {

    // Tracks whether the key combo was down last frame to prevent repeat activations
    private boolean wasKeyPressedLastFrame = false;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()) return;

        // Only care about player ship
        ShipAPI player = Global.getCombatEngine().getPlayerShip();
        if (ship != player) return;

        // Setup listener if not already added
        if (!ship.hasListenerOfClass(HealthThresholdListener.class)) {
            HealthThresholdListener listener = new HealthThresholdListener(ship);
            ship.addListener(listener);
            ship.getCustomData().put("goldion_aura_listener", listener);
        }

        HealthThresholdListener listener = (HealthThresholdListener) ship.getCustomData().get("goldion_aura_listener");

        // Detect input (Shift + W), only trigger on key press edge
        boolean shiftDown = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        boolean wDown = Keyboard.isKeyDown(Keyboard.KEY_W);
        boolean keyComboDown = shiftDown && wDown;

        if (keyComboDown && !wasKeyPressedLastFrame) {
            listener.tryActivateGoldionMode();
        }

        wasKeyPressedLastFrame = keyComboDown;

        listener.advance(amount);
    }

    // -- Listener that handles Goldion behavior --
    private static class HealthThresholdListener implements DamageListener {
        private final ShipAPI ship;

        private boolean goldionActive = false;
        private float goldionTimer = 0f;
        private boolean crusherFired = false;

        private static final float GOLDION_TOTAL_DURATION = 15f;
        private static final float CRUSHER_IMPACT_TIME = 10f;

        public HealthThresholdListener(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            // No-op
        }

        public void tryActivateGoldionMode() {
            if (!goldionActive) {
                activateGoldionMode();
            }
        }

        public void advance(float amount) {
            if (!goldionActive) return;

            goldionTimer += amount;

            applyAuraEffects();        // Remove projectiles
            applyCrusherChargeFX();    // Charging visuals

            if (goldionTimer >= CRUSHER_IMPACT_TIME && !crusherFired) {
                fireGoldionCrusher();
                crusherFired = true;
            }

            if (goldionTimer >= GOLDION_TOTAL_DURATION) {
                deactivateGoldionMode();
            }
        }

        private void activateGoldionMode() {
            goldionActive = true;
            goldionTimer = 0f;
            crusherFired = false;

            // Visual effects
            ship.setVentFringeColor(new Color(255, 223, 70, 200));
            ship.setVentCoreColor(new Color(255, 190, 0, 180));
            ship.setJitter(ship, new Color(255, 215, 0, 75), 1f, 3, 10f, 20f);

            Global.getSoundPlayer().playSound("vent_flux", 1f, 1f, ship.getLocation(), ship.getVelocity());
            Global.getSoundPlayer().playSound("shield_raise", 1f, 1f, ship.getLocation(), ship.getVelocity());

            Global.getCombatEngine().maintainStatusForPlayerShip(
                    "goldion_mode", "graphics/icons/hullsys/high_energy_focus.png",
                    "Goldion Crusher", "Charging...", false
            );

            if (!ship.hasListenerOfClass(GoldionDamageReducer.class)) {
                ship.addListener(new GoldionDamageReducer());
            }
        }

        private void deactivateGoldionMode() {
            goldionActive = false;
            ship.setVentFringeColor(null);
            ship.setVentCoreColor(null);
        }

        private void applyAuraEffects() {
            for (DamagingProjectileAPI proj : Global.getCombatEngine().getProjectiles()) {
                if (MathUtils.getDistance(ship, proj) < ship.getCollisionRadius() + 50f) {
                    Global.getCombatEngine().removeEntity(proj);
                    spawnGoldionParticle(proj.getLocation());
                }
            }
        }

        private void spawnGoldionParticle(Vector2f loc) {
            Global.getCombatEngine().addSmoothParticle(
                    loc, new Vector2f(), 20f, 1.0f, 0.75f,
                    new Color(255, 215, 0, 200)
            );
        }

        private void applyCrusherChargeFX() {
            float intensity = Math.min(1f, goldionTimer / CRUSHER_IMPACT_TIME);
            Global.getCombatEngine().addSmoothParticle(
                    ship.getLocation(),
                    new Vector2f(),
                    80f + goldionTimer * 4f,
                    1.2f,
                    0.2f,
                    new Color(255, 200, 0, 150)
            );

            if (goldionTimer > CRUSHER_IMPACT_TIME - 1f) {
                ship.setJitterUnder(ship, new Color(255, 255, 255, 255), 2f, 10, 15f, 30f);
            }
        }

        private void fireGoldionCrusher() {
            Global.getCombatEngine().spawnExplosion(
                    ship.getLocation(),
                    ship.getVelocity(),
                    new Color(255, 255, 200),
                    300f,
                    2f
            );

            for (ShipAPI enemy : Global.getCombatEngine().getShips()) {
                if (enemy.isAlive() && enemy.getOwner() != ship.getOwner()) {
                    float dist = MathUtils.getDistance(ship, enemy);
                    if (dist < 600f) {
                        float damage = 1500f * (1f - dist / 600f);
                        Global.getCombatEngine().applyDamage(enemy, enemy.getLocation(), damage,
                                DamageType.HIGH_EXPLOSIVE, 0f, false, false, ship);
                    }
                }
            }

            Global.getCombatEngine().maintainStatusForPlayerShip(
                    "goldion_crusher_fire", "graphics/icons/hullsys/high_energy_focus.png",
                    "Goldion Crusher", "Impact!", false
            );

            ship.setJitter(ship, new Color(255, 255, 150, 255), 2f, 8, 20f, 40f);
        }
    }

    private static class GoldionDamageReducer implements DamageTakenModifier {
        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (damage.getType() == DamageType.ENERGY) {
                damage.getModifier().modifyMult("goldion_beam", 0.05f); // Reduce to 5%
            }
            return null;
        }
    }
}
