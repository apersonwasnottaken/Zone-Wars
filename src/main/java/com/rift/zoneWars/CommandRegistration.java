package com.rift.zoneWars;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.rift.zoneWars.zone.Zones;
import com.rift.zoneWars.zone.claimZone.ClaimZoneEventManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.units.qual.N;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class CommandRegistration {

    private final ZoneWars plugin;
    private ComponentLogger logger;
    private final LifecycleEventManager lifecycleEventManager;
    private final Zones zones;
    private final Teams teams;
    private final PluginData pluginData;
    private final ClaimZoneEventManager claimZoneEventManager;
    private final MoveCapitalCooldownManager moveCapitalCooldownManager;

    public CommandRegistration(ZoneWars plugin, LifecycleEventManager<Plugin> lifecycleEventManager, Zones zones, Teams teams, PluginData pluginData, ClaimZoneEventManager claimZoneEventManager, MoveCapitalCooldownManager moveCapitalCooldownManager) {
        this.plugin = plugin;
        this.lifecycleEventManager = lifecycleEventManager;
        this.logger = plugin.getComponentLogger();
        this.zones = zones;
        this.teams = teams;
        this.pluginData = pluginData;
        this.claimZoneEventManager = claimZoneEventManager;
        this.moveCapitalCooldownManager = moveCapitalCooldownManager;
    }

    public void registerCommand(LifecycleEventManager<Plugin> lifecycleEventManager) {
        lifecycleEventManager.registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralCommandNode<CommandSourceStack> buildCommand = Commands.literal("zonewars")
                    .then(opBranch("get_current_zone")
                            .executes(ctx -> {
                                if (!(ctx.getSource().getExecutor() instanceof Player)) {
                                    logger.warn("Command must be executed by a player!");
                                    return -1;
                                }
                                JSONObject zone = zones.findTerritoryFromLocation(ctx.getSource().getLocation());
                                ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize(String.format("""
<yellow><bold>CURRENT ZONE INFO</bold></yellow>
Origin chunk: (<light_purple>%d</light_purple>, <light_purple>%d</light_purple>)
From (<light_purple>%d</light_purple>, <light_purple>%d</light_purple>) to (<light_purple>%d</light_purple>, <light_purple>%d</light_purple>)
Occupied by team <click:copy_to_clipboard:%s><hover:show_text:'<yellow>Click to copy to clipboard!'><aqua>%s</aqua></hover></click>
Is team capital? %s
""",
                                        zone.getInt("chunk_region_x"),
                                        zone.getInt("chunk_region_z"),
                                        zone.getInt("chunk_region_x") * 16,
                                        zone.getInt("chunk_region_z") * 16,
                                        zone.getInt("chunk_region_x") * 16 + 31,
                                        zone.getInt("chunk_region_z") * 16 + 31,
                                        zone.get("team").toString(),
                                        zone.get("team").toString(),
                                        zone.getBoolean("capital") ? "<green>Yes</green>" : "<red>No</red>")));
                                return Command.SINGLE_SUCCESS;
                            })
                    )
                    .then(opBranch("generate_zones")
                            .executes(ctx -> {
                                if (!(ctx.getSource().getExecutor() instanceof Player)) {
                                    logger.warn("Command must be executed by a player!");
                                    return -1;
                                }
                                zones.generateZones(pluginData.getTeamsConfig().length(), plugin.getServer().getWorld("world"), (Player) ctx.getSource().getExecutor());
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(opBranch("start_territory_claim")
                            .then(Commands.argument("invading_team", ArgumentTypes.uuid())
                                    .suggests((ctx, builder) -> {
                                        teams.getAllTeamUUIDs().stream()
                                                .map(UUID::toString)
                                                .forEach(builder::suggest);
                                        return builder.buildFuture();
                                    })
                                    .then(Commands.argument("defending_team", ArgumentTypes.uuid())
                                            .suggests((ctx, builder) -> {
                                                teams.getAllTeamUUIDs().stream()
                                                        .map(UUID::toString)
                                                        .forEach(builder::suggest);
                                                return builder.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                if (ctx.getSource().getExecutor() == null) {
                                                    logger.warn("Command must be executed by a player!");
                                                    return -1;
                                                }
                                                claimZoneEventManager.startNewClaim(teams, teams.getTeamIndexFromUUID(ctx.getArgument("invading_team", UUID.class)), teams.getTeamIndexFromUUID(ctx.getArgument("defending_team", UUID.class)), zones.findTerritoryFromLocation(ctx.getSource().getLocation()), ctx.getSource().getExecutor());
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                    )
                    .then(opBranch("create_capital")
                            .then(Commands.argument("team", ArgumentTypes.uuid())
                                    .suggests((ctx, builder) -> {
                                        teams.getAllTeamUUIDs().stream()
                                                .map(UUID::toString)
                                                .forEach(builder::suggest);
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize(zones.createCapital(teams.getTeamIndexFromUUID(ctx.getArgument("team", UUID.class))) ? "<green>Capital creation successful!</green>" : "<red>Capital creation unsuccessful! This may be because a capital zone already exists for the team.</red>"));
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                    )
                    .then(opBranch("remove_capital")
                            .then(Commands.argument("team", ArgumentTypes.uuid())
                                    .suggests((ctx, builder) -> {
                                        teams.getAllTeamUUIDs().stream()
                                                .map(UUID::toString)
                                                .forEach(builder::suggest);
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize(zones.removeCapital(teams.getTeamIndexFromUUID(ctx.getArgument("team", UUID.class))) ? "<green>Capital removal successful!</green>" : "<red>Capital removal unsuccessful! This may be because the team does not have a capital.</red>"));
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                    )
                    .then(opBranch("teleport_to_capital")
                            .then(Commands.argument("team", ArgumentTypes.uuid())
                                    .suggests((ctx, builder) -> {
                                        teams.getAllTeamUUIDs().stream()
                                                .map(UUID::toString)
                                                .forEach(builder::suggest);
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        JSONArray capitalZones = zones.getCapitalTerritories(teams.getTeamIndexFromUUID(ctx.getArgument("team", UUID.class)));
                                        if (!(ctx.getSource().getExecutor() instanceof Player)) {
                                            logger.warn("Command must be executed by a player!");
                                            return 1;
                                        }
                                        if (capitalZones.isEmpty()) {
                                            ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize("<red>Capital territory not found for team </red><aqua>" + ctx.getArgument("team", UUID.class) + "</aqua><red>.</red>"));
                                            return 1;
                                        }
                                        JSONObject zone = capitalZones.getJSONObject(0);
                                        ctx.getSource().getExecutor().teleportAsync(new Location(plugin.getServer().getWorld("world"), zone.getInt("chunk_region_x") * 16, plugin.getServer().getWorld("world").getHighestBlockYAt(zone.getInt("chunk_region_x") * 16, zone.getInt("chunk_region_z") * 16), zone.getInt("chunk_region_z") * 16));
                                        ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize("<green>Successfully teleported!</green>"));
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                    )
                    .then(opBranch("create_team")
                            .then(Commands.argument("team_name", StringArgumentType.string())
                                    .then(Commands.argument("team_color", ArgumentTypes.hexColor())
                                            .executes(ctx -> {
                                                teams.addTeam(ctx.getArgument("team_name", String.class), ctx.getArgument("team_color", TextColor.class).value());
                                                ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize("<green>Team created successfully!"));
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                    )
                    .then(opBranch("get_team_info")
                            .then(Commands.argument("team_id", ArgumentTypes.uuid())
                                    .suggests((ctx, builder) -> {
                                        teams.getAllTeamUUIDs().stream()
                                                .map(UUID::toString)
                                                .forEach(builder::suggest);
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        final UUID teamUUID = ctx.getArgument("team_id", UUID.class);
                                        ArrayList<String> teamMemberNames = new ArrayList<>();
                                        teams.getTeamMembers(teams.getTeamIndexFromUUID(teamUUID)).forEach(obj -> {
                                            teamMemberNames.add(((JSONObject) obj).getString("username"));
                                        });
                                        String teamColor = "0".repeat(6 - Integer.toHexString(teams.getTeamColor(teams.getTeamIndexFromUUID(teamUUID))).length()) + Integer.toHexString(teams.getTeamColor(teams.getTeamIndexFromUUID(teamUUID)));
                                        ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize(String.format("""
                                                <green>TEAM INFO</green>
                                                Team UUID: <hover:show_text:'<yellow>Click to copy to clipboard!'><click:copy_to_clipboard:%s>%s</click></hover>
                                                Team Name: %s
                                                Team Color: <#%s>#%s<gray>
                                                Team Members: %s
                                                Amount of Territories: %d
                                                """, teamUUID, teamUUID, teams.getTeamName(teams.getTeamIndexFromUUID(teamUUID)), teamColor, teamColor, teamMemberNames.isEmpty() ? "<italic>None!</italic>" : String.join(", ", teamMemberNames), zones.getTeamTerritories(teams.getTeamIndexFromUUID(teamUUID)).length())));
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                    )
                    .then(opBranch("list_teams")
                            .executes(ctx -> {
                                StringBuilder message = new StringBuilder("""
                                        <yellow>List of Teams</yellow>
                                        """);
                                for (Object obj : pluginData.getTeamsConfig()) {
                                    message.append("<#").append(Integer.toHexString(((JSONObject) obj).getInt("color"))).append("><hover:show_text:\"<aqua>Click to view team info!\"><click:run_command:/zw get_team_info ").append(((JSONObject) obj).getString("id")).append(">").append(((JSONObject) obj).getString("name")).append("</click>\n");
                                }
                                ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize(message.toString()));
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(opBranch("team")
                            .then(Commands.argument("team_id", StringArgumentType.string())
                                    .suggests((ctx, builder) -> {
                                        teams.getAllTeamUUIDs().stream()
                                                .map(UUID::toString)
                                                .forEach(builder::suggest);
                                        return builder.buildFuture();
                                    })
                                    .then(Commands.literal("delete")
                                            .executes(ctx -> {
                                                teams.deleteTeam(UUID.fromString(ctx.getArgument("team_id", String.class)));
                                                ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize("Team deleted!"));
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                                    .then(Commands.literal("add_team_member")
                                            .then(Commands.argument("team_member", ArgumentTypes.playerProfiles())
                                                    .executes(ctx -> {
                                                        var selector = ctx.getArgument("team_member", PlayerProfileListResolver.class);
                                                        Collection<PlayerProfile> profiles = selector.resolve(ctx.getSource());
                                                        if (profiles.isEmpty()) {
                                                            return 0;
                                                        }
                                                        PlayerProfile profile = profiles.stream().findFirst().get();
                                                        UUID teamId = UUID.fromString(ctx.getArgument("team_id", String.class));
                                                        teams.addMemberToTeam(teams.getTeamIndexFromUUID(teamId), profile);
                                                        ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize("<green>Team member added successfully!"));
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                            )
                                    )
                                    .then(Commands.literal("remove_team_member")
                                            .then(Commands.argument("team_member", ArgumentTypes.playerProfiles())
                                                    .executes(ctx -> {
                                                        var selector = ctx.getArgument("team_member", PlayerProfileListResolver.class);
                                                        Collection<PlayerProfile> profiles = selector.resolve(ctx.getSource());
                                                        if (profiles.isEmpty()) {
                                                            return 0;
                                                        }
                                                        PlayerProfile profile = profiles.stream().findFirst().get();
                                                        UUID teamId = UUID.fromString(ctx.getArgument("team_id", String.class));
                                                        teams.removeMemberFromTeam(teams.getTeamIndexFromUUID(teamId), profile);
                                                        ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize("<green>Team member removed successfully!"));
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                            )
                                    )
                                    .then(Commands.literal("change_team_name")
                                            .then(Commands.argument("team_name", StringArgumentType.string())
                                                    .executes(ctx -> {
                                                        teams.setTeamName(teams.getTeamIndexFromUUID(UUID.fromString(ctx.getArgument("team_id", String.class))), ctx.getArgument("team_name", String.class));
                                                        ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize("<green>Team name changed!"));
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                            )
                                    )
                                    .then(Commands.literal("change_team_color")
                                            .then(Commands.argument("team_color", ArgumentTypes.hexColor())
                                                    .executes(ctx -> {
                                                        teams.setTeamColor(teams.getTeamIndexFromUUID(UUID.fromString(ctx.getArgument("team_id", String.class))), ctx.getArgument("team_color", TextColor.class).value());
                                                        ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize("<green>Team color changed!"));
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                            )
                                    )
                            )
                    )
                    .then(opBranch("toggle_claiming")
                            .executes(ctx -> {
                                plugin.getConfig().set("claiming_enabled", !plugin.getConfig().getBoolean("claiming_enabled"));
                                plugin.saveConfig();
                                ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize("Toggled claiming state to <bold>" + (plugin.getConfig().getBoolean("claiming_enabled") ? "<green>true" : "<red>false")));
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(opBranch("reload")
                            .executes(ctx -> {
                                teams.updateTeams();
                                zones.updateTerritories();
                                plugin.getMainGameLoop().updateTeamCache();
                                claimZoneEventManager.clearAllEvents();
                                ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize("<green>Plugin reloaded successfully!"));
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(opBranch("delete_zones")
                            .executes(ctx -> {
                                zones.resetZones();
                                ctx.getSource().getExecutor().sendMessage(MiniMessage.miniMessage().deserialize("<green>Zones deleted successfully!"));
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(Commands.literal("toggle_zone_particles")
                            .executes(ctx -> {
                                if (!(ctx.getSource().getExecutor() instanceof Player)) {
                                    logger.warn("Command must be executed by a player!");
                                    return 1;
                                }
                                NamespacedKey particlesKey = new NamespacedKey(plugin, "particles");
                                Player player = (Player) ctx.getSource().getExecutor();
                                if (player.getPersistentDataContainer().has(particlesKey) && player.getPersistentDataContainer().get(particlesKey, PersistentDataType.BOOLEAN) != null) {
                                    player.getPersistentDataContainer().set(particlesKey, PersistentDataType.BOOLEAN, !player.getPersistentDataContainer().get(particlesKey, PersistentDataType.BOOLEAN));
                                }
                                else {
                                    player.getPersistentDataContainer().set(particlesKey, PersistentDataType.BOOLEAN, false);
                                }
                                player.sendMessage(MiniMessage.miniMessage().deserialize("Territory particles set to: <bold>" + (player.getPersistentDataContainer().get(particlesKey, PersistentDataType.BOOLEAN) ? "<green>ON" : "<red>OFF")));
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(Commands.literal("start_claim")
                            .executes(ctx -> {
                                if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                    logger.warn("Command must be executed by a player!");
                                    return 1;
                                }
                                claimZoneEventManager.startNewClaim(teams, teams.getTeamIndexFromPlayer(player.getUniqueId()), teams.getTeamIndexFromUUID(UUID.fromString(zones.findTerritoryFromLocation(ctx.getSource().getLocation()).get("team").toString())), zones.findTerritoryFromLocation(ctx.getSource().getLocation()), ctx.getSource().getExecutor());
                                return Command.SINGLE_SUCCESS;
                            }))
                    /*
                    .then(Commands.literal("move_capital")
                            .executes(ctx -> {
                                if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                    logger.warn("Command must be executed by a player!");
                                    return 1;
                                }
                                player = (Player) ctx.getSource().getExecutor();
                                int teamIdx = teams.getTeamIndexFromPlayer(player.getUniqueId());
                                if (moveCapitalCooldownManager.getCooldown(teams.getTeamUUID(teamIdx)) / 1000 < 600) {
                                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You must wait " + Math.floor(moveCapitalCooldownManager.getCooldown(teams.getTeamUUID(teamIdx)) / 1000) + " more seconds!"));
                                    return;
                                }
                                zones.removeCapital(teamIdx);
                                zones.createCapital(teamIdx, player.getLocation());
                                moveCapitalCooldownManager.startCooldown(teams.getTeamUUID(teams.getTeamIndexFromPlayer(player.getUniqueId())));
                                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Moved successfully!"));
                                return Command.SINGLE_SUCCESS;
                            }))

                     */
                    .build();
            commands.registrar().register(buildCommand, "Commands for Zone Wars gimmick", List.of("zw", "zwars"));
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> opBranch(String name) {
        return Commands.literal(name).requires(source -> source.getSender().isOp());
    }
}
