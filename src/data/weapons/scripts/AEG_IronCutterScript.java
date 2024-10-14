package data.weapons.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AEG_IronCutterScript implements EveryFrameWeaponEffectPlugin {
    private boolean hasFired = false;
    private float timer = 0f;
    private List<Vector2f> particlePositions = new ArrayList<>();
    private Random random = new Random();

    @Override
    public void advance(float amount, final CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        if (weapon.isFiring() && !hasFired) {
            hasFired = true;
            timer = 0f;

            // Get ship spec and bounds
            ShipAPI ship = weapon.getShip();
            ShipHullSpecAPI spec = ship.getHullSpec();
            SpriteAPI sprite = Global.getSettings().getSprite(spec.getSpriteName());
            float width = sprite.getWidth();
            float height = sprite.getHeight();

            // Initialize particle positions around the ship's bounds
            for (float x = -width / 2; x <= width / 2; x += 10f) {
                for (float y = -height / 2; y <= height / 2; y += 10f) {
                    particlePositions.add(new Vector2f(ship.getLocation().x + x, ship.getLocation().y + y));
                }
            }
        }

        if (hasFired) {
            timer += amount;

            // Create particle effect
            for (Vector2f pos : particlePositions) {
                Vector2f direction = VectorUtils.getDirectionalVector(pos, weapon.getLocation());
                direction.scale(timer * 0.1f); // Adjust speed as needed
                Vector2f.add(pos, direction, pos);

                // Spawn particle
                engine.addHitParticle(pos, new Vector2f(), 10f, 1f, 0.5f, Color.YELLOW);

                // Create red lightning effect
                if (random.nextFloat() < 0.1f) { // Adjust frequency as needed
                    Vector2f lightningEnd = new Vector2f(pos);
                    Vector2f.add(lightningEnd, direction, lightningEnd);
                    engine.spawnEmpArcVisual(weapon.getLocation(), null, pos, null, 10f, new Color(128, 0, 0, 128), new Color(255, 0, 0, 255));
                }
            }

            // Check if the effect is complete
            if (timer >= 7f) { // Adjust duration as needed
                // Spawn the Iron Cutter ship
                Vector2f spawnLocation = weapon.getLocation();
                CombatFleetManagerAPI fleetManager = engine.getFleetManager(weapon.getShip().getOwner());
                FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "AEG_IronCutter_Hull");
                fleetManager.spawnFleetMember(member, spawnLocation, weapon.getShip().getFacing(), 0f);
                final ShipAPI ironCutter = (ShipAPI) member.getStats().getEntity();

                // Activate the ship system
                ironCutter.useSystem();

                // Set a timer to explode the ship after 20 seconds
                Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                    private float explosionTimer = 20f;

                    @Override
                    public void advance(float amount, List events) {
                        if (engine.isPaused()) {
                            return;
                        }

                        explosionTimer -= amount;
                        if (explosionTimer <= 0f) {
                            engine.applyDamage(ironCutter, ironCutter.getLocation(), ironCutter.getHitpoints(), DamageType.HIGH_EXPLOSIVE, 0f, true, false, ironCutter);
                            engine.removeEntity(ironCutter);
                            engine.removePlugin(this);
                        }
                    }
                });

                // Clear particles
                particlePositions.clear();
                hasFired = false;
            }
        }
    }
}
