package com.rift.zoneWars.zone.claimZone;

import com.rift.zoneWars.Teams;
import com.rift.zoneWars.ZoneWars;
import com.rift.zoneWars.zone.Zones;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.json.JSONObject;

import java.util.*;
import java.util.function.Consumer;

public class ClaimZone {

    private final ZoneWars plugin;
    private final UUID eventID;
    private int secondsRemaining;
    private final int invader, defender;
    private BukkitTask timer;
    private final Teams teams;
    private final Zones zones;
    private final JSONObject zone;
    private Consumer<EventOutcome> endCallback;
    private boolean isEnded = false;
    private static int countdown;

    public ClaimZone(ZoneWars plugin, Teams teams, Zones zones, int invader, int defender, JSONObject zone) {
        this.plugin = plugin;
        this.eventID = UUID.randomUUID();
        this.secondsRemaining = 120;
        this.invader = invader;
        this.defender = defender;
        this.teams = teams;
        this.zones = zones;
        this.zone = zone;
    }

    public JSONObject getZone() {
        return zone;
    }

    public void startClaimZone(Consumer<EventOutcome> onEndCallback) {
        if (zone.getBoolean("capital")) {
            secondsRemaining = (int) (60 * 5 + (double) (25 * (zones.getTeamTerritoryCount(defender) / zones.getDefaultTerritoryAmount())));
        }
        else {
            secondsRemaining = 120;
        }
        this.endCallback = onEndCallback;
        teams.getTeamMembers(defender).forEach(obj -> {
            Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).getString("uuid")));
            if (player != null && player.isOnline()) {
                if (zone.getBoolean("capital")) {
                    player.playSound(net.kyori.adventure.sound.Sound.sound(Key.key("entity.wither.spawn"), net.kyori.adventure.sound.Sound.Source.NEUTRAL, 1, 1));
                    player.playSound(net.kyori.adventure.sound.Sound.sound(Key.key("entity.wither.death"), net.kyori.adventure.sound.Sound.Source.NEUTRAL, 1, 1));
                    player.playSound(net.kyori.adventure.sound.Sound.sound(Key.key("entity.elder_guardian.curse"), net.kyori.adventure.sound.Sound.Source.NEUTRAL, 1, 1));
                }
                else {
                    player.playSound(net.kyori.adventure.sound.Sound.sound(Key.key("block.bell.use"), net.kyori.adventure.sound.Sound.Source.NEUTRAL, 1, 1));
                    player.playSound(net.kyori.adventure.sound.Sound.sound(Key.key("block.note_block.pling"), net.kyori.adventure.sound.Sound.Source.NEUTRAL, 1, 1));
                }
            }
        });
        this.timer = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (secondsRemaining <= 0) {
                teams.getTeamMembers(defender).forEach(obj -> {
                    Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).getString("uuid")));
                    if (player != null && player.isOnline()) {
                        player.setGlowing(false);
                    }
                });
                complete(EventOutcome.SUCCESS);
                return;
            }

            teams.getTeamMembers(invader).forEach(obj -> {
                Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).getString("uuid")));
                if (player != null && player.isOnline()) {
                    player.sendActionBar(MiniMessage.miniMessage().deserialize(
                            "You must hold the territory for <green>" + secondsRemaining + "</green> more seconds!"
                    ));
                    player.playSound(net.kyori.adventure.sound.Sound.sound(Key.key("ui.button.click"), net.kyori.adventure.sound.Sound.Source.NEUTRAL, 1, 1));
                }
            });

            teams.getTeamMembers(defender).forEach(obj -> {
                Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).getString("uuid")));
                if (player != null && player.isOnline() && secondsRemaining == 120) {
                    player.showTitle(Title.title(MiniMessage.miniMessage().deserialize("<bold><red>Your territory is being invaded!"), MiniMessage.miniMessage().deserialize("<gray>Go defend it!")));
                    player.sendMessage(MiniMessage.miniMessage().deserialize(String.format("""
The territory at <yellow>(%d, %d)</yellow> is being raided by another team!
""", zone.getInt("chunk_region_x") * 16, zone.getInt("chunk_region_z") * 16)));
                    Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(zones.getMaxHealth(player) + 2);
                    player.setGlowing(true);
                }
            });
            Map<UUID, Integer> teamInTerritory = new HashMap<>();
            for (Player player : zones.getPlayersInTerritory(zone.getInt("chunk_region_x"), zone.getInt("chunk_region_z"))) {
                if (teamInTerritory.containsKey(teams.getTeamUUID(teams.getTeamIndexFromPlayer(player.getUniqueId())))) {
                    teamInTerritory.put(teams.getTeamUUID(teams.getTeamIndexFromPlayer(player.getUniqueId())), teamInTerritory.get(teams.getTeamUUID(teams.getTeamIndexFromPlayer(player.getUniqueId()))) + 1);
                }
                else {
                    teamInTerritory.put(teams.getTeamUUID(teams.getTeamIndexFromPlayer(player.getUniqueId())), 1);
                }
            }
            secondsRemaining--;
            int defendingPlayers = 0;
            if (teamInTerritory.get(UUID.fromString(zone.get("team").toString())) != null) {
                defendingPlayers = teamInTerritory.get(UUID.fromString(zone.get("team").toString()));
            }
            if (!teamInTerritory.containsKey(teams.getTeamUUID(invader))) fail();
            for (Map.Entry<UUID, Integer> team : teamInTerritory.entrySet()) {
                plugin.getComponentLogger().info(String.valueOf(team.getValue()));
                if (team.getValue() - 2 + 1 < defendingPlayers) {
                    fail();
                }
            }
        }, 0L, 20L);
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    public void fail() {
        complete(EventOutcome.FAILURE);
    }

    private void complete(EventOutcome outcome) {
        if (isEnded) return;
        isEnded = true;

        if (timer != null) {
            timer.cancel();
        }

        if (endCallback != null) {
            endCallback.accept(outcome);
        }
    }

    public UUID getEventId() {
        return eventID;
    }
    public int getInvader() {
        return invader;
    }
    public int getDefender() {
        return defender;
    }
    public int getSecondsRemaining() {
        return secondsRemaining;
    }
}
