package com.rift.zoneWars.zone.claimZone;

import com.rift.zoneWars.Teams;
import com.rift.zoneWars.ZoneWars;
import com.rift.zoneWars.zone.Zones;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClaimZoneEventManager {
    private final ZoneWars plugin;
    private final Zones zones;
    private final Map<UUID, ClaimZone> activeEvents = new HashMap<>();

    public ClaimZoneEventManager(ZoneWars plugin, Zones zones) {
        this.plugin = plugin;
        this.zones = zones;
    }

    public void startNewClaim(Teams teams, int invader, int defender, JSONObject zone) {
        for (ClaimZone claimZone : activeEvents.values()) {
            if (claimZone.getInvader() == invader && claimZone.getDefender() == defender) {
                if (claimZone.getZone().getInt("chunk_region_x") == zone.getInt("chunk_region_x") && claimZone.getZone().getInt("chunk_region_z") == zone.getInt("chunk_region_z")) {
                    return; // Such claim already exists and should not be duplicated
                }
            }
        }
        ClaimZone claimZone = new ClaimZone(plugin, teams, invader, defender, zone);
        UUID id = claimZone.getEventId();
        activeEvents.put(id, claimZone);
        claimZone.startClaimZone((outcome) -> {
            activeEvents.remove(id);
            if (outcome == EventOutcome.SUCCESS) {
                // Transfer ownership of territory
                zones.updateTerritory(zone.put("team", invader));
                teams.getTeamMembers(invader).forEach(obj -> {
                    Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).getString("uuid")));
                    if (player != null && player.isOnline()) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Your team managed to claim the territory!"));
                    }
                });
                teams.getTeamMembers(defender).forEach(obj -> {
                    Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).getString("uuid")));
                    if (player != null && player.isOnline()) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Your team was not able to defend the territory."));
                    }
                });
            }
            else if (outcome == EventOutcome.FAILURE) {
                // Reset timer
                teams.getTeamMembers(invader).forEach(obj -> {
                    Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).getString("uuid")));
                    if (player != null && player.isOnline()) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You ahve lost the advantage, the timer has been reset."));
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
