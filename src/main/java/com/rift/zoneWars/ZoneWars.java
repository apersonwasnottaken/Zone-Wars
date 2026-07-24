package com.rift.zoneWars;

import org.bukkit.plugin.java.JavaPlugin;

public final class ZoneWars extends JavaPlugin {

    /*
     * Zone Wars
     * The map is split up equally among all teams
     *
     * Team size: 6-8 members
     * In order to stake a claim, the attacking team must have at least 2 more members than the defending team on the plot of territory for at least 10 minutes
     * If you lose the advantage that timer resets
     * Defending team gets perm +1 max health
     * There must be at least one member of the defending team online when trying to claim territory
     *
     * Capital Zones:
     * Gives the defending team significantly more buffs (Perm god apple effects)
     * Once the capital zone is claimed the team it belonged to gets debuffed (1/2 heart max health for all team members)
     * Also the team the capital belonged to loses all their land (If they have multiple capitals they lose a portion of their land: 2 capitals = 1/2 lost, 3 capitals = 1/3 lost etc.)
     * If a team reclaims their capital their hearts return to normal
     *
     * More territory = More hearts (Cap: +10 max hearts)
     * Less territory = Less hearts (Cap: -9.5 max hearts)
     */

    @Override
    public void onEnable() {
        // Plugin startup logic

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
