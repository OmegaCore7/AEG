package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;

public class AEG_HellHeaven extends BaseShipSystemScript {

    private static final float RADIUS = 1000f;
    private static final Color NEBULA_COLOR = new Color(0, 255, 0, 255); // Green color
    private IntervalUtil effectInterval = new IntervalUtil(0.1f, 0.1f); // Interval for effect updates
    private IntervalUtil smallLightningInterval = new IntervalUtil(1f, 2f); // Interval for small lightning effects
    private IntervalUtil largeLightningInterval = new IntervalUtil(4f, 4f); // Interval for large lightning bolt
    private boolean effectActive = false;
    private float chargeUpTime = 4f; // Charge-up time
    private int leftIndex = 0;
    private int rightIndex = 15;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        final ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        final CombatEngineAPI engine = Global.getCombatEngine();

        if (state == State.IN) {
            effectActive = false;
        }

        if (state == State.ACTIVE) {
            if (!effectActive) {
                effectActive = true;
                engine.addPlugin(new BaseEveryFrameCombatPlugin() {
                    private float timer = 0f;
                    private List<Vector2f> ringPoints = new ArrayList<>();

                    @Override
                    public void advance(float amount, List<InputEventAPI> events) {
                        if (!effectActive) {
                            engine.removePlugin(this);
                            return;
                        }

                        timer += amount;
                        effectInterval.advance(amount);
                        smallLightningInterval.advance(amount);
                        largeLightningInterval.advance(amount);

                        Vector2f location = ship.getLocation();
                        float radius;

                        if (timer <= chargeUpTime) {
                            // Charge-up phase: expanding ring
                            radius = RADIUS * (timer / chargeUpTime);
                        } else {
                            // Fully expanded phase
                            radius = RADIUS;
                        }

                        // Ensure ringPoints is populated correctly
                        if (ringPoints.isEmpty() || radius != RADIUS) {
                            ringPoints.clear();
                            int numPoints = 30; // Number of points along the circumference
                            for (int i = 0; i < numPoints; i++) {
                                float angle = (float) (i * 2 * Math.PI / numPoints);
                                ringPoints.add(new Vector2f(
                                        location.x + radius * (float) Math.cos(angle),
                                        location.y + radius * (float) Math.sin(angle)
                                ));
                            }
                        }

                        // Create green nebula ring
                        for (int i = 0; i < 5; i++) {
                            float angle = (float) (Math.random() * 2 * Math.PI);
                            Vector2f nebulaPoint = new Vector2f(
                                    location.x + radius * (float) Math.cos(angle),
                                    location.y + radius * (float) Math.sin(angle)
                            );
                            float nebulaSize = 50f + (float)(Math.random() * 100f);
                            engine.addNebulaParticle(nebulaPoint, new Vector2f(), nebulaSize, 1, 0.5f, 0.5f, 1f, NEBULA_COLOR);
                        }

                        // Create small lightning effects every 1 to 2 seconds
                        if (smallLightningInterval.intervalElapsed() && timer > chargeUpTime) {
                            Vector2f startPoint = ringPoints.get(leftIndex);
                            Vector2f endPoint = ringPoints.get(rightIndex);
                            if (ship != null && startPoint != null && endPoint != null) {
                                float width = 10f + (float)(Math.random() * 20f); // Random width
                                float transparency = 0.5f + (float)(Math.random() * 0.5f); // Random transparency
                                engine.spawnEmpArc(ship, startPoint, ship, ship, DamageType.ENERGY, 0, 0, 1100f, null, width, NEBULA_COLOR, new Color(200,255,200,255));
                            }
                            leftIndex = (leftIndex + 1) % 15;
                            rightIndex = 15 + ((rightIndex + 1) % 15);
                        }

                        // Create large lightning bolt every 4 seconds
                        if (largeLightningInterval.intervalElapsed() && timer > chargeUpTime) {
                            Vector2f startPoint = ringPoints.get(leftIndex);
                            Vector2f endPoint = ringPoints.get(rightIndex);
                            if (ship != null && startPoint != null && endPoint != null) {
                                float width = 40f + (float)(Math.random() * 20f); // Random width
                                float transparency = 0.5f + (float)(Math.random() * 0.5f); // Random transparency
                                engine.spawnEmpArc(ship, startPoint, ship, ship, DamageType.ENERGY, 0, 0, 1100f, null, width, NEBULA_COLOR, new Color(200,255,200,255));
                            }
                            leftIndex = (leftIndex + 1) % 15;
                            rightIndex = 15 + ((rightIndex + 1) % 15);
                        }
                    }
                });
            }
        } else {
            effectActive = false;
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (state == State.ACTIVE) {
            return new StatusData("HellHeaven system active", false);
        }
        return null;
    }
}