package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import java.util.List;
import java.util.Random;

public class AEG_4g_HHImpact extends BaseShipSystemScript {

    public static final float SPEED_THRESHOLD = 400f;
    private static final float IMPACT_INTERVAL = 2f;
    private static final float LIGHTNING_INTERVAL = 2f;
    private float lastImpactTime = 0f;
    private float lastLightningTime = 0f;
    private Random random = new Random();

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        final ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        if (state == State.ACTIVE) {
            Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {

                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);

                    if (ship.getVelocity().length() > SPEED_THRESHOLD && currentTime - lastImpactTime > IMPACT_INTERVAL) {
                        for (ShipAPI target : Global.getCombatEngine().getShips()) {
                            if (target.getOwner() != ship.getOwner() && target.isAlive() && MathUtils.getDistance(ship, target) < ship.getCollisionRadius() + target.getCollisionRadius()) {
                                Vector2f impactPoint = MathUtils.getMidpoint(ship.getLocation(), target.getLocation());

                                // Find the armor cells that were damaged
                                ArmorGridAPI armorGrid = target.getArmorGrid();
                                int[] cell = armorGrid.getCellAtLocation(impactPoint);

                                if (cell != null) {
                                    Vector2f cellCenter = armorGrid.getLocation(cell[0], cell[1]);

                                    // Generate green lightning sparks from (0, 70) relative to player facing to random points on the enemy ship
                                    if (currentTime - lastLightningTime > LIGHTNING_INTERVAL) {
                                        Vector2f startPoint = transformRelativeToShip(ship, new Vector2f(0, 70));
                                        for (int i = 0; i < 1; i++) {
                                            Vector2f targetPoint = new Vector2f(
                                                    target.getLocation().x + random.nextFloat() * target.getCollisionRadius() * 2 - target.getCollisionRadius(),
                                                    target.getLocation().y + random.nextFloat() * target.getCollisionRadius() * 2 - target.getCollisionRadius()
                                            );
                                            Global.getCombatEngine().spawnEmpArcVisual(startPoint, null, targetPoint, target, 10f, new Color(0, 255, 100, 255), new Color(200, 255, 200, 255));
                                        }
                                        lastLightningTime = currentTime;
                                    }

                                    // Generate persistent smoke and spark particles at the damaged armor cells
                                    for (int i = 0; i < 20; i++) {
                                        float angle = random.nextFloat() * 360f;
                                        float speed = 50f + random.nextFloat() * 50f;
                                        Vector2f velocity = (Vector2f) Misc.getUnitVectorAtDegreeAngle(angle).scale(speed);
                                        Vector2f sparkPoint = new Vector2f(
                                                cellCenter.x + 20f * (float) Math.cos(angle),
                                                cellCenter.y + 20f * (float) Math.sin(angle)
                                        );
                                        Global.getCombatEngine().addHitParticle(sparkPoint, velocity, 5f + random.nextFloat() * 10f, 1f, Float.MAX_VALUE, new Color(255, 200, 0, 255));
                                        Global.getCombatEngine().addSmokeParticle(sparkPoint, velocity, 15f + random.nextFloat() * 25f, 1f, Float.MAX_VALUE, Color.GRAY.darker());
                                    }
                                }

                                // Update the last impact time
                                lastImpactTime = currentTime;
                            }
                        }
                    }
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
