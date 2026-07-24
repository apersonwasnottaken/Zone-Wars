package com.rift.zoneWars;

import org.bukkit.World;
import org.json.JSONArray;
import org.json.JSONObject;

public class Zones {
    // Zone size: 2x2 chunks

    private final PluginData pluginData;

    public Zones(PluginData pluginData) {
        this.pluginData = pluginData;
    }

    public void generateZones(int borderRadius, int teams, World world) {
        JSONArray zoneData = new JSONArray();
        for (int x = ((-borderRadius / 16 - 1) / 2) * 2; x < borderRadius / 16 + 1; x += 2) {
            for (int z = ((-borderRadius / 16 - 1) / 2) * 2; z < borderRadius / 16; z += 2) {
                JSONObject zone = new JSONObject();
                zone.put("world", world.getName());
                zone.put("chunk_region_x", x);
                zone.put("chunk_region_z", z);
                if (x >= -2 && x < 2 && z >= -2 && z < 2) { // Needs testing
                    zone.put("team", -1);
                }
                else {
                    switch (teams) {
                        case 2:
                            if (x < 0) {
                                zone.put("team", 0);
                            }
                            else {
                                zone.put("team", 1);
                            }
                            break;
                        case 3:
                            break;
                        case 4:
                            if (x < 0) {
                                if (z < 0) {
                                    zone.put("team", 0);
                                }
                                else {
                                    zone.put("team", 1);
                                }
                            }
                            else {
                                if (z < 0) {
                                    zone.put("team", 2);
                                }
                                else {
                                    zone.put("team", 3);
                                }
                            }
                            break;
                        case 5:
                            break;
                        case 6:
                            break;
                    }
                }
                zone.put("capital", false);
                zoneData.put(zone);
            }
        }
    }
}
