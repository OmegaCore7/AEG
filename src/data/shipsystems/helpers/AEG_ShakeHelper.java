package data.shipsystems.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import org.lwjgl.util.vector.Vector2f;
//THIS SCRIPT IS UNUSED!!!!!
public class AEG_ShakeHelper {

    private static final float DEFAULT_INTENSITY = 10f;
    private static final float DEFAULT_DURATION = 0.5f;
    private static final float DEFAULT_FREQUENCY = 0.5f;

    private static float shakeIntensity = 0f;
    private static float shakeDuration = 0f;
    private static float shakeTimer = 0f;
    private static Vector2f originalCameraPosition = new Vector2f(0f, 0f);
    private static float decayTime = 1f;  // Define decayTime to gradually reduce shake intensity

    // Method to apply the screen shake
    public static void applyScreenShake(float intensity, float duration, float decayTime, float frequency) {
        shakeIntensity = intensity;
        shakeDuration = duration;
        shakeTimer = 0f;
        AEG_ShakeHelper.decayTime = decayTime;  // Update decayTime

        CombatEngineAPI engine = Global.getCombatEngine();
        ViewportAPI viewport = engine.getViewport();

        // Store the original camera position so we can "shake" it
        originalCameraPosition.set(viewport.getCenter().getX(), viewport.getCenter().getY());
    }

    // Method to advance the shake every frame
    public static void advance(float amount) {
        if (shakeTimer < shakeDuration) {
            shakeTimer += amount;

            // Calculate a random offset within the shake intensity bounds
            float offsetX = (float) (Math.random() * shakeIntensity * 2 - shakeIntensity);
            float offsetY = (float) (Math.random() * shakeIntensity * 2 - shakeIntensity);

            // Apply the shake by setting the camera to the offset position
            CombatEngineAPI engine = Global.getCombatEngine();
            ViewportAPI viewport = engine.getViewport();

            // Use Vector2f for the camera center
            Vector2f shakePosition = new Vector2f(originalCameraPosition.x + offsetX, originalCameraPosition.y + offsetY);
            viewport.setCenter(shakePosition);

            // Gradually reduce shake intensity over time (optional)
            shakeIntensity = Math.max(0, shakeIntensity - (amount / decayTime));  // Reduce intensity based on decayTime
        } else {
            // Reset the camera to its original position after the shake duration ends
            CombatEngineAPI engine = Global.getCombatEngine();
            ViewportAPI viewport = engine.getViewport();
            viewport.setCenter(originalCameraPosition);

            // Reset the shake parameters to stop shaking
            shakeIntensity = 0f;
            shakeDuration = 0f;
            shakeTimer = 0f;
        }
    }
}
