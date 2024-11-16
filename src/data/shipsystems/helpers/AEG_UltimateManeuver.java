package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Random;

public class AEG_UltimateManeuver {

    private static final float DURATION = 10f;
    private static final float DAMAGE_REDUCTION = 0.01f; // 99% damage reduction
    private static final float BLACK_HOLE_RADIUS = 2000f;
    private static final float PULL_STRENGTH = 300f;
    private static final Color BLACK_HOLE_COLOR = new Color(93, 36, 145);
    private static final Color EXPLOSION_COLOR = new Color(200, 255, 161); // White with green fringe
    private static final Color EXPLOSION_FRINGE_COLOR = new Color(25, 153, 25);

    private static Vector2f blackHolePosition;
    private static boolean isActive = false;
    private static float elapsedTime = 0f;

    public static void execute(final ShipAPI ship, final String id) {
        isActive = true;
        elapsedTime = 0f;

        // Lock arms and shoulders
        AEG_MeteorSmash.initializePositions(ship);
        setWeaponAngles(ship);

        // Apply visual effects
        applyVisualEffects(ship);

        // Reduce incoming damage
        ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, DAMAGE_REDUCTION);
        ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, DAMAGE_REDUCTION);
        ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, DAMAGE_REDUCTION);

        // Lock ship in place
        ship.getMutableStats().getMaxSpeed().modifyMult(id, 0f);
        ship.getMutableStats().getAcceleration().modifyMult(id, 0f);
        ship.getMutableStats().getDeceleration().modifyMult(id, 0f);

        // Determine black hole position
        blackHolePosition = MathUtils.getPointOnCircumference(ship.getLocation(), 1500f, ship.getFacing());

        // Add plugin to handle the effect over time
        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (Global.getCombatEngine().isPaused() || !isActive) {
                    return;
                }

                elapsedTime += amount;
                if (elapsedTime >= DURATION) {
                    endEffect(ship, id);
                    Global.getCombatEngine().removePlugin(this);
                    return;
                }

                // Apply black hole pull effect
                applyBlackHolePull();
            }
        });
    }

    private static void setWeaponAngles(ShipAPI ship) {
        float shipFacing = ship.getFacing();
        for (WeaponAPI w : ship.getAllWeapons()) {
            switch (w.getSlot().getId()) {
                case "WS0003":
                    w.setCurrAngle(shipFacing + AEG_MeteorSmash.TARGET_LEFT_ARM_ANGLE);
                    break;
                case "WS0004":
                    w.setCurrAngle(shipFacing + AEG_MeteorSmash.TARGET_RIGHT_ARM_ANGLE);
                    break;
                case "WS0001":
                    w.setCurrAngle(shipFacing + AEG_MeteorSmash.TARGET_LEFT_SHOULDER_ANGLE);
                    break;
                case "WS0002":
                    w.setCurrAngle(shipFacing + AEG_MeteorSmash.TARGET_RIGHT_SHOULDER_ANGLE);
                    break;
                case "WS0013":
                    // Ensure OmegaBlaster weapons glow
                    applyWeaponGlow(w);
                    break;
            }
        }
    }

    private static void applyVisualEffects(ShipAPI ship) {
        ship.setJitterUnder(
                ship,
                BLACK_HOLE_COLOR,
                1.0f,
                10,
                20f,
                40f
        );
        ship.addAfterimage(BLACK_HOLE_COLOR, 0, 0, -ship.getVelocity().x, -ship.getVelocity().y, 50f, 0, 0, 2f, false, false, false);
    }

    private static void applyBlackHolePull() {
        CombatEngineAPI engine = Global.getCombatEngine();
        for (CombatEntityAPI entity : engine.getShips()) {
            if (entity instanceof ShipAPI && entity != engine.getPlayerShip()) {
                float distance = MathUtils.getDistance(entity, blackHolePosition);
                if (distance <= BLACK_HOLE_RADIUS) {
                    Vector2f pullVector = VectorUtils.getDirectionalVector(entity.getLocation(), blackHolePosition);
                    float strength = (1f - distance / BLACK_HOLE_RADIUS) * PULL_STRENGTH;
                    pullVector.scale(strength);
                    Vector2f.add(entity.getVelocity(), pullVector, entity.getVelocity());
                }
            }
        }
    }

    private static void applyWeaponGlow(WeaponAPI weapon) {
        // Apply glow effect to the weapon
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f weaponLocation = weapon.getLocation();
        float weaponFacing = weapon.getCurrAngle();
        Color glowColor = new Color(105, 255, 105, 255); // White glow with some transparency

        // Create a pulsing effect by varying the size and opacity
        for (int i = 0; i < 5; i++) {
            float size = 20f + (float) Math.random() * 10f;
            float opacity = 0.5f + (float) Math.random() * 0.5f;
            engine.addHitParticle(weaponLocation, new Vector2f(), size, opacity, 0.5f, glowColor);
        }
    }

    private static void endEffect(ShipAPI ship, String id) {
        isActive = false;

        // Create final explosion
        CombatEngineAPI engine = Global.getCombatEngine();
        engine.spawnExplosion(blackHolePosition, new Vector2f(), EXPLOSION_COLOR, 1000f, 1f);
        engine.addHitParticle(blackHolePosition, new Vector2f(), 1500f, 1f, 1f, EXPLOSION_FRINGE_COLOR);

        // Deal 10,000 high explosive damage to all entities except the player ship
        for (CombatEntityAPI entity : engine.getShips()) {
            if (entity != ship) {
                engine.applyDamage(entity, blackHolePosition, 10000f, DamageType.HIGH_EXPLOSIVE, 0f, true, false, ship);
            }
        }

        // Put all weapons and systems into a 10-second cooldown
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            weapon.disable(true);
            weapon.setRemainingCooldownTo(5f);
        }
        ship.getSystem().setCooldownRemaining(5f);

        // Reset damage reduction
        ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
        ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
        ship.getMutableStats().getShieldDamageTakenMult().unmodify(id);

        // Reset speed and maneuverability
        ship.getMutableStats().getMaxSpeed().unmodify(id);
        ship.getMutableStats().getAcceleration().unmodify(id);
        ship.getMutableStats().getDeceleration().unmodify(id);

        // Reset arm and shoulder positions
        AEG_MeteorSmash.resetPositions(ship);
    }
}