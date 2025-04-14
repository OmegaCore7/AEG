package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class AEG_4g_HHImpact extends BaseShipSystemScript {

    public static final float SPEED_THRESHOLD = 230f;
    public static final float HIGH_SPEED_THRESHOLD = 600f; // New higher speed threshold
    private static final float IMPACT_INTERVAL = 3f; // Increased interval for better performance
    private static final int EFFECT_DURATION = 3; // Reduced duration for efficiency
    private static final int EXPLOSION_DELAY = 2; // Delay before explosion
    private static final float DAMAGE_DELAY = 2f; // Delay before damage
    private static final int EXPLOSION_DAMAGE = 4000; // Explosion damage
    private static final float EXPLOSION_RADIUS = 150f; // Radius of the explosion
    private float lastImpactTime = 0f;
    private Random random = new Random();
    private boolean collisionDetected = false;
    private boolean explosionTriggered = false; // Flag to prevent multiple explosions

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        final ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        if (state == State.ACTIVE) {
            Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {

                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    final float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);

                    if (ship.getVelocity().length() > SPEED_THRESHOLD && currentTime - lastImpactTime > IMPACT_INTERVAL) {
                        Vector2f fistPoint = transformRelativeToShip(ship, new Vector2f(70, 0));

                        for (final ShipAPI target : Global.getCombatEngine().getShips()) {
                            if (target.getOwner() != ship.getOwner() && target.isAlive() && isPointInsideBounds(fistPoint, target)) {
                                Vector2f impactPoint = MathUtils.getMidpoint(ship.getLocation(), target.getLocation());

                                // Generate splash spark particles at the crossing point
                                for (int i = 0; i < 10; i++) { // Reduced from 20 to 10
                                    float angle = random.nextFloat() * 360f;
                                    float speed = 50f + random.nextFloat() * 50f;
                                    Vector2f velocity = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(speed);
                                    Vector2f sparkPoint = new Vector2f(
                                            fistPoint.x + 20f * (float) Math.cos(angle),
                                            fistPoint.y + 20f * (float) Math.sin(angle)
                                    );
                                    Global.getCombatEngine().addHitParticle(sparkPoint, velocity, 5f + random.nextFloat() * 10f, 1f, EFFECT_DURATION, new Color(255, 150, 50, 255));
                                    Global.getCombatEngine().addHitParticle(sparkPoint, velocity, 2f + random.nextFloat() * 5f, 1f, EFFECT_DURATION, new Color(255, 200, 100, 255));
                                    Global.getCombatEngine().addSmokeParticle(sparkPoint, velocity, 15f + random.nextFloat() * 25f, 1f, EFFECT_DURATION, Color.GRAY.darker());
                                }

                                // Find the armor cells that were damaged
                                ArmorGridAPI armorGrid = target.getArmorGrid();
                                int[] cell = armorGrid.getCellAtLocation(impactPoint);

                                if (cell != null) {
                                    Vector2f cellCenter = armorGrid.getLocation(cell[0], cell[1]);
                                    boolean b = ship.getVelocity().length() < HIGH_SPEED_THRESHOLD;

                                    // Generate streaming black smoke from the damaged armor cells
                                    for (int i = 10; i < 15; i++) { // Reduced from 20 to 5
                                        float angle = random.nextFloat() * 360f;
                                        float speed = 50f + random.nextFloat() * 50f;
                                        Vector2f velocity = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(speed);
                                        Vector2f smokePoint = new Vector2f(
                                                cellCenter.x + 20f * (float) Math.cos(angle),
                                                cellCenter.y + 20f * (float) Math.sin(angle)
                                        );
                                        Global.getCombatEngine().addSmokeParticle(smokePoint, velocity, 2f + random.nextFloat() * 5f, 1f, EFFECT_DURATION, new Color(100, 100, 0, 200));
                                        Global.getCombatEngine().addSmokeParticle(smokePoint, velocity, 5f + random.nextFloat() * 8f, 1f, EFFECT_DURATION, new Color(50, 50, 0, 200));
                                        Global.getCombatEngine().addSmokeParticle(smokePoint, velocity, 8f + random.nextFloat() * 12f, 1f, EFFECT_DURATION, new Color(0, 0, 0, 200));
                                    }
                                }

                                // Check for higher speed threshold and trigger explosion
                                if (ship.getVelocity().length() > HIGH_SPEED_THRESHOLD && !explosionTriggered) {
                                    explosionTriggered = true; // Set the flag to prevent multiple explosions
                                    Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                                        private float explosionTime = currentTime + EXPLOSION_DELAY;
                                        private float damageTime = explosionTime + DAMAGE_DELAY;

                                        @Override
                                        public void advance(float amount, List<InputEventAPI> events) {
                                            float elapsedTime = Global.getCombatEngine().getTotalElapsedTime(false);

                                            if (elapsedTime >= explosionTime && elapsedTime < damageTime) {
                                                Vector2f targetCenter = target.getLocation(); // Center of the enemy ship
                                                Global.getCombatEngine().spawnExplosion(targetCenter, new Vector2f(0, 0), new Color(100, 255, 150,200), EXPLOSION_RADIUS, 1f);
                                            }

                                            if (elapsedTime >= damageTime) {
                                                Vector2f targetCenter = target.getLocation(); // Center of the enemy ship
                                                Global.getCombatEngine().applyDamage(target, targetCenter, EXPLOSION_DAMAGE, DamageType.HIGH_EXPLOSIVE, 0f, false, false, ship);

                                                // Remove the plugin after damage
                                                Global.getCombatEngine().removePlugin(this);
                                            }
                                        }
                                    });
                                }

                                // Update the last impact time
                                lastImpactTime = currentTime;
                            }
                        }
                    }
                }

                private boolean isPointInsideBounds(Vector2f point, ShipAPI target) {
                    float halfWidth = target.getCollisionRadius();
                    float halfHeight = target.getCollisionRadius();
                    Vector2f targetLocation = target.getLocation();

                    return point.x >= targetLocation.x - halfWidth &&
                            point.x <= targetLocation.x + halfWidth &&
                            point.y >= targetLocation.y - halfHeight &&
                            point.y <= targetLocation.y + halfHeight;
                }
            });
        }
    }

    private Vector2f transformRelativeToShip(ShipAPI ship, Vector2f relative) {
        float facing = ship.getFacing() * (float) Math.PI / 180f;
        float cos = (float) Math.cos(facing);
        float sin = (float) Math.sin(facing);
        return new Vector2f(
                ship.getLocation().x + relative.x * cos - relative.y * sin,
                ship.getLocation().y + relative.x * sin + relative.y * cos
        );
    }
}
