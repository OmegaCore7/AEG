package data.weapons.scripts;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import java.awt.Color;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

public class AEG_ChainLightningEffect implements BeamEffectPlugin
{
    // If true, don't hit the same ship twice in the same burst
    private static final boolean IGNORE_SAME_SHIP_IN_BURST = true;
    // If true, pick a random enemy in range to be the next link in the chain
    private static final boolean PICK_RANDOM_ENEMY_IN_RANGE = true;
    // How long must the beam be fired for before it will start a second chain?
    private static final float TIME_BETWEEN_BURSTS = .3f;
    // How far can the initial chain travel to hit an enemy?
    private static final float INITIAL_CHAIN_RANGE = 1500f;
    // How much of its previous max length will each chain retain?
    private static final float RANGE_RETENTION_PER_CHAIN = .75f;
    // How much damage will each chain do, compared to the previous?
    private static final float DAMAGE_RETENTION_PER_CHAIN = .85f;
    // How many chains should we limit the weapon to generating?
    private static final int MAXIMUM_CHAINS_PER_BURST = 10;
    // What color is the core of the arc?
    private static final Color CORE_COLOR = new Color(255, 255, 255, 255);
    // What color is the fringe of the arc?
    private static final Color FRINGE_COLOR = new Color(130, 204, 102, 255);
    private IntervalUtil fireInterval = new IntervalUtil(TIME_BETWEEN_BURSTS, TIME_BETWEEN_BURSTS);

    private static float getDistance(Vector2f point1, Vector2f point2)
    {
        return (float) Math.hypot(point1.x - point2.x, point1.y - point2.y);
    }

    private static List getEnemiesInRange(CombatEntityAPI ship, float range,
                                          int side, CombatEngineAPI engine)
    {
        List enemies = new ArrayList(), allShips = engine.getShips();
        ShipAPI tmp;

        // Iterate through all ships on the battlefield
        for (int x = 0; x < allShips.size(); x++)
        {
            tmp = (ShipAPI) allShips.get(x);

            // Filter through and find active enemies that are within range
            if (!tmp.isHulk() && !tmp.isShuttlePod() && ship.getOwner() != side
                    && getDistance(ship.getLocation(), tmp.getLocation()) <= range)
            {
                enemies.add(tmp);
            }
        }

        return enemies;
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam)
    {
        CombatEntityAPI target = beam.getDamageTarget();

        // Check that we hit something and that it wasn't a shield hit
        if (target != null && target instanceof ShipAPI
                && (target.getShield() == null || !target.getShield().isWithinArc(beam.getTo())))
        {
            if (beam.getBrightness() >= 1f)
            {
                fireInterval.advance(amount);
                if (fireInterval.intervalElapsed())
                {
                    // Count how many links are in the chain so far
                    int numStrikes = 0;

                    // Source of the current lightning chain
                    Vector2f source = beam.getFrom();
                    // Victim of the current lightning chain
                    CombatEntityAPI currentVictim = target;

                    float range = INITIAL_CHAIN_RANGE;
                    // Ensure we keep the same DPS as listed in the weapon's stats tooltip
                    float damage = beam.getWeapon().getDerivedStats().getDps()
                            * beam.getSource().getMutableStats().getBeamWeaponDamageMult().getModifiedValue()
                            * TIME_BETWEEN_BURSTS;
                    float emp = beam.getWeapon().getDerivedStats().getEmpPerSecond()
                            * TIME_BETWEEN_BURSTS;

                    // This is used to prevent hitting the same target twice
                    // if IGNORE_SAME_SHIP_IN_BURST is set to true
                    Set struck = new HashSet();

                    do
                    {
                        // Spawn this chain's lightning arc
                        engine.spawnEmpArc(beam.getSource(), source,
                                currentVictim, currentVictim,
                                DamageType.ENERGY, damage, emp, 100000f,
                                "tachyon_lance_emp_impact", 15f,
                                FRINGE_COLOR, CORE_COLOR);

                        // Check that we haven't hit our chain limit
                        if (++numStrikes >= MAXIMUM_CHAINS_PER_BURST)
                        {
                            return;
                        }

                        // Reduce the stats of the next chain
                        range *= RANGE_RETENTION_PER_CHAIN;
                        damage *= DAMAGE_RETENTION_PER_CHAIN;
                        emp *= DAMAGE_RETENTION_PER_CHAIN;

                        // Find our next victim
                        source = currentVictim.getLocation();
                        List enemies = getEnemiesInRange(currentVictim, range,
                                beam.getSource().getOwner(), engine);
                        enemies.remove(currentVictim);

                        // Remove enemies that have already been struck once
                        // (only if IGNORE_SAME_SHIP_IN_BURST is true)
                        if (IGNORE_SAME_SHIP_IN_BURST)
                        {
                            struck.add(currentVictim);
                            enemies.removeAll(struck);
                        }

                        // Remove enemies who would block or avoid a strike
                        ShipAPI tmp;
                        for (Iterator iter = enemies.iterator(); iter.hasNext();)
                        {
                            tmp = (ShipAPI) iter.next();
                            if ((tmp.getShield() != null && tmp.getShield().isOn()
                                    && tmp.getShield().isWithinArc(source))
                                    || (tmp.getPhaseCloak() != null
                                    && tmp.getPhaseCloak().isActive()))
                            {
                                iter.remove();
                            }
                        }

                        // Pick a random valid enemy in range
                        if (!enemies.isEmpty())
                        {
                            if (PICK_RANDOM_ENEMY_IN_RANGE)
                            {
                                currentVictim = (ShipAPI) enemies.get((int) (Math.random() * enemies.size()));
                            }
                            else
                            {
                                ShipAPI closest = null;
                                float distance, closestDistance = Float.MAX_VALUE;

                                // Find the closest enemy in range
                                for (int x = 0; x < enemies.size(); x++)
                                {
                                    tmp = (ShipAPI) enemies.get(x);

                                    distance = getDistance(tmp.getLocation(),
                                            currentVictim.getLocation());

                                    // This ship is closer than the previous best
                                    if (distance < closestDistance)
                                    {
                                        closest = tmp;
                                        closestDistance = distance;
                                    }
                                }

                                currentVictim = closest;
                            }
                            // No enemies in range, end the chain
                        }
                        else
                        {
                            currentVictim = null;
                        }
                    }
                    while (currentVictim != null);
                }
            }
        }
    }
}