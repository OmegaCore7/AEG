package data.weapons.scripts;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

public class AEG_RustHurricane implements EveryFrameWeaponEffectPlugin {

    public static Color STANDARD_RIFT_COLOR = new Color(100,60,255,255);
    public static Color EXPLOSION_UNDERCOLOR = new Color(100, 0, 25, 100);
    public static Color NEGATIVE_SOURCE_COLOR = new Color(200,255,200,25);

    public static String RIFTCASCADE_MINELAYER = "riftcascade_minelayer";

    public static int MAX_RIFTS = 5;
    public static float UNUSED_RANGE_PER_SPAWN = 200;
    public static float SPAWN_SPACING = 175;
    public static float SPAWN_INTERVAL = 0.1f;

    protected Vector2f prevMineLoc = null;
    protected boolean doneSpawningMines = false;
    protected float spawned = 0;
    protected int numToSpawn = 0;
    protected float untilNextSpawn = 0;

    protected IntervalUtil tracker = new IntervalUtil(0.1f, 0.2f);

    public AEG_RustHurricane() {
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!(weapon.getShip() instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) weapon.getShip();

        WeaponAPI linkedWeapon = null;
        List<WeaponAPI> weapons = ship.getAllWeapons();
        for (WeaponAPI w : weapons) {
            if ("WS0007".equals(w.getSlot().getId())) {
                linkedWeapon = w;
                break;
            }
        }
        if (linkedWeapon == null) return;

        for (MissileAPI missile : engine.getMissiles()) {
            if (missile.getWeapon() != weapon) continue;

            tracker.advance(amount);
            if (tracker.intervalElapsed()) {
                spawnNegativeParticles(engine, missile);
            }

            if (doneSpawningMines) return;

            if (numToSpawn <= 0) {
                float range = missile.getMaxFlightTime() * missile.getMaxSpeed();
                numToSpawn = (int) (range / UNUSED_RANGE_PER_SPAWN) + 1;
                if (numToSpawn > MAX_RIFTS) {
                    numToSpawn = MAX_RIFTS;
                }
                untilNextSpawn = 0f;
            }

            untilNextSpawn -= amount;
            if (untilNextSpawn > 0) return;

            Vector2f loc = missile.getLocation();
            spawnMine(ship, loc);
            spawned++;
            if (spawned >= numToSpawn) {
                doneSpawningMines = true;
            }
            untilNextSpawn = SPAWN_INTERVAL;

            // Update missile direction based on the linked weapon's facing
            float linkedWeaponFacing = linkedWeapon.getCurrAngle();
            missile.setFacing(linkedWeaponFacing);
        }
    }

    public void spawnNegativeParticles(CombatEngineAPI engine, MissileAPI missile) {
        // Similar to the beam version, but using missile's location
    }

    public void spawnMine(ShipAPI source, Vector2f mineLoc) {
        CombatEngineAPI engine = Global.getCombatEngine();

        MissileAPI mine = (MissileAPI) engine.spawnProjectile(source, null,
                RIFTCASCADE_MINELAYER,
                mineLoc,
                (float) Math.random() * 360f, null);

        float sizeMult = getSizeMult();
        mine.setCustomData(RiftCascadeMineExplosion.SIZE_MULT_KEY, sizeMult);

        if (source != null) {
            Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
                    source, WeaponAPI.WeaponType.ENERGY, false, mine.getDamage());
        }

        mine.getDamage().getModifier().modifyMult("mine_sizeMult", sizeMult);

        float fadeInTime = 0.05f;
        mine.getVelocity().scale(0);
        mine.fadeOutThenIn(fadeInTime);

        float liveTime = 0f;
        mine.setFlightTime(mine.getMaxFlightTime() - liveTime);
        mine.addDamagedAlready(source);
        mine.setNoMineFFConcerns(true);

        prevMineLoc = mineLoc;
    }

    public float getSizeMult() {
        float sizeMult = 1f - spawned / (float) Math.max(1, numToSpawn - 1);
        sizeMult = 0.75f + (1f - sizeMult) * 0.5f;
        return sizeMult;
    }
}
