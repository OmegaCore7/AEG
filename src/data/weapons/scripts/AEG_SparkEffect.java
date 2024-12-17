package data.weapons.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

public class AEG_SparkEffect implements EveryFrameWeaponEffectPlugin {
    private float previousAngle = 0f;
    private int currentFrame = 0;
    private float frameTime = 0f;
    private final Random random = new Random();
    private float lightningTime = 0f;
    private boolean strikeFirstBolt = true;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        float currentAngle = weapon.getCurrAngle();
        if (currentAngle != previousAngle) {
            // Weapon is moving
            frameTime += amount;
            // Duration of each frame in seconds
            float frameDuration = 0.1f;
            if (frameTime >= frameDuration) {
                frameTime = 0f;
                currentFrame++;
                if (currentFrame > 12) {
                    currentFrame = 1; // Skip frame 00
                }
            }
        } else {
            // Weapon is idle
            currentFrame = 0;
        }

        weapon.getAnimation().setFrame(currentFrame);
        previousAngle = currentAngle;

        // Generate lightning effects
        lightningTime += amount;
        // Interval between lightning strikes in seconds
        float lightningInterval = 2f;
        if (lightningTime >= lightningInterval) {
            lightningTime = 0f;
            if (strikeFirstBolt) {
                generateLightningBolt(engine, weapon, new Color(255, 255, 255), new Color(0, 0, 255)); // White core, blue fringe
            } else {
                generateLightningBolt(engine, weapon, new Color(255, 0, 255), new Color(128, 0, 128)); // Magenta core, purple fringe
            }
            strikeFirstBolt = !strikeFirstBolt;
        }
    }

    private void generateLightningBolt(CombatEngineAPI engine, WeaponAPI weapon, Color coreColor, Color fringeColor) {
        Vector2f weaponLocation = weapon.getLocation();
        Vector2f firingOffset = weapon.getSpec().getHardpointFireOffsets().get(0); // Assuming hardpoint offset 0
        Vector2f boltEnd = new Vector2f(weaponLocation.x + firingOffset.x, weaponLocation.y + firingOffset.y);

        // Calculate a random point within a 10f radius around the weapon's center
        float angle = random.nextFloat() * 360f;
        float distance = 10f * random.nextFloat();
        Vector2f targetPoint = new Vector2f((float) Math.cos(Math.toRadians(angle)) * distance, (float) Math.sin(Math.toRadians(angle)) * distance);
        targetPoint.translate(weaponLocation.x, weaponLocation.y);

        // Adjust the length of the lightning bolt to reach the target point
        Vector2f boltStart = new Vector2f();
        Vector2f.sub(targetPoint, boltEnd, boltStart);
        boltStart.normalise();
        boltStart.scale(300f); // Adjust this value as needed to ensure the bolt reaches the target
        boltStart.translate(boltEnd.x, boltEnd.y);

        // Create the EMP arc for the lightning bolt
        engine.spawnEmpArcVisual(boltStart, weapon.getShip(), targetPoint, null, 10f, coreColor, fringeColor);
    }
}