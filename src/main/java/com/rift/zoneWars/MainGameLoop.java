package com.rift.zoneWars;

import com.rift.zoneWars.zone.Zones;
import com.rift.zoneWars.zone.claimZone.ClaimZoneEventManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
                // Every (200 / amount of teams) pieces of land = 1/2 a heart
                if (zones.getDefaultTerritoryAmount() > 0) {
                    int maxHealth = 20 - Math.max(-20, Math.min(19, (zones.getTeamTerritoryCount(teams.getTeamIndexFromPlayer(player.getUniqueId())) - zones.getDefaultTerritoryAmount()) / (200 / pluginData.getTeamsConfig().length())));
                    if (!zones.findTerritoryFromLocation(player.getLocation()).isEmpty()) {
                        if (zones.findTerritoryFromLocation(player.getLocation()).getBoolean(("capital")) && teams.getTeamIndexFromUUID(UUID.fromString(zones.findTerritoryFromLocation(player.getLocation()).get("team").toString())) == teams.getTeamIndexFromPlayer(player.getUniqueId())) {
                            maxHealth += 16;
                        }
                    }
                    Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(maxHealth);
                }
                // Player buffs for capital
                if (!zones.findTerritoryFromLocation(player.getLocation()).isEmpty()) {
                    if (
                            zones.findTerritoryFromLocation(player.getLocation()).getBoolean(("capital")) &&
                            teams.getTeamIndexFromUUID(UUID.fromString(zones.findTerritoryFromLocation(player.getLocation()).get("team").toString())) == teams.getTeamIndexFromPlayer(player.getUniqueId())
                    ) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20, 0, true));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20, 0, true));
                    }
                }
            }
            // To-do: Claiming territory
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
                for (Map.Entry<UUID, Integer> team : teamInTerritory.entrySet()) {
                    if (team.getValue() - 2 >= defendingPlayers) {
                        // Start a claim
                        claimZoneEventManager.startNewClaim(teams, teams.getTeamIndexFromUUID(team.getKey()), territory.getInt("team"), territory);
                        break;
                    }
                }
            }

        }, 0L, 10L);
    }

}
