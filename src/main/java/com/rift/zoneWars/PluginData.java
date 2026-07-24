package com.rift.zoneWars;

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
    private final JSONObject defaultEntry = new JSONObject("{\"teams\":[],\"zones\":[]}");
    private final Path dataFilePath;

    public PluginData(ZoneWars plugin) {
        this.plugin = plugin;
        this.dataFilePath = Path.of(plugin.getDataFolder().getAbsolutePath() + "/data.json");
    }

    /*
     * Plugin data
     * {
     *   "teams": [
     *     {"name": "Team 1", "color": 0, "members": [{"username": "Test1", "uuid": "d083b954-a81a-4e3f-8a5a-0629f3c13028"}, {"username": "Test2", "uuid": "281469bf-2016-4296-9470-1a2aa310d899"}]}
     *   ]
     *   "zones": [
     *     {"world": "world", "chunk_region_x": 0, "chunk_region_z": 0, "team": 0}
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

    public JSONArray getZones() {
        readData();
        return data.getJSONArray("zones");
    }

    public JSONArray getTeamZones(int teamIdx) {
        return getZones().getJSONArray(teamIdx);
    }

    /**
     * Finds and returns the capital zone of a team.
     *
     * @param teamIdx Team index
     * @return A JSONObject with info of the zone the player is in. If multiple zones meet the criteria, it will return the last entry in the list.
     */
    public JSONObject getCapitalZone(int teamIdx) {
        final JSONObject[] foundZone = {new JSONObject()};
        getTeamZones(teamIdx).forEach(obj -> {
            if (((JSONObject) obj).getBoolean("capital")) {
                foundZone[0] = (JSONObject) obj;
            }
        });
        return foundZone[0];
    }

    /**
     * Finds and returns the zone in the specified location
     *
     * @param location a location class
     * @return A JSONObject with info of the zone the player is in. If it cannot find a zone, it will return a new JSONObject.
     */
    public JSONObject findZoneFromLocation(Location location) {
        for (int i = 0; i < getZones().length(); i++) {
            JSONArray teamZones = getZones().getJSONArray(i);
            for (int j = 0; j < teamZones.length(); j++) {
                JSONObject teamZone = teamZones.getJSONObject(j);
                if (!Objects.equals(teamZone.getString("world"), location.getWorld().getName())) continue;
                if (location.getX() < teamZone.getInt("max_x") && location.getX() > teamZone.getInt("min_x")) {
                    if (location.getZ() < teamZone.getInt("max_z") && location.getZ() > teamZone.getInt("min_z")) {
                        teamZone.put("team", i);
                        return teamZone;
                    }
                }
            }
        }
        return new JSONObject();
    }
}
