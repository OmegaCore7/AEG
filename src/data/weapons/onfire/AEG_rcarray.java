package data.weapons.onfire;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;


public class AEG_rcarray implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {
	private static org.apache.log4j.Logger log = Global.getLogger(AEG_rcarray.class);

public static Color LIGHTNING_CORE_COLOR = new Color(247, 195, 190, 200);
public static Color LIGHTNING_FRINGE_COLOR = new Color(255, 112, 99, 175);

	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {

		CombatEntityAPI target = projectile.getSource();
		Vector2f point = projectile.getLocation();

		Vector2f vector = VectorUtils.rotate(new Vector2f(10000f,0f), projectile.getFacing());
		vector = new Vector2f(vector.getX()+point.getX(),vector.getY()+point.getY());

		SpriteAPI sprite = Global.getSettings().getSprite("spatk", "smolwhitepixel");
		//MagicRender.battlespace(sprite,MathUtils.getMidpoint(point,vector),new Vector2f(0f,0f), new Vector2f(MathUtils.getDistance(point,vector),3),new Vector2f(0,0),projectile.getFacing(),0f,Color.white,true,0,1f,0.5f);

		MagicRender.battlespace(sprite,MathUtils.getMidpoint(point,vector),new Vector2f(0f,0f), new Vector2f(MathUtils.getDistance(point,vector),3),new Vector2f(0,0),projectile.getFacing(),0f,Color.white,0f,0f,0f,0f,0f,0f,1f,0.5f,CombatEngineLayers.BELOW_SHIPS_LAYER,250,250);

		if(!CombatUtils.getEntitiesWithinRange(weapon.getShip().getLocation(),10000).isEmpty()) {
			for (CombatEntityAPI p : CombatUtils.getEntitiesWithinRange(weapon.getShip().getLocation(), 10000)) {
				if (CollisionUtils.getCollides(point, vector, p.getLocation(), p.getCollisionRadius()) && p.getOwner()!=weapon.getShip().getOwner()) {
					if (p instanceof DamagingProjectileAPI) {
						engine.removeEntity(p);
					} else if(CollisionUtils.getCollisionPoint(point, vector, p)!=null){
						Vector2f entry = CollisionUtils.getCollisionPoint(point, vector, p);
						engine.applyDamage(p, entry, 3000f, DamageType.FRAGMENTATION, 0f, true, false, weapon);
						engine.spawnExplosion(entry,new Vector2f(0f,0f),Color.red,50f,1f);

						//if(p instanceof ShipAPI){
						//	((ShipAPI)p).getFluxTracker().setCurrFlux(((ShipAPI)p).getFluxTracker().getCurrFlux());
						//	randmalf(((ShipAPI)p));
						//}

						if(CollisionUtils.getCollisionPoint(vector, point, p)!=null){
							engine.applyDamage(p, CollisionUtils.getCollisionPoint(vector, point, p), 3000f, DamageType.FRAGMENTATION, 0f, true, false, weapon);
							engine.spawnExplosion(CollisionUtils.getCollisionPoint(vector, point, p),new Vector2f(0f,0f),Color.red,50f,1f);
						}
					}
				}
			}
		}
		engine.removeEntity(projectile);
	}

	public static void randmalf(ShipAPI ship) {
		java.util.List<Object> possible = new ArrayList<>();
		possible.addAll(ship.getAllWeapons());
		possible.addAll(ship.getEngineController().getShipEngines());
		for (Object p : possible) {
			if(Math.random()>0.9f) {
				ship.applyCriticalMalfunction(p,false);
			}
		}
	}


	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
	{

	}

}
  
