package data.weapons.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class AEG_ChestInfernoLimbController implements EveryFrameCombatPlugin {

    private CombatEngineAPI engine;

    private static final String MAIN_WEAPON_SLOT = "WS0002";  // AEG_ChestInferno
    private static final String ARM_L = "WS0006";
    private static final String ARM_R = "WS0005";
    private static final String SHOULDER_L = "WS0003";
    private static final String SHOULDER_R = "WS0004";

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) return;

        for (ShipAPI ship : engine.getShips()) {
            if (ship == null || !ship.isAlive()) continue;

            WeaponAPI inferno = getWeaponBySlot(ship, MAIN_WEAPON_SLOT);
            if (inferno == null) continue;

            // Only animate if the inferno weapon is selected
            WeaponGroupAPI selected = ship.getSelectedGroupAPI();
            if (selected == null || !selected.getWeaponsCopy().contains(inferno)) continue;

            float charge = inferno.getChargeLevel();
            float facing = ship.getFacing();

            // Interpolated limb angles based on charge
            float armLStart = 15f, armLCharge = 65f, armLFire = 82f;
            float armRStart = -15f, armRCharge = -65f, armRFire = -82f;

            float shoulderLStart = 0f, shoulderLCharge = 30f, shoulderLFire = 25f;
            float shoulderRStart = 0f, shoulderRCharge = -30f, shoulderRFire = -25f;

// Smooth phase blending
            float t = charge;
            float chargeT = mapCharge(t, 0.2f, 0.7f);
            float fireT = mapCharge(t, 0.7f, 1.0f);

// Final interpolated angles
            float finalArmL = lerp(armLStart, lerp(armLCharge, armLFire, fireT), chargeT);
            float finalArmR = lerp(armRStart, lerp(armRCharge, armRFire, fireT), chargeT);
            float finalShoulderL = lerp(shoulderLStart, lerp(shoulderLCharge, shoulderLFire, fireT), chargeT);
            float finalShoulderR = lerp(shoulderRStart, lerp(shoulderRCharge, shoulderRFire, fireT), chargeT);

// Apply rotations relative to ship
            rotateWeapon(ship, ARM_L, ship.getFacing() + finalArmL);
            rotateWeapon(ship, ARM_R, ship.getFacing() + finalArmR);
            rotateWeapon(ship, SHOULDER_L, ship.getFacing() + finalShoulderL);
            rotateWeapon(ship, SHOULDER_R, ship.getFacing() + finalShoulderR);
        }
    }

    private WeaponAPI getWeaponBySlot(ShipAPI ship, String slotId) {
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot() != null && slotId.equals(w.getSlot().getId())) {
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

    private float mapCharge(float charge, float min, float max) {
        if (charge < min) return 0f;
        if (charge > max) return 1f;
        return (charge - min) / (max - min);
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {}

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {}

    @Override
    public void renderInUICoords(ViewportAPI viewport) {}
}
