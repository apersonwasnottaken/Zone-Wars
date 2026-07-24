package com.rift.zoneWars;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEvent;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class CommandRegistration {

    private final LifecycleEventManager lifecycleEventManager;
    public CommandRegistration(LifecycleEventManager<Plugin> lifecycleEventManager) {
        this.lifecycleEventManager = lifecycleEventManager;
    }

    public void registerCommand(LifecycleEventManager<Plugin> lifecycleEventManager) {
        lifecycleEventManager.registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralCommandNode<CommandSourceStack> buildCommand = Commands.literal("zonewars")
                    .then(opBranch("get_current_zone"))
                    .build();
            commands.registrar().register(buildCommand, "Commands for Zone Wars gimmick", List.of("zw", "zwars"));
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> opBranch(String name) {
        return Commands.literal(name).requires(source -> source.getSender().isOp());
    }
}
