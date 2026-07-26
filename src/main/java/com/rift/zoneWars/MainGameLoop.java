package com.rift.zoneWars;

import com.rift.zoneWars.zone.Zones;
import com.rift.zoneWars.zone.claimZone.ClaimZoneEventManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class MainGameLoop {

    private final ZoneWars plugin;
    private final PluginData pluginData;
    private final Zones zones;
    private final Teams teams;
    private final ClaimZoneEventManager claimZoneEventManager;

    public MainGameLoop(ZoneWars plugin, PluginData pluginData, Zones zones, Teams teams, ClaimZoneEventManager claimZoneEventManager) {
        this.plugin = plugin;
        this.pluginData = pluginData;
        this.zones = zones;
        this.teams = teams;
        this.claimZoneEventManager = claimZoneEventManager;
    }

    public void startGameLoop() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                // Spawn particles in a 2 radius chunk area
                World world = plugin.getServer().getWorld("world");
                Chunk originChunk = world.getChunkAt(player.getLocation());
                List<List<Integer>> alreadyDrawn = new ArrayList<>();
                int radius = 4;
                for (int tx = -radius + originChunk.getX(); tx <= radius + originChunk.getX(); tx++) {
                    for (int tz = -radius + originChunk.getZ(); tz <= radius + originChunk.getZ(); tz++) {
                        if (alreadyDrawn.contains(List.of(tx, tz))) {
                            continue;
                        }
                        // Spawn particles for chunk
                        double minX = tx * 16 + 0.5;
                        double maxX = tx * 16 + 15.5;
                        double minZ = tz * 16 + 0.5;
                        double maxZ = tz * 16 + 15.5;
                        if (zones.getTerritory(tx, tz).isEmpty() || Objects.equals(zones.getTerritory(tx, tz).get("team").toString(), "-1")) continue;
                        int teamColor = teams.getTeamColor(teams.getTeamIndexFromUUID(
                                UUID.fromString(
                                        zones.getTerritory(tx, tz).get("team").toString()
                                )));
                        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(teamColor), 4.0f);
                        int step = 4;
                        for (double x = minX; x <= maxX; x += step) {
                            world.spawnParticle(Particle.DUST, new Location(world, x, world.getHighestBlockYAt((int) x, (int) minZ, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 2, minZ), 1, 0, 0, 0, 0, dustOptions);
                            world.spawnParticle(Particle.DUST, new Location(world, x, world.getHighestBlockYAt((int) x, (int) maxZ, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 2, maxZ), 1, 0, 0, 0, 0, dustOptions);
                        }
                        for (double z = minZ; z <= maxZ; z += step) {
                            world.spawnParticle(Particle.DUST, new Location(world, minX, world.getHighestBlockYAt((int) minX, (int) z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 2, z), 1, 0, 0, 0, 0, dustOptions);
                            world.spawnParticle(Particle.DUST, new Location(world, maxX, world.getHighestBlockYAt((int) maxX, (int) z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 2, z), 1, 0, 0, 0, 0, dustOptions);
                        }
                        alreadyDrawn.add(List.of(tx, tz));
                    }
                }
                // Every (200 / amount of teams) pieces of land = 1/2 a heart
                if (zones.getDefaultTerritoryAmount() > 0) {
                    int maxHealth = zones.getMaxHealth(player);
                    Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(maxHealth);
                }
                // Player buffs for capital
                if (!zones.findTerritoryFromLocation(player.getLocation()).isEmpty()) {
                    if (
                            zones.findTerritoryFromLocation(player.getLocation()).getBoolean(("capital")) &&
                            teams.getTeamIndexFromUUID(UUID.fromString(zones.findTerritoryFromLocation(player.getLocation()).get("team").toString())) == teams.getTeamIndexFromPlayer(player.getUniqueId())
                    ) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 20, 3, true));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20, 0, true));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20, 0, true));
                    }
                }
            }
            // Claiming territory (Needs Testing)
            for (JSONObject territory : zones.getAllTerritoryOccupiedByAPlayer()) {
                if (territory.isEmpty()) continue;
                Map<UUID, Integer> teamInTerritory = new HashMap<>();
                for (Player player : zones.getPlayersInTerritory(territory.getInt("chunk_region_x"), territory.getInt("chunk_region_z"))) {
                    if (teamInTerritory.containsKey(teams.getTeamUUID(teams.getTeamIndexFromPlayer(player.getUniqueId())))) {
                        teamInTerritory.put(teams.getTeamUUID(teams.getTeamIndexFromPlayer(player.getUniqueId())), teamInTerritory.get(teams.getTeamUUID(teams.getTeamIndexFromPlayer(player.getUniqueId()))) + 1);
                    }
                    else {
                        teamInTerritory.put(teams.getTeamUUID(teams.getTeamIndexFromPlayer(player.getUniqueId())), 1);
                    }
                }
                if (Objects.equals(territory.get("team").toString(), "-1")) {
                    continue;
                }
                if (teams.getTeamIndexFromUUID(UUID.fromString(territory.get("team").toString())) < 0) {
                    continue;
                }
                if (teamInTerritory.get(UUID.fromString(territory.get("team").toString())) == null) {
                    continue;
                }
                int defendingPlayers = teamInTerritory.get(UUID.fromString(territory.get("team").toString()));
                Object teamObj = territory.get("team");
                if (teamObj == null || Objects.equals(teamObj.toString(), "-1")) return;
                int teamIndex = teams.getTeamIndexFromUUID(UUID.fromString(teamObj.toString()));
                if (teamIndex == -1) return;
                List<?> members = teams.getTeamMembers(teamIndex).toList();
                long onlineDefendingTeamMembers = members.stream()
                        .map(obj -> ((java.util.Map<?, ?>) obj).get("uuid").toString())
                        .map(uuidStr -> plugin.getServer().getPlayer(UUID.fromString(uuidStr)))
                        .filter(player -> player != null && player.isOnline())
                        .count();
                for (Map.Entry<UUID, Integer> team : teamInTerritory.entrySet()) {
                    if (team.getValue() - 2 >= defendingPlayers && onlineDefendingTeamMembers > 0) {
                        // Start a claim
                        claimZoneEventManager.startNewClaim(teams, teams.getTeamIndexFromUUID(team.getKey()), territory.getInt("team"), territory);
                        break;
                    }
                }
            }

        }, 0L, 10L);
        MiniMessage miniMessage = MiniMessage.miniMessage();
        final Component title = miniMessage.deserialize("<bold><italic:false><yellow>ZONE WARS</yellow>");
        final Component blank = miniMessage.deserialize("");
        final Component currentZoneTitle = miniMessage.deserialize("<gold>Current Zone</gold>");
        final Component teamMembersTitle = miniMessage.deserialize("<aqua>Team Members</aqua>");
        final Component notInTeam = miniMessage.deserialize("Not in a team!");
        final Component noZones1 = miniMessage.deserialize("Territories have not been generated!");
        final Component noZones2 = miniMessage.deserialize("Please contact a server admin.");
        updateTeamCache();
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                World world = plugin.getServer().getWorld("world");
                Chunk originChunk = world.getChunkAt(player.getLocation());
                // Scoreboard
                Scoreboard scoreboard = player.getScoreboard();
                Objective objective = scoreboard.getObjective("zwscoreboard");

                if (objective == null) {
                    ScoreboardManager manager = Bukkit.getScoreboardManager();
                    scoreboard = manager.getNewScoreboard();
                    objective = scoreboard.registerNewObjective("zwscoreboard", Criteria.DUMMY, Component.text("ZONE WARS"));
                    objective.setDisplaySlot(DisplaySlot.SIDEBAR);

                    for (int i = 0; i < 31; i++) {
                        String entryKey = "§" + Integer.toHexString(i) + "§r";
                        Team team = scoreboard.registerNewTeam("line_" + i);
                        team.addEntry(entryKey);
                    }
                    player.setScoreboard(scoreboard);
                }
                objective.displayName(title);

                String currentZoneTeamHexColor;
                String currentZoneTeamName;
                if (zones.getTerritory(originChunk.getX(), originChunk.getZ()).isEmpty() || Objects.equals(zones.getTerritory(originChunk.getX(), originChunk.getZ()).get("team").toString(), "-1")) {
                    currentZoneTeamHexColor = "ffffff";
                    currentZoneTeamName = "No one!";
                }
                else {
                    currentZoneTeamHexColor = Integer.toHexString(teams.getTeamColor(teams.getTeamIndexFromUUID(UUID.fromString(zones.getTerritory(originChunk.getX(), originChunk.getZ()).get("team").toString()))));
                    currentZoneTeamHexColor = "0".repeat(6 - currentZoneTeamHexColor.length()) + currentZoneTeamHexColor;
                    currentZoneTeamName = teams.getTeamName(teams.getTeamIndexFromUUID(UUID.fromString(zones.getTerritory(originChunk.getX(), originChunk.getZ()).get("team").toString())));
                }
                int teamIdx = teams.getTeamIndexFromPlayer(player.getUniqueId());
                String teamHexColor;
                if (teams.getTeamIndexFromPlayer(player.getUniqueId()) < 0) {
                    teamHexColor = "ffffff";
                }
                else {
                    teamHexColor = Integer.toHexString(teams.getTeamColor(teamIdx));
                    teamHexColor = "0".repeat(6 - teamHexColor.length()) + teamHexColor;
                }
                ArrayList<Component> scoreboardLines = new ArrayList<>();
                scoreboardLines.add(blank);
                scoreboardLines.add(currentZoneTitle);
                scoreboardLines.add(miniMessage.deserialize("Occupied by: <#" + currentZoneTeamHexColor + ">" + (zones.getTerritory(originChunk.getX(), originChunk.getZ()).isEmpty() ? "No one!" : currentZoneTeamName)));
                scoreboardLines.add(miniMessage.deserialize("Team Capital: <bold>" + (!zones.getTerritory(originChunk.getX(), originChunk.getZ()).isEmpty() && zones.getTerritory(originChunk.getX(), originChunk.getZ()).getBoolean("capital") ? "<green>Yes</green>" : "<red>No</red>")));
                scoreboardLines.add(blank);
                if (teams.getTeamIndexFromPlayer(player.getUniqueId()) > -1) {
                    scoreboardLines.add(miniMessage.deserialize("<#" + teamHexColor + ">" + teams.getTeamName(teams.getTeamIndexFromPlayer(player.getUniqueId()))));
                    if (zones.getTerritories().isEmpty()) {
                        scoreboardLines.add(noZones1);
                        scoreboardLines.add(noZones2);
                    }
                    else {
                        scoreboardLines.add(miniMessage.deserialize("Claimed Zones: <aqua>" + zones.getTeamTerritories(teamIdx).length() + "</aqua>/<dark_aqua>" + (zones.getTerritories().length() - 4) + "</dark_aqua> (<dark_green>" + ((float) zones.getTeamTerritories(teamIdx).length() / (zones.getTerritories().length() - 4)) * 100 + "%</dark_green>)"));
                        scoreboardLines.add(miniMessage.deserialize("Capitals: " + (!zones.getCapitalTerritories(teamIdx).isEmpty() ? "<green>" : "<red>") + zones.getCapitalTerritories(teamIdx).length()));
                    }
                    scoreboardLines.add(blank);
                    scoreboardLines.add(teamMembersTitle);
                    teamsConfigCache.forEach(obj ->
                            ((JSONObject) obj).getJSONArray("members").forEach(obj1 -> {
                                boolean online = false;
                                if (plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj1).getString("uuid"))) != null) {
                                    online = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj1).getString("uuid"))).isOnline();
                                }
                                scoreboardLines.add(MiniMessage.miniMessage().deserialize("    " + (online ? "<green>" : "<gray>") + ((JSONObject) obj1).getString("username") + (!online ? " (Offline)" : "")));
                            })
                    );
                }
                else {
                    scoreboardLines.add(notInTeam);
                }

                for (int i = 0; i < 31; i++) {
                    Team team = scoreboard.getTeam("line_" + i);
                    if (team == null) continue;

                    String entryKey = "§" + Integer.toHexString(i) + "§r";

                    if (i < scoreboardLines.size()) {
                        team.prefix(scoreboardLines.get(i));

                        Score score = objective.getScore(entryKey);
                        score.setScore(scoreboardLines.size() - i);

                        score.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.fixed(Component.empty()));
                    } else {
                        team.prefix(Component.empty());
                        scoreboard.resetScores(entryKey);
                    }
                }
            }
        }, 0L, 2L);
    }

    private JSONArray teamsConfigCache;

    public void updateTeamCache() {
        teamsConfigCache = pluginData.getTeamsConfig();
    }
}
