package com.rift.zoneWars;

import java.util.HashMap;
import java.util.UUID;

public class MoveCapitalCooldownManager {

    private HashMap<UUID, Long> cooldownTeams = HashMap.newHashMap(0);
    private Teams teams;

    public MoveCapitalCooldownManager(Teams teams) {
        this.teams = teams;
    }

    public void startCooldown(UUID teamUUID) {
        cooldownTeams.put(teamUUID, System.currentTimeMillis());
    }

    public Long getCooldown(UUID team) {
        return cooldownTeams.get(team);
    }
}
