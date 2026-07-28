package com.rift.zoneWars.zone;

import com.rift.zoneWars.PluginData;
import com.rift.zoneWars.Teams;
import com.rift.zoneWars.ZoneWars;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class Zones {
    // Zone size: 2x2 chunks

    private final ZoneWars plugin;
    private final ComponentLogger logger;
    private final PluginData pluginData;
    private int defaultTerritoryAmount = 0;
    private JSONArray territories;
    private final Teams teams;
    private final Map<String, JSONObject> territoryCache = new ConcurrentHashMap<>();


    public Zones(ZoneWars plugin, PluginData pluginData, Teams teams) {
        this.pluginData = pluginData;
        this.plugin = plugin;
        this.logger = plugin.getComponentLogger();
        this.teams = teams;
        this.territories = pluginData.getTerritoriesConfig();
        rebuildTerritoryCache();
        updateDefaultTerritoryAmount();
    }

    public void updateDefaultTerritoryAmount() {
        int borderRadius = (int) (plugin.getServer().getWorld("world").getWorldBorder().getSize() / 2);
        int minCoord = ((((-borderRadius / 16 - 1) / 2)) * 2);
        int maxCoord = ((((borderRadius / 16 + 1) / 2)) * 2);
        int xZones = 0;
        for (int x = minCoord; x < maxCoord; x += 2) {
            xZones++;
        }
        int zZones = 0;
        for (int z = minCoord; z < maxCoord; z += 2) {
            zZones++;
        }
        int totalZonesGenerated = xZones * zZones;
        if (pluginData.getTeamsConfig().length() == 0) {
            defaultTerritoryAmount = 0;
        }
        else {
            defaultTerritoryAmount = totalZonesGenerated / pluginData.getTeamsConfig().length();
        }
    }

    public int getDefaultTerritoryAmount() {
        return defaultTerritoryAmount;
    }

    public void generateZones(int numTeams, World world, Player executor) {
        double borderRadius = world.getWorldBorder().getSize() / 2;
        if (borderRadius > 10000) {
            executor.sendMessage(MiniMessage.miniMessage().deserialize("<red>The border size is too big! (" + borderRadius + "). The task has been aborted."));
        }
        int minCoord = (((int) ((-borderRadius / 16 - 1) / 2)) * 2);
        int maxCoord = (((int) ((borderRadius / 16 + 1) / 2)) * 2);
        int xZones = 0;
        for (int x = minCoord; x < maxCoord; x += 2) {
            xZones++;
        }
        int zZones = 0;
        for (int z = minCoord; z < maxCoord; z += 2) {
            zZones++;
        }
        int totalZonesGenerated = xZones * zZones;
        defaultTerritoryAmount = totalZonesGenerated / numTeams;
        long xSteps = Math.max(0, (long) Math.ceil((double)(maxCoord - minCoord) / 2));
        long zSteps = Math.max(0, (long) Math.ceil((double)(maxCoord - minCoord) / 2));
        int iterations = (int) (xSteps * zSteps);
        BossBar bossBar = BossBar.bossBar(
                MiniMessage.miniMessage().deserialize("<green>Generating Zones"),
                0.0f,
                BossBar.Color.GREEN,
                BossBar.Overlay.NOTCHED_10
        );
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            int i = 0;
            JSONArray newTerritories = new JSONArray();
            if (pluginData.getTeamsConfig().length() < 2) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    executor.sendMessage(MiniMessage.miniMessage().deserialize("<red>Not enough teams!"));
                    executor.hideBossBar(bossBar);
                });
                return;
            }
            UUID team1UUID = teams.getTeamUUID(0);
            UUID team2UUID = teams.getTeamUUID(1);
            for (int x = minCoord; x < maxCoord; x += 2) {
                for (int z = minCoord; z < maxCoord; z += 2) {
                    JSONObject zone = new JSONObject();
                    zone.put("world", world.getName());
                    zone.put("chunk_region_x", x);
                    zone.put("chunk_region_z", z);
                    if (x >= -2 && x < 2 && z >= -2 && z < 2) {
                        zone.put("team", -1);
                        zone.put("capital", false);
                        newTerritories.put(zone);
                        continue;
                    } else {
                        switch (numTeams) {
                            case 2:
                                zone.put("team", x < 0 ? team1UUID.toString() : team2UUID.toString());
                                break;
                            case 4:
                                if (x < 0) {
                                    zone.put("team", z < 0 ? team1UUID.toString() : team2UUID.toString());
                                } else {
                                    UUID team3UUID = teams.getTeamUUID(2);
                                    UUID team4UUID = teams.getTeamUUID(3);
                                    zone.put("team", z < 0 ? team3UUID.toString() : team4UUID.toString());
                                }
                                break;
                            default:
                                zone.put("team", -1);
                                break;
                        }
                    }
                    zone.put("capital", false);
                    newTerritories.put(zone);
                    i++;
                    final float progress = Math.max(0, Math.min(1.0f, (float) i / iterations));
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        bossBar.progress(progress);
                    });
                }
            }
            this.territories = newTerritories;
            updateTerritories();
            rebuildTerritoryCache();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                executor.hideBossBar(bossBar);
                executor.sendMessage(MiniMessage.miniMessage().deserialize("<green>Zones successfully generated!"));
            });
        });
    }

    public JSONArray getTerritories() {
        return territories;
    }

    public void updateTerritories() {
        pluginData.updateData(pluginData.readData().put("territories", territories));
    }

    public void resetZones() {
        territories = new JSONArray();
        pluginData.updateData(pluginData.readData().put("territories", territories));
    }

    /**
     * Finds and returns the territories that a team has
     * @param teamIdx team index
     * @return A JSONArray containing a list of a team's territory. If they have none, returns an empty JSONArray
     */
    public JSONArray getTeamTerritories(int teamIdx) {
        JSONArray teamTerritories = new JSONArray();
        territories.forEach(obj -> {
            if ((Objects.equals(((JSONObject) obj).get("team").toString(), "-1") ? -1 : teams.getTeamIndexFromUUID(UUID.fromString(((JSONObject) obj).get("team").toString()))) == teamIdx) {
                teamTerritories.put(obj);
            }
        });
        return teamTerritories;
    }

    public void rebuildTerritoryCache() {
        territoryCache.clear();
        for (Object obj : territories) {
            JSONObject territory = (JSONObject) obj;
            int rx = territory.getInt("chunk_region_x");
            int rz = territory.getInt("chunk_region_z");

            for (int xOffset = 0; xOffset <= 1; xOffset++) {
                for (int zOffset = 0; zOffset <= 1; zOffset++) {
                    String key = (rx + xOffset) + "," + (rz + zOffset);
                    territoryCache.put(key, territory);
                }
            }
        }
    }

    public JSONObject getTerritory(int tx, int tz) {
        String key = tx + "," + tz;
        JSONObject territory = territoryCache.get(key);
        return territory != null ? territory : new JSONObject();
    }

    /**
     * Finds and returns the capital territory of a team.
     *
     * @param teamIdx Team index
     * @return A JsONArray with a list of info of the capital territories. If the team does not have a capital territory, it returns an empty JSONArray.
     */
    public JSONArray getCapitalTerritories(int teamIdx) {
        JSONArray foundTerritory = new JSONArray();
        getTeamTerritories(teamIdx).forEach(obj -> {
            if (((JSONObject) obj).getBoolean("capital")) {
                foundTerritory.put(obj);
            }
        });
        return foundTerritory;
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
                if (Objects.equals(territory.getInt("chunk_region_x"), newData.getInt("chunk_region_x"))) {
                    if (Objects.equals(territory.getInt("chunk_region_z"), newData.getInt("chunk_region_z"))) {
                        territories.put(i, newData);
                        updateTerritories();
                    }
                }
            }
        }
        rebuildTerritoryCache();
    }

    public int getTeamTerritoryCount(int teamIdx) {
        return getTeamTerritories(teamIdx).length();
    }

    public ArrayList<Player> getPlayersInTerritory(int tx, int tz) {
        ArrayList<Player> playersInTerritory = new ArrayList<>();
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            if (!findTerritoryFromLocation(player.getLocation()).isEmpty()) {
                if (findTerritoryFromLocation(player.getLocation()).getInt("chunk_region_x") == tx && findTerritoryFromLocation(player.getLocation()).getInt("chunk_region_z") == tz) {
                    playersInTerritory.add(player);
                }
            }
        });
        return playersInTerritory;
    }

    public int getMaxHealth(Player player) {
        return 20 - Math.max(-20, Math.min(19, (getTeamTerritoryCount(teams.getTeamIndexFromPlayer(player.getUniqueId())) - getDefaultTerritoryAmount()) / (200 / pluginData.getTeamsConfig().length())));
    }

    // Turns a random piece of a team's territory into a capital
    public boolean createCapital(int teamIdx) {
        if (!getCapitalTerritories(teamIdx).isEmpty()) {
            return false;
        }
        JSONArray teamTerritories = getTeamTerritories(teamIdx);
        Random random = new Random();

        JSONObject capital = teamTerritories.getJSONObject(
                random.nextInt(teamTerritories.length()));

        for (int i = 0; i < territories.length(); i++) {
            JSONObject territory = territories.getJSONObject(i);
            if (territory.getString("world").equals(capital.getString("world"))
                    && territory.getInt("chunk_region_x") == capital.getInt("chunk_region_x")
                    && territory.getInt("chunk_region_z") == capital.getInt("chunk_region_z")) {
                territory.put("capital", true);
                territories.put(i, territory);
                updateTerritories();
                rebuildTerritoryCache();
                return true;
            }
        }
        return false;
    }

    public boolean removeCapital(int teamIdx) {
        for (int i = 0; i < territories.length(); i++) {
            JSONObject territory = territories.getJSONObject(i);
            if (territory.getString("team").equals(teams.getTeamUUID(teamIdx).toString())
                    && territory.getBoolean("capital")) {
                territory.put("capital", false);
                territories.put(i, territory);
                updateTerritories();
                rebuildTerritoryCache();
                return true;
            }
        }
        return false;
    }

    public ArrayList<JSONObject> getAllTerritoryOccupiedByAPlayer() {
        ArrayList<JSONObject> territories = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            territories.add(findTerritoryFromLocation(player.getLocation()));
        }
        return territories;
    }
}
