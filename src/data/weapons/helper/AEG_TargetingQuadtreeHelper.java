package data.weapons.helper;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class AEG_TargetingQuadtreeHelper {
    private static final int MAX_OBJECTS = 10;
    private static final int MAX_LEVELS = 5;

    private int level;
    private List<CombatEntityAPI> objects;
    private Vector2f bounds;
    private AEG_TargetingQuadtreeHelper[] nodes;

    public AEG_TargetingQuadtreeHelper(int level, Vector2f bounds) {
        this.level = level;
        this.objects = new ArrayList<CombatEntityAPI>();
        this.bounds = bounds;
        this.nodes = new AEG_TargetingQuadtreeHelper[4];
    }

    public void clear() {
        objects.clear();
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] != null) {
                nodes[i].clear();
                nodes[i] = null;
            }
        }
    }

    private void split() {
        float subWidth = bounds.x / 2;
        float subHeight = bounds.y / 2;
        float x = bounds.x;
        float y = bounds.y;

        nodes[0] = new AEG_TargetingQuadtreeHelper(level + 1, new Vector2f(x + subWidth, y));
        nodes[1] = new AEG_TargetingQuadtreeHelper(level + 1, new Vector2f(x, y));
        nodes[2] = new AEG_TargetingQuadtreeHelper(level + 1, new Vector2f(x, y + subHeight));
        nodes[3] = new AEG_TargetingQuadtreeHelper(level + 1, new Vector2f(x + subWidth, y + subHeight));
    }

    private int getIndex(CombatEntityAPI entity) {
        int index = -1;
        double verticalMidpoint = bounds.x / 2;
        double horizontalMidpoint = bounds.y / 2;

        boolean topQuadrant = (entity.getLocation().y < horizontalMidpoint);
        boolean bottomQuadrant = (entity.getLocation().y > horizontalMidpoint);

        if (entity.getLocation().x < verticalMidpoint) {
            if (topQuadrant) {
                index = 1;
            } else if (bottomQuadrant) {
                index = 2;
            }
        } else if (entity.getLocation().x > verticalMidpoint) {
            if (topQuadrant) {
                index = 0;
            } else if (bottomQuadrant) {
                index = 3;
            }
        }

        return index;
    }

    public void insert(CombatEntityAPI entity) {
        if (nodes[0] != null) {
            int index = getIndex(entity);

            if (index != -1) {
                nodes[index].insert(entity);
                return;
            }
        }

        objects.add(entity);

        if (objects.size() > MAX_OBJECTS && level < MAX_LEVELS) {
            if (nodes[0] == null) {
                split();
            }

            int i = 0;
            while (i < objects.size()) {
                int index = getIndex(objects.get(i));
                if (index != -1) {
                    nodes[index].insert(objects.remove(i));
                } else {
                    i++;
                }
            }
        }
    }

    public List<CombatEntityAPI> retrieve(List<CombatEntityAPI> returnObjects, CombatEntityAPI entity) {
        int index = getIndex(entity);
        if (index != -1 && nodes[0] != null) {
            nodes[index].retrieve(returnObjects, entity);
        }

        returnObjects.addAll(objects);

        return returnObjects;
    }
}
