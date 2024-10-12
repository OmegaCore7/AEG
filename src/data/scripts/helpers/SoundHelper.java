package data.scripts.helpers;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class SoundHelper {

    private Map<String, String> sounds = new HashMap<>();
    private Queue<String> soundQueue = new LinkedList<>();
    private boolean isLooping = false;

    public SoundHelper(String jsonFilePath) {
        loadSounds(jsonFilePath);
    }

    private void loadSounds(String jsonFilePath) {
        try {
            JSONObject json = Global.getSettings().loadJSON(jsonFilePath);
            JSONArray soundArray = json.getJSONArray("sounds");

            for (int i = 0; i < soundArray.length(); i++) {
                JSONObject soundObject = soundArray.getJSONObject(i);
                String id = soundObject.getString("id");
                String file = soundObject.getString("file");

                sounds.put(id, file);
            }
        } catch (IOException e) {
            Global.getLogger(SoundHelper.class).error("Failed to load sounds from JSON", e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void playSound(String id) {
        String file = sounds.get(id);
        if (file != null) {
            Global.getSoundPlayer().playSound(file, 1.0f, 1.0f, null, null);
        } else {
            Global.getLogger(SoundHelper.class).warn("Sound ID not found: " + id);
        }
    }

    public void playSoundInOrder(String... ids) {
        for (String id : ids) {
            soundQueue.add(id);
        }
        playNextInQueue();
    }

    private void playNextInQueue() {
        if (!soundQueue.isEmpty()) {
            String nextSoundId = soundQueue.poll();
            playSound(nextSoundId);
        }
    }

    public void loopSound(String id, boolean loop) {
        String file = sounds.get(id);
        if (file != null) {
            isLooping = loop;
            if (loop) {
                Global.getSoundPlayer().playSound(file, 1.0f, 1.0f, null, null);
            } else {
                Global.getSoundPlayer().pauseMusic();
            }
        } else {
            Global.getLogger(SoundHelper.class).warn("Sound ID not found: " + id);
        }
    }
}
