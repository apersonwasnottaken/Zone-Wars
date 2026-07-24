package com.rift.zoneWars;

import com.rift.zoneWars.ZoneWars;
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
    private final ZoneWars plugin;
    private JSONObject data;
    private final JSONObject defaultEntry = new JSONObject("{\"teams\":[],\"Territories\":[]}");
    private final Path dataFilePath;
    private final ComponentLogger logger;

    public PluginData(ZoneWars plugin) {
        this.plugin = plugin;
        this.logger = plugin.getComponentLogger();
        this.dataFilePath = Path.of(plugin.getDataFolder().getAbsolutePath() + "/data.json");
    }

    /*
     * Plugin data
     * {
     *   "teams": [
     *     {"name": "Team 1", "color": 0, "members": [{"username": "Test1", "uuid": "d083b954-a81a-4e3f-8a5a-0629f3c13028"}, {"username": "Test2", "uuid": "281469bf-2016-4296-9470-1a2aa310d899"}]}
     *   ]
     *   "territories": [
     *     {"world": "world", "chunk_region_x": 0, "chunk_region_z": 0, "team": -1, "capital": false} // Team of -1 means no one claimed it (and no one can claim it)
     *     {"world": "world", "chunk_region_x": 2, "chunk_region_z": 2, "team": 0, "capital": false} // The territory from (32, 32) to (63, 63)
     *   ]
     * }
     */

    public void readData() {
        try {
            if (Files.exists(dataFilePath)) {
                data = new JSONObject(Files.readString(dataFilePath));
            }
            else {
                Files.createFile(dataFilePath);
                Files.writeString(dataFilePath, defaultEntry.toString());
                data = defaultEntry;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JSONArray getTeamsConfig() {
        readData();
        return data.getJSONArray("teams");
    }

    public JSONObject getTeam(int teamIdx) {
        if (teamIdx >= getTeamsConfig().length() || teamIdx < 0) {
            return new JSONObject();
        }
        return getTeamsConfig().getJSONObject(teamIdx);
    }

    public String getTeamName(int teamIdx) {
        return getTeam(teamIdx).getString("name");
    }

    public int getTeamColor(int teamIdx) {
        return getTeam(teamIdx).getInt("color");
    }

    public JSONArray getTeamMembers(int teamIdx) {
        return getTeam(teamIdx).getJSONArray("members");
    }

    /**
     * Finds and returns the index of the team a player's in.
     *
     * @param username The player's username
     * @return The index of the team the player is in. If a player is in multiple teams, it will return the index of the team that is last in the list (so highest index).
     */
    public int getTeamIndexFromPlayer(String username) {
        AtomicInteger foundTeam = new AtomicInteger(-1);
        for (int i = 0; i < getTeamsConfig().length(); i++) {
            JSONArray teamMembers = getTeamMembers(i);
            int finalI = i;
            teamMembers.forEach(obj -> {
                if (Objects.equals(((JSONObject) obj).getString("username"), username)) {
                    foundTeam.set(finalI);
                }
            });
        }
        return foundTeam.get();
    }

    /**
     * Finds and returns the index of the team a player's in.
     * Should be used over getTeamIndexFromPlayer(String username) when possible
     *
     * @param uuid The player's UUID
     * @return The index of the team the player is in. If a player is in multiple teams, it will return the index of the team that is last in the list (so highest index).
     */
    public int getTeamIndexFromPlayer(UUID uuid) {
        AtomicInteger foundTeam = new AtomicInteger(-1);
        for (int i = 0; i < getTeamsConfig().length(); i++) {
            JSONArray teamMembers = getTeamMembers(i);
            int finalI = i;
            teamMembers.forEach(obj -> {
                if (Objects.equals(UUID.fromString(((JSONObject) obj).getString("uuid")), uuid)) {
                    foundTeam.set(finalI);
                }
            });
        }
        return foundTeam.get();
    }

    public JSONArray getTerritories() {
        readData();
        return data.getJSONArray("territories");
    }

    public void updateTerritories(JSONArray newTerritories) {
        data.put("territories", newTerritories);
    }

    /**
     * Finds and returns the territories that a team has
     * @param teamIdx
     * @return A JSONArray containing a list of a team's territory. If they have none, returns an empty JSONArray
     */
    public JSONArray getTeamTerritories(int teamIdx) {
        JSONArray teamTerritories = new JSONArray();
        getTerritories().forEach(obj -> {
           if (((JSONObject) obj).getInt("team") == teamIdx) {
                teamTerritories.put(obj);
           }
        });
        return teamTerritories;
    }

    /**
     * Finds and returns the capital territory of a team.
     *
     * @param teamIdx Team index
     * @return A JSONObject with info of the territory the player is in. If multiple territories meet the criteria, it will return the last entry in the list. If the team does not have a capital territory, it returns an empty JSONObject.
     */
    public JSONObject getCapitalTerritory(int teamIdx) {
        final JSONObject[] foundTerritory = {new JSONObject()};
        getTeamTerritories(teamIdx).forEach(obj -> {
            if (((JSONObject) obj).getBoolean("capital")) {
                foundTerritory[0] = (JSONObject) obj;
            }
        });
        return foundTerritory[0];
    }

    /**
     * Finds and returns the territory in the specified location
     *
     * @param location a location class
     * @return A JSONObject with info of the territory the player is in. If it cannot find a territory, it will return a new JSONObject.
     */
    public JSONObject findTerritoryFromLocation(Location location) {
        for (int i = 0; i < getTerritories().length(); i++) {
            JSONObject territory = getTerritories().getJSONObject(i);
            if (!location.getWorld().getName().equals(territory.getString("world"))) continue;
            if (location.getX() >= territory.getInt("chunk_region_x") * 16 && location.getX() <= territory.getInt("chunk_region_x") * 16 + 31) {
                if (location.getZ() >= territory.getInt("chunk_region_z") * 16 && location.getZ() <= territory.getInt("chunk_region_z") * 16 + 31) {
                    return territory;
                }
            }
        }
        return new JSONObject();
    }

    public boolean territoryValidator(JSONObject territory) {
        return territory.has("world") && territory.has("chunk_region_x") && territory.has("chunk_region_z") && territory.has("team") && territory.has("capital");
    }

    public void updateTerritory(JSONObject newData) {
        if (!territoryValidator(newData)) {
            logger.warn(MiniMessage.miniMessage().deserialize("Invalid territory data received while trying to update! " + newData));
            return;
        }
        for (int i = 0; i < getTerritories().length(); i++) {
            JSONObject territory = getTerritories().getJSONObject(i);
            // Checks world and position only.
            if (territory.getString("world") == newData.getString("world")) {
                if (territory.getString("chunk_region_x") == newData.getString("chunk_region_x")) {
                    if (territory.getString("chunk_region_z") == newData.getString("chunk_region_z")) {
                        updateTerritories(getTerritories().put(i, newData));
                    }
                }
            }
        }
    }
}
