package com.rift.zoneWars;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.rift.zoneWars.zone.Zones;
import com.rift.zoneWars.zone.claimZone.ClaimZoneEventManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;

import java.util.List;

public class CommandRegistration {

    private final ZoneWars plugin;
    private ComponentLogger logger;
    private final LifecycleEventManager lifecycleEventManager;
    private final Zones zones;
    private final Teams teams;
    private final PluginData pluginData;
    private final ClaimZoneEventManager claimZoneEventManager;

    public CommandRegistration(ZoneWars plugin, LifecycleEventManager<Plugin> lifecycleEventManager, Zones zones, Teams teams, PluginData pluginData, ClaimZoneEventManager claimZoneEventManager) {
        this.plugin = plugin;
        this.lifecycleEventManager = lifecycleEventManager;
        this.logger = plugin.getComponentLogger();
        this.zones = zones;
        this.teams = teams;
        this.pluginData = pluginData;
        this.claimZoneEventManager = claimZoneEventManager;
    }

    public void registerCommand(LifecycleEventManager<Plugin> lifecycleEventManager) {
        lifecycleEventManager.registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralCommandNode<CommandSourceStack> buildCommand = Commands.literal("zonewars")
                    .then(opBranch("get_current_zone")
                            .executes(ctx -> {
                                if (ctx.getSource().getExecutor() == null) {
                                    logger.warn("Command must be executed by a player!");
                                    return -1;
                                }
                                JSONObject zone = zones.findTerritoryFromLocation(ctx.getSource().getLocation());
                                ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize(String.format("""
<yellow><bold>CURRENT ZONE INFO</bold></yellow>
Origin chunk: (<light_purple>%d</light_purple>, <light_purple>%d</light_purple>)
From (<light_purple>%d</light_purple>, <light_purple>%d</light_purple>) to (<light_purple>%d</light_purple>, <light_purple>%d</light_purple>)
Occupied by team <aqua>%d</aqua>
Is team capital? %s
""", zone.getInt("chunk_region_x"), zone.getInt("chunk_region_z"), zone.getInt("chunk_region_x") * 16, zone.getInt("chunk_region_x") * 16 + 31, zone.getInt("chunk_region_z") * 16, zone.getInt("chunk_region_x") * 16 + 31, zone.getInt("team"), zone.getBoolean("capital") ? "<green>Yes</green>" : "<red>No</red>")));
                                return Command.SINGLE_SUCCESS;
                            })
                    )
                    .then(opBranch("generate_zones")
                            .executes(ctx -> {
                                zones.generateZones(pluginData.getTerritoriesConfig().length(), plugin.getServer().getWorld("world"));
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(opBranch("start_territory_claim")
                            .then(Commands.argument("invading_team_index", IntegerArgumentType.integer(0)))
                            .then(Commands.argument("defending_team_index", IntegerArgumentType.integer(0)))
                            .executes(ctx -> {
                                if (ctx.getSource().getExecutor() == null) {
                                    logger.warn("Command must be executed by a player!");
                                    return -1;
                                }
                                claimZoneEventManager.startNewClaim(teams, ctx.getArgument("invading_team_index", int.class), ctx.getArgument("defending_team_index", int.class), zones.findTerritoryFromLocation(ctx.getSource().getLocation()));
                                return Command.SINGLE_SUCCESS;
                            })
                    )
                    .then(opBranch("set_capital_mode_for_territory")
                            .then(Commands.argument("value", BoolArgumentType.bool()))
                            .executes(ctx -> {
                                if (ctx.getSource().getExecutor() == null) {
                                    logger.warn("Command must be executed by a player!");
                                    return -1;
                                }
                                // zones.toggleTerritoryCapital(zones.findTerritoryFromLocation(ctx.getSource().getLocation()));
                                return Command.SINGLE_SUCCESS;
                            })
                    )
                    .build();
            commands.registrar().register(buildCommand, "Commands for Zone Wars gimmick", List.of("zw", "zwars"));
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> opBranch(String name) {
        return Commands.literal(name).requires(source -> source.getSender().isOp());
    }
}
