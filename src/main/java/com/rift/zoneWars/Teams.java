package com.rift.zoneWars;

import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Teams {
    private final ZoneWars plugin;
    private final PluginData pluginData;
    private JSONArray teamsData;

    public Teams(ZoneWars plugin, PluginData pluginData) {
        this.plugin = plugin;
        this.pluginData = pluginData;
        this.teamsData = pluginData.getTeamsConfig();
    }

    public void addTeam(String name, int color) {
        JSONObject newTeam = new JSONObject();
        newTeam.put("name", name);
        newTeam.put("color", color);
        newTeam.put("id", UUID.randomUUID());
        newTeam.put("members", new JSONArray());
        teamsData.put(newTeam);
        updateTeams();
    }

    public void deleteTeam(UUID teamUUID) {
        updateTeams();
        for (int i = 0; i < teamsData.length(); i++) {
            if (Objects.equals(((JSONObject) teamsData.get(i)).get("id").toString(), teamUUID.toString())) {
                teamsData.remove(i);
                break;
            }
        }
    }

    public void addMemberToTeam(int teamIdx, Player member) {
        for (int i = 0; i < ((JSONObject) teamsData.get(teamIdx)).getJSONArray("members").length(); i++) {
            if (Objects.equals(((JSONObject) ((JSONObject) teamsData.get(teamIdx)).getJSONArray("members").get(i)).get("uuid").toString(), member.getUniqueId().toString())) {
                return;
            }
        }
        ((JSONObject) teamsData.get(teamIdx)).getJSONArray("members").put(new JSONObject().put("username", member.getName()).put("uuid", member.getUniqueId()));
        updateTeams();
    }

    public void removeMemberFromTeam(int teamIdx, Player member) {
        for (int i = 0; i < ((JSONObject) teamsData.get(teamIdx)).getJSONArray("members").length(); i++) {
            if (Objects.equals(((JSONObject) ((JSONObject) teamsData.get(teamIdx)).getJSONArray("members").get(i)).get("uuid").toString(), member.getUniqueId().toString())) {
                ((JSONObject) teamsData.get(teamIdx)).getJSONArray("members").remove(i);
            }
        }
        updateTeams();
    }

    public JSONObject getTeam(int teamIdx) {
        return teamsData.getJSONObject(teamIdx);
    }

    public UUID getTeamUUID(int teamIdx) {
        return UUID.fromString(getTeam(teamIdx).get("id").toString());
    }

    public int getTeamIndexFromUUID(UUID teamUUID) {
        for (int i = 0; i < teamsData.length(); i++) {
            if (Objects.equals(((JSONObject) teamsData.get(i)).get("id").toString(), teamUUID.toString())) {
                return i;
            }
        }
        return -1;
    }

    public ArrayList<UUID> getAllTeamUUIDs() {
        ArrayList<UUID> uuids = new ArrayList<>();
        teamsData.forEach(obj -> {
            uuids.add(UUID.fromString(((JSONObject) obj).get("id").toString()));
        });
        return uuids;
    }

    public void updateTeams() {
        JSONObject newData = pluginData.readData().put("teams", teamsData);
        pluginData.updateData(newData);
    }

    public String getTeamName(int teamIdx) {
        return getTeam(teamIdx).getString("name");
    }

    public void setTeamName(int teamIdx, String name) {
        teamsData.put(teamIdx, getTeam(teamIdx).put("name", name));
        updateTeams();
    }

    public int getTeamColor(int teamIdx) {
        return getTeam(teamIdx).getInt("color");
    }

    public void setTeamColor(int teamIdx, int color) {
        teamsData.put(teamIdx, getTeam(teamIdx).put("color", color));
        updateTeams();
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
                if (Objects.equals(UUID.fromString(((JSONObject) obj).get("uuid").toString()), uuid)) {
                    foundTeam.set(finalI);
                }
            });
        }
        return foundTeam.get();
    }
}
