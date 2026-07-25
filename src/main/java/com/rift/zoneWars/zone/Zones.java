package com.rift.zoneWars.zone;

import com.rift.zoneWars.PluginData;
import com.rift.zoneWars.ZoneWars;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.A;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

public class Zones {
    // Zone size: 2x2 chunks

    private final ZoneWars plugin;
    private final ComponentLogger logger;
    private final PluginData pluginData;
    private int defaultTerritoryAmount = 0;

    public Zones(ZoneWars plugin, PluginData pluginData) {
        this.pluginData = pluginData;
        this.plugin = plugin;
        this.logger = plugin.getComponentLogger();
    }

    public int getDefaultTerritoryAmount() {
        return defaultTerritoryAmount;
    }

    public void generateZones(int borderRadius, int teams, World world) {
        defaultTerritoryAmount = (int) (Math.pow(borderRadius * 2 / 32, 2) / teams);
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
                    switch (teams) {
                        case 2:
                            if (x < 0) {
                                zone.put("team", 0);
                            }
                            else {
                                zone.put("team", 1);
                            }
                            break;
                        case 4:
                            if (x < 0) {
                                if (z < 0) {
                                    zone.put("team", 0);
                                }
                                else {
                                    zone.put("team", 1);
                                }
                            }
                            else {
                                if (z < 0) {
                                    zone.put("team", 2);
                                }
                                else {
                                    zone.put("team", 3);
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
    }

    public JSONArray getTerritories() {
        return pluginData.readData().getJSONArray("territories");
    }

    public void updateTerritories(JSONArray newTerritories) {
        pluginData.updateData(pluginData.readData().put("territories", newTerritories));
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
            if (Objects.equals(territory.getString("world"), newData.getString("world"))) {
                if (Objects.equals(territory.getString("chunk_region_x"), newData.getString("chunk_region_x"))) {
                    if (Objects.equals(territory.getString("chunk_region_z"), newData.getString("chunk_region_z"))) {
                        updateTerritories(getTerritories().put(i, newData));
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
}
