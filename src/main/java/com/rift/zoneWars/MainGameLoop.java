package com.rift.zoneWars;

import com.rift.zoneWars.zone.Zones;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public class MainGameLoop {

    private final ZoneWars plugin;
    private final Zones zones;
    private final Teams teams;

    public MainGameLoop(ZoneWars plugin, Zones zones, Teams teams) {
        this.plugin = plugin;
        this.zones = zones;
        this.teams = teams;
    }

    public void startGameLoop() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                // Player buffs for capital
                if (zones.findTerritoryFromLocation(player.getLocation()).getBoolean(("capital")) && zones.findTerritoryFromLocation(player.getLocation()).getInt("team") == teams.getTeamIndexFromPlayer(player.getUniqueId())) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20, 3, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20, 0, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20, 0, true));
                }
                // Every 50 pieces of land = 1/2 a heart
                if (zones.getDefaultTerritoryAmount() > 0) {
                    Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20 - Math.max(-20, Math.min(19, (zones.getTeamTerritoryCount(teams.getTeamIndexFromPlayer(player.getUniqueId())) - zones.getDefaultTerritoryAmount()) / 50)));
                }
            }
        }, 0L, 10L);
    }

}
