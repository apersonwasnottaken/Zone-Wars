package com.rift.zoneWars.zone.claimZone;

import com.rift.zoneWars.Teams;
import com.rift.zoneWars.ZoneWars;
import com.rift.zoneWars.zone.Zones;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
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

    public void startNewClaim(Teams teams, int invader, int defender, JSONObject zone, Entity initiator) {
        if (!plugin.getConfig().getBoolean("claiming_enabled")) {
            initiator.sendMessage(MiniMessage.miniMessage().deserialize("<red>Claiming isn't enabled!"));
            return;
        }
        int zoneChunkX = zone.getInt("chunk_region_x");
        int zoneChunkZ = zone.getInt("chunk_region_z");
        for (ClaimZone claimZone : activeEvents.values()) {
            if (claimZone.getInvader() == invader && claimZone.getDefender() == defender) {
                if (claimZone.getZone().getInt("chunk_region_x") == zoneChunkX && claimZone.getZone().getInt("chunk_region_z") == zoneChunkZ) {
                    initiator.sendMessage(MiniMessage.miniMessage().deserialize("<red>A similar claim already exists!"));
                    return; // Such claim already exists and should not be duplicated
                }
            }
        }
        if (
                    !((!zones.getTerritory(zoneChunkX - 2, zoneChunkZ).isEmpty() && teams.getTeamIndexFromUUID(UUID.fromString(zones.getTerritory(zoneChunkX - 2, zoneChunkZ).get("team").toString())) == invader) ||
                    (!zones.getTerritory(zoneChunkX + 2, zoneChunkZ).isEmpty() && teams.getTeamIndexFromUUID(UUID.fromString(zones.getTerritory(zoneChunkX + 2, zoneChunkZ).get("team").toString())) == invader) ||
                    (!zones.getTerritory(zoneChunkX, zoneChunkZ - 2).isEmpty() && teams.getTeamIndexFromUUID(UUID.fromString(zones.getTerritory(zoneChunkX, zoneChunkZ - 2).get("team").toString())) == invader) ||
                    (!zones.getTerritory(zoneChunkX, zoneChunkZ + 2).isEmpty() && teams.getTeamIndexFromUUID(UUID.fromString(zones.getTerritory(zoneChunkX, zoneChunkZ + 2).get("team").toString())) == invader))
        ) {
            initiator.sendMessage(MiniMessage.miniMessage().deserialize("<red>The territory is not adjacent to one of your territories!"));
            return;
        }
        int numDefendingPlayers = 0;
        for (Object obj : teams.getTeamMembers(defender)) {
            Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).getString("uuid")));
            if (player != null && player.isOnline()) {
                numDefendingPlayers++;
            }
        }
        if (numDefendingPlayers < 1) {
            initiator.sendMessage(MiniMessage.miniMessage().deserialize("<red>The defending team does not have any players online!"));
            return;
        }

        ClaimZone claimZone = new ClaimZone(plugin, teams, zones, invader, defender, zone);
        UUID id = claimZone.getEventId();
        activeEvents.put(id, claimZone);
        claimZone.startClaimZone((outcome) -> {
            activeEvents.remove(id);
            if (outcome == EventOutcome.SUCCESS) {
                // Transfer ownership of territory
                zones.updateTerritory(zone.put("team", teams.getTeamUUID(invader)));
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
                            zones.updateTerritory(teamZone.put("team", teams.getTeamUUID(invader)));
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
                            player.playSound(net.kyori.adventure.sound.Sound.sound(Key.key(random.nextInt(2) == 1 ? "item.goat_horn.sound.3" : "item.goat_horn.sound.5"), net.kyori.adventure.sound.Sound.Source.NEUTRAL, 1, 1));
                            AtomicInteger count = new AtomicInteger();
                            plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                                plugin.getServer().getWorld("world").spawnEntity(player.getLocation(), EntityType.FIREWORK_ROCKET);
                                count.getAndIncrement();
                            }, 0L, 10L);
                        }
                        else {
                            player.playSound(net.kyori.adventure.sound.Sound.sound(Key.key(random.nextInt(2) == 1 ? "item.goat_horn.sound.1" : "item.goat_horn.sound.6"), net.kyori.adventure.sound.Sound.Source.NEUTRAL, 1, 1));
                            player.playSound(net.kyori.adventure.sound.Sound.sound(Key.key("entity.player_levelup"), net.kyori.adventure.sound.Sound.Source.NEUTRAL, 1, 1));
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
                ClaimZoneCooldown cooldown = new ClaimZoneCooldown(plugin, teams, invader);
                cooldown.startCooldown();
            }
            else {
                teams.getTeamMembers(invader).forEach(obj -> {
                    Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).getString("uuid")));
                    if (player != null && player.isOnline()) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You do not have a sufficient advantage! (2 players needed)"));
                        player.setGlowing(false);
                    }
                });
                teams.getTeamMembers(invader).forEach(obj -> {
                    Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).getString("uuid")));
                    if (player != null && player.isOnline()) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>The invading team does not have a sufficient advantage."));
                        player.setGlowing(false);
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
