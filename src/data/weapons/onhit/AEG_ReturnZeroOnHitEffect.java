package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class AEG_ReturnZeroOnHitEffect implements OnHitEffectPlugin {
    private static final float DURATION = 20f;
    private static final float BLACK_HOLE_RADIUS = 3000f;
    private static final float PULL_STRENGTH = 300f;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        engine.addPlugin(new BlackHoleEffectPlugin(point));
    }

    public static class BlackHoleEffectPlugin implements EveryFrameCombatPlugin {

        private final Vector2f location;
        private float elapsed = 0f;
        private boolean explosionOccurred = false;

        // 🔄 New: List to track orbiting orbs
        private final List<OrbitalParticle> orbitals = new ArrayList<>();
        private boolean initialized = false;

        public BlackHoleEffectPlugin(Vector2f loc) {
            this.location = new Vector2f(loc);
        }

        @Override
        public void init(CombatEngineAPI engine) {
        }

        @Override
        public void renderInWorldCoords(ViewportAPI viewport) {
        }

        @Override
        public void renderInUICoords(ViewportAPI viewport) {
        }

        @Override
        public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
        }


        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;

            elapsed += amount;

            if (!initialized) {
                spawnOrbitingParticles();
                initialized = true;
            }
            if (elapsed < 12f) {
                applyRadiantBurn(engine);
                fireGodThunderboltBreaker(engine);
            }
            if (elapsed >= 12f) {
                applyBlackHolePull(engine);
            }

            // Core glow effect
            spawnNucleusGlow(engine);

            // Rotating elliptical rings
            createEllipticalGradientRing(engine, location, 200f, 251, elapsed, (float) (Math.PI / 5), 0f, 1.0f, false);
            createEllipticalGradientRing(engine, location, 180f, 251, elapsed, (float) (Math.PI / 6), (float) Math.PI / 4f, 0.8f, false);
            createEllipticalGradientRing(engine, location, 160f, 251, elapsed, (float) (Math.PI / 7), (float) Math.PI / 2f, 0.6f, true);
            createEllipticalGradientRing(engine, location, 220f, 251, elapsed, (float) (Math.PI / 8), (float) (3 * Math.PI / 4f), 0.5f, true);

            // 🔄 Update orbiting orbs
            for (OrbitalParticle orb : orbitals) {
                orb.advance(elapsed, engine, location);
            }

            if (elapsed >= DURATION && !explosionOccurred) {
                triggerExplosion(engine, location);
                explosionOccurred = true;
            }

            if (elapsed >= DURATION + 1f) {
                engine.removePlugin(this);
            }
        }

        private void spawnOrbitingParticles() {
            for (int i = 0; i < 6; i++) {
                float angleOffset = (float) (Math.random() * Math.PI * 2f);
                Color[] palette = {
                        new Color(255, 255, 200),
                        new Color(255, 60, 60),
                        new Color(255, 180, 50),
                        new Color(255, 100, 0)
                };
                Color color = palette[i % palette.length];
                orbitals.add(new OrbitalParticle(angleOffset, color));
            }
        }

        private void spawnNucleusGlow(CombatEngineAPI engine) {
            // Core white center
            engine.addNebulaParticle(location, new Vector2f(), 50f, 2f, 0f, 0.3f, 1f, new Color(255 - MathUtils.getRandom().nextInt(50), 150 - MathUtils.getRandom().nextInt(50), 50 - MathUtils.getRandom().nextInt(50), 255 - MathUtils.getRandom().nextInt(50)));
            // Orange glow halo
            engine.addNebulaParticle(location, new Vector2f(), 75f, 2f, 0f, 0.3f, 1f, new Color(150 - MathUtils.getRandom().nextInt(50), 100 - MathUtils.getRandom().nextInt(50), 0, 255 - MathUtils.getRandom().nextInt(50)));
            // Blue glow halo
            engine.addNebulaParticle(location, new Vector2f(), 100f, 2f, 0f, 0.3f, 1f, new Color(100 - MathUtils.getRandom().nextInt(50), 50 - MathUtils.getRandom().nextInt(50), 0, 200 - MathUtils.getRandom().nextInt(50)));
        }

        // 🔄 Orbital particle class for orbiting orbs
        public class OrbitalParticle {
            float baseAngle;
            Color baseColor;
            float speedMultiplier;

            public OrbitalParticle(float angle, Color color) {
                this.baseAngle = angle;
                this.baseColor = color;
                this.speedMultiplier = 1f + (float) Math.random() * 0.5f; // Between 1.0 and 1.5
            }

            void advance(float elapsedTime, CombatEngineAPI engine, Vector2f center) {
                float radius = 180f + 20f * (float) Math.sin(elapsedTime * 1.5f * speedMultiplier + baseAngle);
                float speed = 1f;
                float angle = baseAngle + elapsedTime * speed;
                float pulse = 0.5f + 0.5f * (float) Math.sin(elapsedTime * 3f + baseAngle);

                float x = center.x + radius * (float) Math.cos(angle);
                float y = center.y + radius * (float) Math.sin(angle);

                float size = 40f + 50f * pulse;
                float brightness = 0.8f + 0.7f * pulse;

                Color pulseColor = new Color(
                        Math.min(255, (int) (baseColor.getRed() * brightness)),
                        Math.min(255, (int) (baseColor.getGreen() * brightness)),
                        Math.min(255, (int) (baseColor.getBlue() * brightness)),
                        255
                );

                Vector2f pos = new Vector2f(x, y);

                engine.addHitParticle(pos, new Vector2f(), size, 1f, 0.1f, pulseColor);

                if (Math.random() < 0.3f) {
                    engine.addNebulaParticle(
                            pos,
                            MathUtils.getRandomPointInCircle(null, 20f),
                            size * 0.8f, // was 0.6f
                            1.5f,
                            0f,
                            0.1f,
                            0.4f,
                            new Color(255 - MathUtils.getRandom().nextInt(75), 100 - MathUtils.getRandom().nextInt(75), 80 - MathUtils.getRandom().nextInt(75), 150 - MathUtils.getRandom().nextInt(75))
                    );
                }
            }
        }

        private void applyBlackHolePull(CombatEngineAPI engine) {
            for (CombatEntityAPI entity : engine.getShips()) {
                if (entity instanceof ShipAPI && entity != engine.getPlayerShip()) {
                    float distance = MathUtils.getDistance(entity, location);
                    if (distance <= BLACK_HOLE_RADIUS) {
                        Vector2f pull = VectorUtils.getDirectionalVector(entity.getLocation(), location);
                        float strength = (1f - distance / BLACK_HOLE_RADIUS) * PULL_STRENGTH;
                        pull.scale(strength);
                        Vector2f.add(entity.getVelocity(), pull, entity.getVelocity());

                    }
                }
            }
        }

        private void createGradientRing(CombatEngineAPI engine, Vector2f center, float radius, int count) {
            for (int i = 0; i < count; i++) {
                float angle = (float) (i * 2 * Math.PI / count);
                float x = center.x + radius * (float) Math.cos(angle);
                float y = center.y + radius * (float) Math.sin(angle);
                Color color = getOrangeGradientColor(i, count);
                engine.addHitParticle(new Vector2f(x, y), new Vector2f(), 10f, 1f, DURATION, color);
            }
        }

        private Color getOrangeGradientColor(int index, int total) {
            float ratio = (float) index / total;
            int red = (int) (255 * (1 - ratio) + 255 * ratio);
            int green = (int) (140 * (1 - ratio) + 69 * ratio);
            return new Color(red, green, 0);
        }

        private void triggerExplosion(CombatEngineAPI engine, Vector2f center) {
            // Mazinger Zero photon colors
            Color coreWhite = new Color(255, 255, 200);
            Color photonRed = new Color(255, 60, 60);
            Color photonGold = new Color(255, 180, 50);
            Color photonMagenta = new Color(255, 100, 0);

            // Multiple smaller explosions around center to build up the effect
            for (int i = 0; i < 12; i++) {
                Vector2f offset = MathUtils.getRandomPointInCircle(center, 600f);
                Vector2f velocity = MathUtils.getRandomPointInCircle(null, 100f);
                float size = 200f + (float) Math.random() * 150f;
                Color color = (i % 2 == 0) ? photonGold : photonRed;
                engine.spawnExplosion(offset, velocity, color, size, 0.6f);
                engine.addHitParticle(offset, velocity, size * 0.6f, 1f, 0.5f, photonRed);
            }

            // Pulsing nebula shockwaves for depth and energy flow
            for (int i = 0; i < 3; i++) {
                float delay = i * 0.2f;
                float size = 1000f + i * 1000f;
                float duration = 0.7f + i * 0.1f;
                engine.addNebulaParticle(
                        center,
                        new Vector2f(),
                        size,
                        1.5f,
                        delay,   // delay before particle appears
                        0.3f,    // ramp-up duration
                        duration,
                        photonMagenta
                );
            }

            // The final big bright core explosion
            engine.spawnExplosion(center, new Vector2f(), coreWhite, 2500f, 1.2f);
            engine.addHitParticle(center, new Vector2f(), 3000f, 1f, 1f, photonGold);

            // Play explosion sound
            Global.getSoundPlayer().playSound("terrain_hyperspace_lightning", 1f, 1f, center, new Vector2f());

            // Deal heavy damage to all ships except player inside the explosion radius
            ShipAPI player = engine.getPlayerShip();
            for (ShipAPI ship : engine.getShips()) {
                if (ship != null && ship != player && MathUtils.getDistance(ship, center) < 3000f) {
                    engine.applyDamage(ship, center, 99999f, DamageType.HIGH_EXPLOSIVE, 9999f, true, false, player);
                }
            }
        }

        private void createEllipticalGradientRing(CombatEngineAPI engine, Vector2f center, float baseRadius, int count,
                                                  float elapsedTime, float rotationSpeed, float angleOffset, float brightness,
                                                  boolean verticalEllipse) {
            float ellipseFactor = 0.6f + 0.4f * (float) Math.sin(elapsedTime * 2); // oscillates between 0.2 and 1.0
            float angle = elapsedTime * rotationSpeed + angleOffset;

            for (int i = 0; i < count; i++) {
                float theta = (float) (i * 2 * Math.PI / count);
                float sin = (float) Math.sin(theta);
                float cos = (float) Math.cos(theta);

                // Apply ellipse transformation
                float x = baseRadius * cos;
                float y = baseRadius * sin;

                if (verticalEllipse) {
                    y *= ellipseFactor;
                } else {
                    x *= ellipseFactor;
                }

                // Rotate the entire shape
                float rotX = x * (float) Math.cos(angle) - y * (float) Math.sin(angle);
                float rotY = x * (float) Math.sin(angle) + y * (float) Math.cos(angle);

                Vector2f pos = new Vector2f(center.x + rotX, center.y + rotY);
                Color color = getOrangeGradientColor(i, count);
                color = new Color(
                        Math.min(255, (int) (color.getRed() * brightness)),
                        Math.min(255, (int) (color.getGreen() * brightness)),
                        color.getBlue()
                );

                engine.addHitParticle(pos, new Vector2f(), 10f, 1f, 0.1f, color);
            }
        }

        //THUNDERBOLT BREAKER METHOD
        private final float boltInterval = 1f;
        private float boltTimer = 0f;

        private void fireGodThunderboltBreaker(CombatEngineAPI engine) {
            boltTimer += engine.getElapsedInLastFrame();
            if (boltTimer < boltInterval) return;
            boltTimer = 0f;

            ShipAPI player = engine.getPlayerShip();
            List<ShipAPI> enemies = new ArrayList<>();
            for (ShipAPI ship : engine.getShips()) {
                if (ship != null && ship.isAlive() && ship.getOwner() != player.getOwner()) {
                    enemies.add(ship);
                }
            }

            if (enemies.isEmpty()) return;

            enemies.sort((a, b) -> Float.compare(b.getCollisionRadius(), a.getCollisionRadius()));
            ShipAPI initialTarget = enemies.get(0);
            if (initialTarget == null || !initialTarget.isAlive()) return;

            // CONFIGURATION
            float baseDamage = 1000f;
            float baseEmp = 1200f;
            float baseRange = 3000f;
            int maxChains = 5;
            float rangeReduction = 0.65f;
            float damageReduction = 0.5f;

            Color startColor = new Color(255, 255, 180); // Bright yellow-white
            Color endColor = new Color(255, 0, 0);       // Deep red

            // BOUNCE LOGIC
            List<ShipAPI> bounceTargets = new ArrayList<>();
            bounceTargets.add(initialTarget);

            for (int i = 1; i < maxChains; i++) {
                ShipAPI lastTarget = bounceTargets.get(bounceTargets.size() - 1);
                ShipAPI next = null;
                float closest = Float.MAX_VALUE;

                for (ShipAPI potential : enemies) {
                    if (!potential.isAlive() || potential.getPhaseCloak() != null && potential.getPhaseCloak().isActive() || bounceTargets.contains(potential))
                        continue;

                    float dist = MathUtils.getDistance(lastTarget, potential);
                    if (dist <= baseRange && dist < closest) {
                        next = potential;
                        closest = dist;
                    }
                }

                if (next == null) {
                    next = bounceTargets.get(i % bounceTargets.size());
                }

                bounceTargets.add(next);
            }

            // ACCUMULATE DAMAGE
            Map<ShipAPI, Float> damageMap = new HashMap<>();
            Map<ShipAPI, Float> empMap = new HashMap<>();

            Vector2f sourcePoint = location;
            for (int i = 0; i < bounceTargets.size(); i++) {
                ShipAPI target = bounceTargets.get(i);
                if (target == null || !target.isAlive()) continue;

                float t = i / (float) (maxChains - 1); // Ratio for interpolation
                Color core = interpolateColor(startColor, endColor, t);
                Color fringe = interpolateColor(startColor, endColor, t);

                float thisDamage = baseDamage * (float) Math.pow(damageReduction, i);
                float thisEmp = baseEmp * (float) Math.pow(damageReduction, i);
                float arcSize = 60f * (1f - t * 0.7f);  // Tighten width with bounce
                float explosionSize = 100f * (1f - t * 0.6f); // Smaller flash size

                // Add to maps
                damageMap.merge(target, thisDamage, Float::sum);
                empMap.merge(target, thisEmp, Float::sum);

                // SPAWN ARC (VISUAL ONLY — no shield hit)
                for (int j = 0; j < 20; j++) {
                    Vector2f randomizedSource = MathUtils.getRandomPointInCircle(sourcePoint, 30f);
                    engine.spawnEmpArcPierceShields(
                            null,
                            randomizedSource,
                            target,
                            target,
                            DamageType.ENERGY,
                            0f, 0f,
                            100000f,
                            "tachyon_lance_emp_impact",  // Or a custom sound
                            40f + MathUtils.getRandom().nextInt(60),
                            fringe,
                            core
                    );
                }
                addCrackleEffects(engine, target.getLocation(), target, core, fringe);

                Vector2f mid = MathUtils.getMidpoint(sourcePoint, target.getLocation());
                engine.spawnExplosion(mid, new Vector2f(), fringe, explosionSize, 0.3f);
                engine.addNebulaParticle(mid, new Vector2f(), explosionSize * 0.8f, 1.5f, 0.2f, 0.4f, 0.8f, core);

                Global.getSoundPlayer().playSound("realitydisruptor_emp_impact", 1f, 1.1f, target.getLocation(), new Vector2f());
                sourcePoint = target.getLocation(); // Move source point to this target
            }

            // APPLY TOTAL DAMAGE
            for (Map.Entry<ShipAPI, Float> entry : damageMap.entrySet()) {
                ShipAPI target = entry.getKey();
                float dmg = entry.getValue();
                float emp = empMap.getOrDefault(target, 0f);

                engine.applyDamage(
                        target,
                        target.getLocation(),
                        dmg,
                        DamageType.ENERGY,
                        emp,
                        true,
                        true,
                        player
                );
            }

            engine.spawnExplosion(sourcePoint, new Vector2f(), new Color(255, 180, 100), 300f, 0.6f);
            Global.getSoundPlayer().playSound("terrain_hyperspace_lightning", 1f, 1.2f, sourcePoint, new Vector2f());
        }

        // Helper to interpolate colors smoothly
        private Color interpolateColor(Color start, Color end, float t) {
            int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * t);
            int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * t);
            int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * t);
            return new Color(Math.min(255, r), Math.min(255, g), Math.min(255, b));
        }
        private void addCrackleEffects(CombatEngineAPI engine, Vector2f position, ShipAPI target, Color core, Color fringe) {
            // Electric sparks
            for (int i = 0; i < 4; i++) {
                Vector2f dir = MathUtils.getRandomPointInCircle(null, 80f);
                Vector2f sparkPos = Vector2f.add(new Vector2f(position), dir, null);
                engine.addHitParticle(sparkPos, dir, (float) (20f + Math.random() * 30f), 1f, 0.2f, core);
            }

            // Mini EMP arcs around the target
            for (int i = 0; i < 2; i++) {
                Vector2f arcPoint = MathUtils.getRandomPointInCircle(position, 100f);
                engine.spawnEmpArcPierceShields(
                        null,
                        position,
                        target,
                        target,
                        DamageType.ENERGY,
                        25f, 50f,
                        10000f,
                        "tachyon_lance_emp_impact",
                        25f - MathUtils.getRandom().nextInt(15),
                        fringe,
                        core
                );
            }

            // Nebula pulse
            engine.addNebulaParticle(
                    position,
                    MathUtils.getRandomPointInCircle(null, 20f),
                    40f + (float)Math.random() * 60f,
                    1.5f,
                    0f,
                    0.1f,
                    0.4f,
                    fringe
            );

            // Jitter on target (brief and strong)
            if (target != null) {
                target.setJitter("lightning_crackle_id", core, 1f, 3, 0f, 10f);
            }
        }

        private void applyRadiantBurn(CombatEngineAPI engine) {
            for (ShipAPI ship : engine.getShips()) {
                if (ship == null || !ship.isAlive() || ship.getOwner() == engine.getPlayerShip().getOwner()) continue;

                float distance = MathUtils.getDistance(ship.getLocation(), location);
                if (distance > BLACK_HOLE_RADIUS) continue;

                float distanceFactor = 1f - (distance / BLACK_HOLE_RADIUS);
                float intensity = (float) Math.pow(distanceFactor, 2f); // 0 to 1^2
                    // Calculate armor percent remaining
                ArmorGridAPI grid = ship.getArmorGrid();
                float maxArmor = grid.getMaxArmorInCell() * grid.getGrid().length * grid.getGrid()[0].length;
                float currentArmor = 0f;

                for (int x = 0; x < grid.getGrid().length; x++) {
                    for (int y = 0; y < grid.getGrid()[0].length; y++) {
                        currentArmor += grid.getArmorValue(x, y);
                    }
                }

                float armorPercent = currentArmor / maxArmor;

                // Apply DOT only if armor is below 50%
                if (armorPercent < 0.5f) {
                    float armorFactor = 1f - armorPercent; // 0.5 to 1.0 range
                    float scaledDamage = 50f * armorFactor * distanceFactor * engine.getElapsedInLastFrame();
                    engine.applyDamage(ship, ship.getLocation(), scaledDamage, DamageType.ENERGY, 0f, false, false, null);
                }
                // Vector from ship to sun
                Vector2f toSun = VectorUtils.getDirectionalVector(ship.getLocation(), location);

                int gridWidth = grid.getGrid().length;
                int gridHeight = grid.getGrid()[0].length;

                Vector2f shipLoc = ship.getLocation();
                Vector2f sunVec = Vector2f.sub(location, shipLoc, null);
                sunVec.normalise();

                for (int x = 0; x < gridWidth; x++) {
                    for (int y = 0; y < gridHeight; y++) {
                        float armor = grid.getArmorValue(x, y);
                        if (armor <= 0f) continue;

                        Vector2f cellWorldLoc = grid.getLocation(x, y);
                        Vector2f toCell = Vector2f.sub(cellWorldLoc, location, null);
                        float cellDistance = toCell.length();

                        if (cellDistance > BLACK_HOLE_RADIUS) continue;

                        // Angle between sun and armor cell
                        Vector2f armorVec = VectorUtils.getDirectionalVector(cellWorldLoc, location);
                        float angleToSun = VectorUtils.getAngle(new Vector2f(0f, 0f), armorVec);
                        float facingToSun = Math.abs(MathUtils.getShortestRotation(ship.getFacing(), angleToSun));

                        // Ignore armor on the front side (not exposed)
                        if (facingToSun < 90f) continue;

                        // Damage scales with distance and angle
                        float cellFactor = intensity * (facingToSun / 180f);
                        float damagePerSecond = 200f; // base rate per cell
                        float damage = damagePerSecond * cellFactor * engine.getElapsedInLastFrame();

                        grid.setArmorValue(x, y, Math.max(0f, armor - damage));

                        // Burn mark particle on outer edge cells
                        if (cellFactor > 0.1f && isEdgeCell(grid, x, y)) {
                            // Only draw flame if the burn is noticeable
                            engine.addNebulaParticle(
                                    cellWorldLoc,
                                    MathUtils.getRandomPointInCircle(null, 10f),
                                    15f + 25f * cellFactor,
                                    1.5f,
                                    0f,
                                    0.1f,
                                    0.3f,
                                    new Color(255, 100 + (int)(Math.random() * 100), 0, (int)(200 * cellFactor))
                            );
                        }
                    }
                }

                // Jitter & visual FX
                float jitterIntensity = 2f + 3f * distanceFactor; // up to 5 jitter intensity
                ship.setJitter("sun_burn_effect", new Color(255, 140, 0, 75), distanceFactor, 5, 0f, jitterIntensity);
                // Sound FX
                Global.getSoundPlayer().playSound(
                        "explosion_flak",  // or "flamer" or custom
                        1f,
                        0.8f + 0.4f * distanceFactor,
                        ship.getLocation(),
                        ship.getVelocity()
                );
                if (distanceFactor > 0.8f) {
                    engine.addFloatingText(ship.getLocation(), "SYSTEM MELTDOWN!", 32f, Color.RED, ship, 0.5f, 1f);
                } else if (distanceFactor > 0.5f) {
                    engine.addFloatingText(ship.getLocation(), "Critical Overheat", 24f, Color.ORANGE, ship, 0.5f, 1f);
                } else if (distanceFactor > 0.2f) {
                    engine.addFloatingText(ship.getLocation(), "Hull Integrity Compromised", 20f, Color.YELLOW, ship, 0.5f, 1f);
                }
            }

        }
        private boolean isEdgeCell(ArmorGridAPI grid, int x, int y) {
            int w = grid.getGrid().length;
            int h = grid.getGrid()[0].length;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;

                    int nx = x + dx;
                    int ny = y + dy;

                    if (nx < 0 || nx >= w || ny < 0 || ny >= h) return true;

                    if (grid.getArmorValue(nx, ny) <= 0f) return true;
                }
            }

            return false;
        }
    }

}