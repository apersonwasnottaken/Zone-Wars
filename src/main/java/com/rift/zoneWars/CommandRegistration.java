package com.rift.zoneWars;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.rift.zoneWars.zone.Zones;
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

    public CommandRegistration(ZoneWars plugin, LifecycleEventManager<Plugin> lifecycleEventManager, Zones zones) {
        this.plugin = plugin;
        this.lifecycleEventManager = lifecycleEventManager;
        this.logger = plugin.getComponentLogger();
        this.zones = zones;
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
                    .build();
            commands.registrar().register(buildCommand, "Commands for Zone Wars gimmick", List.of("zw", "zwars"));
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> opBranch(String name) {
        return Commands.literal(name).requires(source -> source.getSender().isOp());
    }
}
