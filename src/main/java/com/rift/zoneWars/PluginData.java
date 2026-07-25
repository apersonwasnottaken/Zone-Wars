package com.rift.zoneWars;

import com.rift.zoneWars.ZoneWars;
import com.rift.zoneWars.zone.Zones;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A helper class to obtain stored information for the plugin.
 *
 * @author A Person
 * @version 1.0
 */

public class PluginData {
    private JSONObject data;
    private final JSONObject defaultEntry = new JSONObject("{\"teams\":[],\"territories\":[]}");
    private final Path dataFilePath;
    private final ZoneWars plugin;

    public PluginData(ZoneWars plugin) {
        this.plugin = plugin;
        this.dataFilePath = Path.of(plugin.getDataFolder().getAbsolutePath() + "/data.json");
    }

    /*
     * Plugin data
     * {
     *   "teams": [
     *     {"name": "Team 1", "color": 0, "id": d083b954-a81a-4e3f-8a5a-0629f3c13028, "members": [{"username": "Test1", "uuid": "d083b954-a81a-4e3f-8a5a-0629f3c13028"}, {"username": "Test2", "uuid": "281469bf-2016-4296-9470-1a2aa310d899"}]}
     *   ]
     *   "territories": [
     *     {"world": "world", "chunk_region_x": 0, "chunk_region_z": 0, "team": -1, "capital": false} // Team of -1 means no one claimed it (and no one can claim it)
     *     {"world": "world", "chunk_region_x": 2, "chunk_region_z": 2, "team": 0, "capital": false} // The territory from (32, 32) to (63, 63)
     *   ]
     * }
     */

    public JSONObject readData() {
        try {
            if (Files.exists(dataFilePath)) {
                data = new JSONObject(Files.readString(dataFilePath));
            }
            else {
                if (dataFilePath.getParent() != null) {
                    Files.createDirectories(dataFilePath.getParent());
                }
                Files.createFile(dataFilePath);
                Files.writeString(dataFilePath, defaultEntry.toString());
                data = defaultEntry;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    public void updateData(JSONObject newData) {
        data = newData;
    }

    public JSONArray getTeamsConfig() {
        readData();
        return data.getJSONArray("teams");
    }

    public JSONArray getTerritoriesConfig() {
        readData();
        return data.getJSONArray("territories");
    }
}
