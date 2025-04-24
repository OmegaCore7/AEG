package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class AEG_RealityMarbleBlocker implements DamageDealtModifier {
    private final ShipAPI domainOwner;
    private final float domainRadius;

    public AEG_RealityMarbleBlocker(ShipAPI owner, float radius) {
        this.domainOwner = owner;
        this.domainRadius = radius;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI)) return null;

        // Only check if source is projectile or missile
        if (param instanceof DamagingProjectileAPI || param instanceof MissileAPI) {
            Vector2f targetLoc = target.getLocation();
            Vector2f projLoc;

            if (param instanceof DamagingProjectileAPI) {
                projLoc = ((DamagingProjectileAPI) param).getLocation();
            } else {
                projLoc = ((MissileAPI) param).getLocation();
            }

            boolean sourceInside = MathUtils.getDistance(projLoc, domainOwner.getLocation()) < domainRadius;
            boolean targetInside = MathUtils.getDistance(targetLoc, domainOwner.getLocation()) < domainRadius;

            if (sourceInside != targetInside) {
                damage.setDamage(0f);
                return "Reality Marble: Blocked cross-domain attack";
            }
        }

        return null;
    }
}
