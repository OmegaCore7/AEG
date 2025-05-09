package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AEG_4g_right_willknife implements BeamEffectPlugin {

    private IntervalUtil fireInterval = new IntervalUtil(0.05f, 0.05f);
    private final Vector2f ZERO = new Vector2f();
    private final float BLADE_KNOCKBACK_MAX = 200f;
    private static final Color PARTICLE_COLOR = new Color(111,250,236,255);
    private static final float PARTICLE_SIZE = 8f;
    private static final float PARTICLE_BRIGHTNESS = 150f;
    private static final float PARTICLE_DURATION = .8f;
    private static final int PARTICLE_COUNT = 8;
    private static final float PARTICLE_SIZE_MIN = 1f;
    private static final float PARTICLE_SIZE_MAX = 5f;
    private static final float PARTICLE_DURATION_MIN = 0.4f;
    private static final float PARTICLE_DURATION_MAX = 0.9f;
    private static final float PARTICLE_INERTIA_MULT = 0.5f;
    private static final float PARTICLE_DRIFT = 50f;
    private static final float PARTICLE_DENSITY = 0.15f;
    private static final float PARTICLE_SPAWN_WIDTH_MULT = 0.1f;
    private static final float CONE_ANGLE = 150f;
    private static final float VEL_MIN = 0.1f;
    private static final float VEL_MAX = 0.3f;
    private static final float A_2 = CONE_ANGLE / 2;
    private float arc = 20f;
    private float level = 0f;
    private boolean firstStrike = false;
    private boolean firstTrail = false;
    private float id;
    private float id2;
    private boolean runOnce = false;
    private boolean runOnce2 = false;
    private WeaponAPI weapon;
    private List<CombatEntityAPI> targets = new ArrayList<CombatEntityAPI>();
    private List<CombatEntityAPI> hitTargets = new ArrayList<CombatEntityAPI>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        weapon = beam.getWeapon();
        ShipAPI ship = weapon.getShip();
        if (!runOnce) {
            id = MagicTrailPlugin.getUniqueID();
            id2 = MagicTrailPlugin.getUniqueID();
            runOnce = true;
        }
        if (weapon.getChargeLevel() >= 1f) {
            if (!runOnce2)
                runOnce2 = true;
        }

        float beamWidth = beam.getWidth();

        beam.getDamage().setDamage(0);
        CombatEntityAPI target = beam.getDamageTarget();
        if (!targets.contains(target)) {
            targets.add(target);
        }

        Vector2f spawnPoint = MathUtils.getRandomPointOnLine(beam.getFrom(), beam.getTo());
        Vector2f dir = Vector2f.sub(beam.getTo(), beam.getFrom(), new Vector2f());
        Vector2f point = Vector2f.sub(beam.getTo(), dir, new Vector2f());
        if (weapon.isFiring()) {
            if (Math.random() >= 0.75f && beam.getBrightness() >= 0.8f)
                for (int x = 0; x < 2; x++) {
                    engine.addHitParticle(beam.getFrom(),
                            MathUtils.getPointOnCircumference(weapon.getShip().getVelocity(), MathUtils.getRandomNumberInRange(100f, 150f),
                                    MathUtils.getRandomNumberInRange(weapon.getCurrAngle() - 30f, weapon.getCurrAngle() + 30f)),
                            5f, 1f, MathUtils.getRandomNumberInRange(0.1f, 0.6f), beam.getFringeColor());
                }
            for (CombatEntityAPI enemy : targets) {
                if (enemy == beam.getDamageTarget()) {
                    if (hitTargets.contains(beam.getDamageTarget())) {
                        continue;
                    } else {
                        boolean softFlux = true;

                        if (weapon.getDamage().isForceHardFlux() || weapon.getShip().getVariant().getHullMods().contains("high_scatter_amp")) {
                            softFlux = false;
                        }

                        float dmg = weapon.getDamage().getDamage() * weapon.getSpec().getBurstDuration() * (weapon.getShip().getMutableStats().getBeamWeaponDamageMult().computeMultMod() + weapon.getShip().getMutableStats().getBeamWeaponDamageMult().getPercentMod() / 100f) * (weapon.getShip().getMutableStats().getEnergyWeaponDamageMult().computeMultMod() + weapon.getShip().getMutableStats().getEnergyWeaponDamageMult().getPercentMod() / 100f);

                        float mag = weapon.getShip().getFluxBasedEnergyWeaponDamageMultiplier() - 1f;
                        if (mag > 0)
                            dmg = dmg * (1 + mag);
                        if (beam.getLength() > beam.getWeapon().getOriginalSpec().getMaxRange() * 1.5f)
                            dmg *= Math.max(0.5f, beam.getWeapon().getOriginalSpec().getMaxRange() / beam.getLength());

                        engine.applyDamage(enemy, beam.getTo(), dmg, weapon.getDamageType(), dmg / 2, false, softFlux, weapon.getShip());
                        hitTargets.add(enemy);
                    }
                }
            }

            fireInterval.advance(amount);

            if (fireInterval.intervalElapsed() && beam.getBrightness() == 1f) {
                float angle = weapon.getCurrAngle() - 90f;
                if (MagicRender.screenCheck(0.2f, beam.getFrom())) {
                    Vector2f midpoint = new Vector2f((beam.getFrom().x + beam.getTo().x) / 2f, (beam.getFrom().y + beam.getTo().y) / 2f);
                    MagicTrailPlugin.addTrailMemberAdvanced(
                            weapon.getShip(), id2, Global.getSettings().getSprite("fx", "base_trail_smooth"),
                            midpoint,
                            0f,
                            0f,
                            angle,
                            weapon.getShip().getAngularVelocity(),
                            0f,
                            beam.getLength() * 2, beam.getLength() * 2,
                            beam.getCoreColor(), beam.getFringeColor(), 1f,
                            amount, amount, .1f,
                            true,
                            256f, 0f, 0f,
                            null, null, null, 2f);
                }
            }
        }

        if (target instanceof CombatEntityAPI) {
            float dur = beam.getDamage().getDpsDuration();

            if (!firstStrike) {
                point = beam.getTo();
                float variance = MathUtils.getRandomNumberInRange(-0.3f, .3f);
                Global.getSoundPlayer().playSound("armaa_saber_slash", 1.1f + variance, 1f + variance, point, ZERO);
                firstStrike = true;
                float speed = 500f;
                float facing = beam.getWeapon().getCurrAngle();
                if (MagicRender.screenCheck(0.2f, point)) {
                    SpriteAPI waveSprite = Global.getSettings().getSprite("misc", "armaa_sfxpulse");
                    if (waveSprite != null) {
                        MagicRender.battlespace(
                                waveSprite,
                                beam.getTo(),
                                new Vector2f(),
                                new Vector2f(10f, 10f),
                                new Vector2f(150f, 150f),
                                15f,
                                15f,
                                beam.getFringeColor(),
                                true,
                                .3f,
                                0f,
                                .3f
                        );
                    }
                    Color color = beam.getFringeColor();
                    Color core = beam.getCoreColor();
                    for (int i = 0; i <= PARTICLE_COUNT; i++) {
                        float radius = 10f + (weapon.getChargeLevel() * weapon.getChargeLevel() * MathUtils.getRandomNumberInRange(25f, 75f));
                        float angle = MathUtils.getRandomNumberInRange(facing - A_2,
                                facing + A_2);
                        float vel = MathUtils.getRandomNumberInRange(speed * -VEL_MIN,
                                speed * -VEL_MAX);
                        Vector2f vector = MathUtils.getPointOnCircumference(null,
                                vel,
                                angle);
                        engine.addHitParticle(beam.getTo(),
                                vector,
                                PARTICLE_SIZE,
                                PARTICLE_BRIGHTNESS,
                                PARTICLE_DURATION,
                                PARTICLE_COLOR);
                        radius = 10f + (weapon.getChargeLevel() * weapon.getChargeLevel() * MathUtils.getRandomNumberInRange(25f, 75f));
                        angle = MathUtils.getRandomNumberInRange(facing - A_2,
                                facing + A_2);
                        vel = MathUtils.getRandomNumberInRange(speed * -VEL_MIN,
                                speed * -VEL_MAX);
                        vector = MathUtils.getPointOnCircumference(null,
                                vel,
                                angle);
                        engine.addSmoothParticle(beam.getTo(), vector, radius, 0.1f + weapon.getChargeLevel() * 0.25f, 0.1f, color);
                        radius = 10f + (weapon.getChargeLevel() * weapon.getChargeLevel() * MathUtils.getRandomNumberInRange(25f, 75f));
                        angle = MathUtils.getRandomNumberInRange(facing - A_2,
                                facing + A_2);
                        vel = MathUtils.getRandomNumberInRange(speed * -VEL_MIN,
                                speed * -VEL_MAX);
                        vector = MathUtils.getPointOnCircumference(null,
                                vel,
                                angle);
                        engine.addHitParticle(beam.getTo(), vector, radius * .70f, 0.1f + weapon.getChargeLevel() * 0.1f, 0.1f, core);
                    }
                }

                CombatUtils.applyForce(weapon.getShip(), weapon.getShip().getFacing() - 180f, Math.min(target.getMass() / 2, BLADE_KNOCKBACK_MAX));
            }

            if (MagicRender.screenCheck(0.2f, point)) {
                if (runOnce2) {
                    float particleCount = beamWidth * PARTICLE_SPAWN_WIDTH_MULT * MathUtils.getDistance(beam.getTo(), beam.getFrom()) * amount * PARTICLE_DENSITY * weapon.getChargeLevel();

                    for (int i = 0; i < particleCount; i++) {
                        spawnPoint = MathUtils.getRandomPointInCircle(spawnPoint, beamWidth * PARTICLE_SPAWN_WIDTH_MULT);
                        Vector2f endPoint = MathUtils.getRandomPointOnLine(beam.getFrom(), beam.getTo());
                        if (!Global.getCombatEngine().getViewport().isNearViewport(spawnPoint, PARTICLE_SIZE_MAX * 3f)) {
                            continue;
                        }

                        Vector2f velocity = new Vector2f(ship.getVelocity().x * PARTICLE_INERTIA_MULT, ship.getVelocity().y * PARTICLE_INERTIA_MULT);
                        velocity = MathUtils.getRandomPointInCircle(velocity, PARTICLE_DRIFT);

                        if ((float) Math.random() <= 0.05f) {
                            engine.addNebulaParticle(spawnPoint,
                                    velocity,
                                    40f * (0.75f + (float) Math.random() * 0.5f),
                                    MathUtils.getRandomNumberInRange(1.0f, 3f),
                                    0f,
                                    0f,
                                    1f,
                                    new Color(beam.getFringeColor().getRed(), beam.getFringeColor().getGreen(), beam.getFringeColor().getBlue(), 100),
                                    true);
                        }

                        engine.addSmoothParticle(spawnPoint, velocity, MathUtils.getRandomNumberInRange(PARTICLE_SIZE_MIN, PARTICLE_SIZE_MAX), weapon.getChargeLevel(),
                                MathUtils.getRandomNumberInRange(PARTICLE_DURATION_MIN, PARTICLE_DURATION_MAX), beam.getFringeColor());
                    }
                }
            }
        }
    }}