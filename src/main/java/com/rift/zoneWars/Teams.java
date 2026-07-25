package com.rift.zoneWars;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Teams {
    private final ZoneWars plugin;
    private final PluginData pluginData;

    public Teams(ZoneWars plugin, PluginData pluginData) {
        this.plugin = plugin;
        this.pluginData = pluginData;
    }

    public JSONObject getTeam(int teamIdx) {
        if (teamIdx >= pluginData.getTeamsConfig().length() || teamIdx < 0) {
            return new JSONObject();
        }
        return pluginData.getTeamsConfig().getJSONObject(teamIdx);
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
        for (int i = 0; i < pluginData.getTeamsConfig().length(); i++) {
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
        for (int i = 0; i < pluginData.getTeamsConfig().length(); i++) {
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
}
