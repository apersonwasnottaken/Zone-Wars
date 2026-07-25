package com.rift.zoneWars.zone;

import com.rift.zoneWars.PluginData;
import com.rift.zoneWars.Teams;
import com.rift.zoneWars.ZoneWars;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class Zones {
    // Zone size: 2x2 chunks

    private final ZoneWars plugin;
    private final ComponentLogger logger;
    private final PluginData pluginData;
    private final Teams teams;
    private int defaultTerritoryAmount = 0;
    private JSONArray territories;

    public Zones(ZoneWars plugin, PluginData pluginData, Teams teams) {
        this.pluginData = pluginData;
        this.plugin = plugin;
        this.logger = plugin.getComponentLogger();
        this.teams = teams;
        this.territories = pluginData.getTerritoriesConfig();
    }

    public int getDefaultTerritoryAmount() {
        return defaultTerritoryAmount;
    }

    public void generateZones(int numTeams, World world) {
        int borderRadius = (int) world.getWorldBorder().getMaxSize();
        defaultTerritoryAmount = (int) (Math.pow(borderRadius * 2 / 32, 2) / numTeams);
        JSONArray zoneData = new JSONArray();
        for (int x = ((-borderRadius / 16 - 1) / 2) * 2; x < borderRadius / 16 + 1; x += 2) {
            for (int z = ((-borderRadius / 16 - 1) / 2) * 2; z < borderRadius / 16; z += 2) {
                JSONObject zone = new JSONObject();
                zone.put("world", world.getName());
                zone.put("chunk_region_x", x);
                zone.put("chunk_region_z", z);
                if (x >= -2 && x < 2 && z >= -2 && z < 2) { // Needs testing
                    zone.put("team", -1);
                }
                else {
                    UUID team1UUID = teams.getTeamUUID(0);
                    UUID team2UUID = teams.getTeamUUID(1);
                    UUID team3UUID = teams.getTeamUUID(2);
                    UUID team4UUID = teams.getTeamUUID(3);
                    switch (numTeams) {
                        case 2:
                            if (x < 0) {
                                zone.put("team", team1UUID.toString());
                            }
                            else {
                                zone.put("team", team2UUID.toString());
                            }
                            break;
                        case 4:
                            if (x < 0) {
                                if (z < 0) {
                                    zone.put("team", team1UUID.toString());
                                }
                                else {
                                    zone.put("team", team2UUID.toString());
                                }
                            }
                            else {
                                if (z < 0) {
                                    zone.put("team", team3UUID.toString());
                                }
                                else {
                                    zone.put("team", team4UUID.toString());
                                }
                            }
                            break;
                        case 3, 5, 6:
                            break;
                        default:
                            zone.put("team", -1);
                            break;
                    }
                }
                zone.put("capital", false);
                zoneData.put(zone);
            }
        }
        updateTerritories();
    }

    public void updateTerritories() {
        pluginData.updateData(pluginData.readData().put("territories", territories));
    }

    public void getTerritories() {
        territories = pluginData.getTerritoriesConfig();
    }

    /**
     * Finds and returns the territories that a team has
     * @param teamIdx
     * @return A JSONArray containing a list of a team's territory. If they have none, returns an empty JSONArray
     */
    public JSONArray getTeamTerritories(int teamIdx) {
        getTerritories();
        JSONArray teamTerritories = new JSONArray();
        territories.forEach(obj -> {
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
        getTerritories();
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
        for (int i = 0; i < territories.length(); i++) {
            JSONObject territory = territories.getJSONObject(i);
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
        for (int i = 0; i < territories.length(); i++) {
            JSONObject territory = territories.getJSONObject(i);
            // Checks world and position only.
            if (Objects.equals(territory.getString("world"), newData.getString("world"))) {
                if (Objects.equals(territory.getString("chunk_region_x"), newData.getString("chunk_region_x"))) {
                    if (Objects.equals(territory.getString("chunk_region_z"), newData.getString("chunk_region_z"))) {
                        territories.put(i, newData);
                        updateTerritories();
                    }
                }
            }
        }
    }

    public int getTeamTerritoryCount(int teamIdx) {
        return getTeamTerritories(teamIdx).length();
    }

    public ArrayList<Player> getPlayersInTerritory(int tx, int tz) {
        ArrayList<Player> playersInTerritory = new ArrayList<>();
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            if (findTerritoryFromLocation(player.getLocation()).getInt("chunk_region_x") == tx && findTerritoryFromLocation(player.getLocation()).getInt("chunk_region_x") == tz) {
                playersInTerritory.add(player);
            }
        });
        return playersInTerritory;
    }

    public ArrayList<JSONObject> getAllTerritoryOccupiedByAPlayer() {
        ArrayList<JSONObject> territories = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            territories.add(findTerritoryFromLocation(player.getLocation()));
        }
        return territories;
    }
}
