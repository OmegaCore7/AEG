package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;

public class AEG_ChestInfernoLimbController implements EveryFrameCombatPlugin {

    private CombatEngineAPI engine;
    private boolean initialized = false;

    private static final String MAIN_WEAPON_SLOT = "WS0002";
    private static final String ARM_L = "WS0005";
    private static final String ARM_R = "WS0006";
    private static final String SHOULDER_L = "WS0003";
    private static final String SHOULDER_R = "WS0004";

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    @Override
    public void advance(float amount, java.util.List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) return;

        for (ShipAPI ship : engine.getShips()) {
            if (ship == null || !ship.isAlive()) continue;

            WeaponAPI main = getWeaponBySlot(ship, MAIN_WEAPON_SLOT);
            if (main == null) continue;

            // Only run when the player has this weapon selected
            if (ship != engine.getPlayerShip()) continue;
            WeaponGroupAPI selectedGroup = ship.getSelectedGroupAPI();
            if (selectedGroup == null || !selectedGroup.getWeaponsCopy().contains(main)) continue;

            float charge = main.getChargeLevel();

            // Interpolated limb angles based on charge
            float armBase = 78f;
            float armCharge = 65f;
            float armFire = 82f;

            float shoulderBase = 0f;
            float shoulderCharge = 30f;
            float shoulderFire = 25f;

            // Custom mapping for smooth pose blending
            float t = charge;

            float armT = mapCharge(t, 0.2f, 0.7f);
            float fireT = mapCharge(t, 0.7f, 1f);

            float leftArmAngle = lerp(armBase, lerp(armCharge, armFire, fireT), armT);
            float rightArmAngle = leftArmAngle;

            float leftShoulderAngle = lerp(shoulderBase, lerp(shoulderCharge, shoulderFire, fireT), armT);
            float rightShoulderAngle = leftShoulderAngle;

            // Apply rotations
            rotateWeapon(ship, ARM_L, -leftArmAngle);
            rotateWeapon(ship, ARM_R, rightArmAngle);
            rotateWeapon(ship, SHOULDER_L, -leftShoulderAngle);
            rotateWeapon(ship, SHOULDER_R, rightShoulderAngle);
        }
    }

    private WeaponAPI getWeaponBySlot(ShipAPI ship, String slotId) {
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (slotId.equals(w.getSlot().getId())) {
                return w;
            }
        }
        return null;
    }

    private void rotateWeapon(ShipAPI ship, String slotId, float angle) {
        WeaponAPI weapon = getWeaponBySlot(ship, slotId);
        if (weapon != null) {
            weapon.setCurrAngle(angle);
        }
    }

    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    // Maps charge level to a 0–1 range within a subrange
    private float mapCharge(float charge, float min, float max) {
        if (charge < min) return 0f;
        if (charge > max) return 1f;
        return (charge - min) / (max - min);
    }

    @Override
    public void processInputPreCoreControls(float amount, java.util.List<InputEventAPI> events) {}

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {}

    @Override
    public void renderInUICoords(ViewportAPI viewport) {}
}
