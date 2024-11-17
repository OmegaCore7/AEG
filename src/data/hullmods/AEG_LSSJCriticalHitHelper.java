package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.awt.Color;
import java.util.*;

public class AEG_LSSJCriticalHitHelper implements EveryFrameCombatPlugin {

    private CombatEngineAPI engine;
    private float critChance = 0.05f; // Default critical hit chance
    private float critDamageMultiplier = 2f; // Critical hit deals 2x damage

    private static final Map<String, String> SOUND_IDS_HE = new HashMap<>();
    private static final Map<String, String> SOUND_IDS_ENERGY = new HashMap<>();

    static {
        SOUND_IDS_HE.put("isFrigate", "explosion_ship");
        SOUND_IDS_HE.put("isDestroyer", "explosion_ship");
        SOUND_IDS_HE.put("isCruiser", "explosion_ship");
        SOUND_IDS_HE.put("isCapital", "explosion_ship");

        SOUND_IDS_ENERGY.put("isFrigate", "explosion_ship");
        SOUND_IDS_ENERGY.put("isDestroyer", "explosion_ship");
        SOUND_IDS_ENERGY.put("isCruiser", "explosion_ship");
        SOUND_IDS_ENERGY.put("isCapital", "explosion_ship");
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    @Override
    public void advance(float amount, List events) {
        if (engine.isPaused()) {
            return;
        }

        for (DamagingProjectileAPI proj : engine.getProjectiles()) {
            if (proj == null) continue;

            List<CombatEntityAPI> toCheck = new ArrayList<>();
            toCheck.addAll(CombatUtils.getShipsWithinRange(proj.getLocation(), proj.getCollisionRadius() + 5f));
            toCheck.addAll(CombatUtils.getMissilesWithinRange(proj.getLocation(), proj.getCollisionRadius() + 5f));
            toCheck.addAll(CombatUtils.getAsteroidsWithinRange(proj.getLocation(), proj.getCollisionRadius() + 5f));
            toCheck.remove(proj.getSource());

            for (CombatEntityAPI entity : toCheck) {
                if (entity instanceof ShipAPI) {
                    ShipAPI ship = (ShipAPI) entity;
                    if (ship.getPhaseCloak() != null && ship.getPhaseCloak().isActive()) {
                        continue;
                    }
                }

                if (entity.getShield() != null && entity.getShield().isOn() && entity.getShield().isWithinArc(proj.getLocation())) {
                    continue;
                }

                if (CollisionUtils.isPointWithinBounds(proj.getLocation(), entity)) {
                    if (entity instanceof ShipAPI && critSuccessful(proj)) {
                        applyCriticalHit((ShipAPI) entity, proj);
                    }
                }
            }
        }
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
        // No implementation needed for this method
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
        // No implementation needed for this method
    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {
        // No implementation needed for this method
    }

    private boolean critSuccessful(DamagingProjectileAPI proj) {
        WeaponAPI weapon = proj.getWeapon();
        if (weapon.getDamageType().equals(DamageType.FRAGMENTATION) ||
                weapon.getDamageType().equals(DamageType.KINETIC) ||
                weapon.getDamageType().equals(DamageType.OTHER)) {
            return false;
        }

        float weaponRoF = weapon.getDerivedStats().getRoF();
        if (weaponRoF < 1f) {
            weaponRoF = 1f;
        }

        float critChance = new Random().nextFloat();
        float maxCritChance = this.critChance / weaponRoF;

        return critChance <= maxCritChance;
    }

    private void applyCriticalHit(ShipAPI target, DamagingProjectileAPI proj) {
        float damage = proj.getDamageAmount() * critDamageMultiplier;
        engine.applyDamage(target, proj.getLocation(), damage, proj.getDamageType(), 0f, false, false, proj.getSource());
        shipHitEffect(target, proj);
    }

    private void shipHitEffect(ShipAPI ship, DamagingProjectileAPI proj) {
        WeaponAPI weapon = proj.getWeapon();
        float explosionSize = 0f;
        float weaponRoF = weapon.getDerivedStats().getRoF();

        if (weaponRoF < 1f) {
            weaponRoF = 1f;
        }

        if (weapon.getSize().equals(WeaponAPI.WeaponSize.SMALL)) {
            explosionSize = MathUtils.getRandomNumberInRange(20f, 30f) / weaponRoF;
        } else if (weapon.getSize().equals(WeaponAPI.WeaponSize.MEDIUM)) {
            explosionSize = MathUtils.getRandomNumberInRange(30f, 40f) / weaponRoF;
        } else if (weapon.getSize().equals(WeaponAPI.WeaponSize.LARGE)) {
            explosionSize = MathUtils.getRandomNumberInRange(40f, 50f) / weaponRoF;
        }

        engine.addFloatingText(proj.getLocation(), "CRITICAL HIT", explosionSize, Color.RED, proj.getDamageTarget(), 0f, 0f);

        if (weapon.getDamageType().equals(DamageType.HIGH_EXPLOSIVE)) {
            applyExplosionEffects(ship, proj, SOUND_IDS_HE, explosionSize);
        } else if (weapon.getDamageType().equals(DamageType.ENERGY)) {
            applyExplosionEffects(ship, proj, SOUND_IDS_ENERGY, explosionSize);
        }
    }

    private void applyExplosionEffects(ShipAPI ship, DamagingProjectileAPI proj, Map<String, String> soundMap, float explosionSize) {
        float damage = proj.getDamageAmount() * critDamageMultiplier;
        float emp = proj.getEmpAmount() * critDamageMultiplier;

        engine.applyDamage(ship, proj.getLocation(), damage, proj.getDamageType(), emp, true, true, proj.getSource());
        proj.getVelocity().scale(1.5f - 1.0f);

        engine.spawnExplosion(proj.getLocation(), ship.getVelocity(), Color.ORANGE, 1f, 1.5f);
        Global.getSoundPlayer().playSound(soundMap.get(getShipSize(ship)), 1f, 1f, proj.getLocation(), ship.getVelocity());

        if (!ship.isHulk()) {
            engine.addSmokeParticle(ship.getLocation(), ship.getVelocity(), explosionSize, MathUtils.getRandomNumberInRange(.5f, .75f), 10f, Color.DARK_GRAY);
        }
    }

    private String getShipSize(ShipAPI ship) {
        if (ship.isFrigate()) return "isFrigate";
        if (ship.isDestroyer()) return "isDestroyer";
        if (ship.isCruiser()) return "isCruiser";
        if (ship.isCapital()) return "isCapital";
        return "isFrigate";
    }

    public void setCritChance(float critChance) {
        this.critChance = critChance;
    }

    public void setCritDamageMultiplier(float critDamageMultiplier) {
        this.critDamageMultiplier = critDamageMultiplier;
    }
}