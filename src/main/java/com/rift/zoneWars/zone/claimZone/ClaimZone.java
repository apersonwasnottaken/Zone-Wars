package com.rift.zoneWars.zone.claimZone;

import com.rift.zoneWars.Teams;
import com.rift.zoneWars.ZoneWars;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.json.JSONObject;

import java.util.UUID;
import java.util.function.Consumer;

public class ClaimZone {

    private final ZoneWars plugin;
    private final UUID eventID;
    private int secondsRemaining;
    private final int invader, defender;
    private BukkitTask timer;
    private final Teams teams;
    private JSONObject zone;
    private Consumer<EventOutcome> endCallback;
    private boolean isEnded = false;

    public ClaimZone(ZoneWars plugin, Teams teams, int invader, int defender, JSONObject zone) {
        this.plugin = plugin;
        this.eventID = UUID.randomUUID();
        this.secondsRemaining = 120;
        this.invader = invader;
        this.defender = defender;
        this.teams = teams;
        this.zone = zone;
    }

    public JSONObject getZone() {
        return zone;
    }

    public void startClaimZone(Consumer<EventOutcome> onEndCallback) {
        if (zone.getBoolean("capital")) {
            secondsRemaining = 60 * 15;
        }
        else {
            secondsRemaining = 120;
        }
        this.endCallback = onEndCallback;
        this.timer = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (secondsRemaining <= 0) {
                complete(EventOutcome.SUCCESS);
                return;
            }

            teams.getTeamMembers(invader).forEach(obj -> {
                Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).getString("uuid")));
                if (player != null && player.isOnline()) {
                    player.sendActionBar(net.kyori.adventure.text.Component.text(
                            "You must hold the territory for " + secondsRemaining + " more seconds!"
                    ));
                    player.setGlowing(true);
                }
            });

            teams.getTeamMembers(defender).forEach(obj -> {
                Player player = plugin.getServer().getPlayer(UUID.fromString(((JSONObject) obj).getString("uuid")));
                if (player != null && player.isOnline() && secondsRemaining == 120) {
                    player.showTitle(Title.title(MiniMessage.miniMessage().deserialize("<bold><red>Your territory is being invaded!"), MiniMessage.miniMessage().deserialize("<gray>Go defend it!")));
                    player.sendMessage(MiniMessage.miniMessage().deserialize(String.format("""
The territory at <yellow>(%d, %d)</yellow> is being raided by another team!
""", zone.getInt("chunk_region_x") * 16, zone.getInt("chunk_region_z") * 16)));
                }
            });
            secondsRemaining--;
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
