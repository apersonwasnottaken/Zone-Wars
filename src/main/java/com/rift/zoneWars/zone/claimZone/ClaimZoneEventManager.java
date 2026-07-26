package com.rift.zoneWars.zone.claimZone;

import com.rift.zoneWars.Teams;
import com.rift.zoneWars.ZoneWars;
import com.rift.zoneWars.zone.Zones;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ClaimZoneEventManager {
    private final ZoneWars plugin;
    private final Zones zones;
    private final Map<UUID, ClaimZone> activeEvents = new HashMap<>();

    public ClaimZoneEventManager(ZoneWars plugin, Zones zones) {
        this.plugin = plugin;
        this.zones = zones;
    }

    public void startNewClaim(Teams teams, int invader, int defender, JSONObject zone) {
        if (!plugin.getConfig().getBoolean("claiming_enabled")) return;
        for (ClaimZone claimZone : activeEvents.values()) {
            if (claimZone.getInvader() == invader && claimZone.getDefender() == defender) {
                if (claimZone.getZone().getInt("chunk_region_x") == zone.getInt("chunk_region_x") && claimZone.getZone().getInt("chunk_region_z") == zone.getInt("chunk_region_z")) {
                    return; // Such claim already exists and should not be duplicated
                }
            }
        }
        ClaimZone claimZone = new ClaimZone(plugin, teams, zones, invader, defender, zone);
        UUID id = claimZone.getEventId();
        activeEvents.put(id, claimZone);
        claimZone.startClaimZone((outcome) -> {
            activeEvents.remove(id);
            if (outcome == EventOutcome.SUCCESS) {
                // Transfer ownership of territory
                zones.updateTerritory(zone.put("team", invader));
                Component invaderMessage;
                Component defenderMessage;
                if (zone.getBoolean("capital")) {
                    // Trigger territory loss
                    int capitalTerritoryCount = zones.getCapitalTerritories(defender).length() + 1;
                    int totalTerritory = zones.getTeamTerritoryCount(defender);
                    AtomicInteger lostTerritory = new AtomicInteger();
                    Random random = new Random();
                    zones.getTeamTerritories(defender).forEach(obj -> {
                        JSONObject teamZone = (JSONObject) obj;
                        if (random.nextInt(capitalTerritoryCount) == 0) {
                            zones.updateTerritory(teamZone.put("team", invader));
                            lostTerritory.getAndIncrement();
                        }
                    });
                    invaderMessage = MiniMessage.miniMessage().deserialize(String.format("""
                            <green>Your team managed to claim the capital!
                            <green>Your team has claimed <gold>%f%%</gold> of the opposing team's territory!
                            """, ((float) lostTerritory.get() / totalTerritory) * 100));
                    defenderMessage = MiniMessage.miniMessage().deserialize(String.format("""
                            <red>Your team has lost the capital!
                            <red>Your team has lost <dark_red>%f%%</dark_red> of your territory!
                            """, ((float) lostTerritory.get() / totalTerritory) * 100));
                }
                else {
                    invaderMessage = MiniMessage.miniMessage().deserialize("<green>Your team managed to claim the territory!");
                    defenderMessage = MiniMessage.miniMessage().deserialize("<red>Your team was not able to defend the territory.");
                }
                teams.getTeamMembers(invader).forEach(obj -> {
                    Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).getString("uuid")));
                    if (player != null && player.isOnline()) {
                        Random random = new Random();
                        if (zone.getBoolean("capital")) {
                            player.playSound((net.kyori.adventure.sound.Sound) (random.nextInt(2) == 1 ? Sound.ITEM_GOAT_HORN_SOUND_3 : Sound.ITEM_GOAT_HORN_SOUND_5));
                            AtomicInteger count = new AtomicInteger();
                            plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                                plugin.getServer().getWorld("world").spawnEntity(player.getLocation(), EntityType.FIREWORK_ROCKET);
                                count.getAndIncrement();
                            }, 0L, 10L);
                        }
                        else {
                            player.playSound((net.kyori.adventure.sound.Sound) (random.nextInt(2) == 1 ? Sound.ITEM_GOAT_HORN_SOUND_1 : Sound.ITEM_GOAT_HORN_SOUND_6));
                            player.playSound((net.kyori.adventure.sound.Sound) Sound.ENTITY_PLAYER_LEVELUP);
                        }
                        player.sendMessage(invaderMessage);
                    }
                });
                teams.getTeamMembers(defender).forEach(obj -> {
                    Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).getString("uuid")));
                    if (player != null && player.isOnline()) {
                        if (zone.getBoolean("capital")) {
                            player.playSound((net.kyori.adventure.sound.Sound) Sound.ENTITY_ENDER_DRAGON_DEATH);
                        }
                        else {
                            player.playSound((net.kyori.adventure.sound.Sound) Sound.BLOCK_BEACON_DEACTIVATE);
                        }
                        player.sendMessage(defenderMessage);
                    }
                });
            }
            else if (outcome == EventOutcome.FAILURE) {
                // Reset timer
                teams.getTeamMembers(invader).forEach(obj -> {
                    Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).getString("uuid")));
                    if (player != null && player.isOnline()) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have lost the advantage, the timer has been reset."));
                    }
                });
            }
        });
    }

    public void clearAllEvents() {
        for (ClaimZone claimZone : activeEvents.values()) {
            claimZone.stop();
        }
        activeEvents.clear();
    }

}
