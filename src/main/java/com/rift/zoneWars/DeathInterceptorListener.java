package com.rift.zoneWars;

import com.rift.zoneWars.zone.Zones;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class DeathInterceptorListener implements Listener {

    private Teams teams;
    private Zones zones;

    public DeathInterceptorListener(Teams teams, Zones zones) {
        this.teams = teams;
        this.zones = zones;
    }

    @EventHandler
    public void playerRespawnEvent(PlayerRespawnEvent event) {
        int teamIdx = teams.getTeamIndexFromPlayer(event.getPlayer().getUniqueId());
        if (teamIdx == -1) return;
        if (zones.getCapitalTerritories(teamIdx).isEmpty()) return;
        event.setRespawnLocation(new Location(Bukkit.getWorld("world"), zones.getCapitalTerritories(teamIdx).getJSONObject(0).getInt("chunk_region_x") * 16 + 15, Bukkit.getWorld("world").getHighestBlockYAt(zones.getCapitalTerritories(teamIdx).getJSONObject(0).getInt("chunk_region_x") * 16 + 15, zones.getCapitalTerritories(teamIdx).getJSONObject(0).getInt("chunk_region_z") * 16 + 15), zones.getCapitalTerritories(teamIdx).getJSONObject(0).getInt("chunk_region_z") * 16 + 15));
    }
}
