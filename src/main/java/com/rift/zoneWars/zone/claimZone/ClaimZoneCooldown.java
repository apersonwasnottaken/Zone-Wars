package com.rift.zoneWars.zone.claimZone;

import com.rift.zoneWars.Teams;
import com.rift.zoneWars.ZoneWars;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class ClaimZoneCooldown {

    private int cooldown;
    private int affectedTeamIndex;
    private Teams teams;
    private ZoneWars plugin;

    public ClaimZoneCooldown(ZoneWars plugin, Teams teams, int affectedTeamIndex) {
        this.plugin = plugin;
        this.teams = teams;
        this.affectedTeamIndex = affectedTeamIndex;
    }

    public void startCooldown() {
        Timer timer = new Timer();
        cooldown = 120;
        TimerTask task = new TimerTask(){
            @Override
            public void run(){
                if (cooldown > 0) {
                    teams.getTeamMembers(affectedTeamIndex).forEach(obj -> {
                        Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).get("uuid").toString()));
                        if (player != null && player.isOnline()) {
                            player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>You must wait " + cooldown + " seconds before claiming another piece of territory!"));
                        }
                    });
                    cooldown--;
                } else {
                    teams.getTeamMembers(affectedTeamIndex).forEach(obj -> {
                        Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).get("uuid").toString()));
                        if (player != null && player.isOnline()) {
                            player.sendActionBar(MiniMessage.miniMessage().deserialize("<green>You may claim territory again."));
                        }
                    });
                    timer.cancel();
                }
            }
        };
        timer.scheduleAtFixedRate(task,0,1000);
    }
    public boolean checkCooldown() {
        return cooldown == 0;
    }
}
