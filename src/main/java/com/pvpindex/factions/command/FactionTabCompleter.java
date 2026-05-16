package com.pvpindex.factions.command;

import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.registry.CommandRegistry;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.api.TeamsSubcommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tab-completer for {@code /f}.
 *
 * <p>At argument position 0, accessible top-level command names are listed.
 * At argument position 1+, the call is forwarded to the matched
 * {@link FactionCommand#tabComplete(CommandContext)}, which handles both
 * child-name completion and contextual argument completion (e.g. faction
 * names, online player names, warp names).
 */
public final class FactionTabCompleter implements TabCompleter {

    private final Plugin plugin;
    private final CommandRegistry commandRegistry;
    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;
    private final boolean teamsApiEnabled;

    public FactionTabCompleter(
            final Plugin plugin,
            final CommandRegistry commandRegistry,
            final Repositories repos,
            final FactionsConfig config,
            final Logger logger,
            final boolean teamsApiEnabled) {
        this.plugin = plugin;
        this.commandRegistry = commandRegistry;
        this.repos = repos;
        this.config = config;
        this.logger = logger;
        this.teamsApiEnabled = teamsApiEnabled;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            final @NotNull CommandSender sender,
            final @NotNull Command command,
            final @NotNull String alias,
            final @NotNull String[] args) {
        if (args.length == 0) {
            return List.of();
        }
        if (args.length == 1) {
            final List<String> completions = new ArrayList<>(
                commandRegistry.completionNames(args[0].toLowerCase(), sender));
            if (teamsApiEnabled) {
                final String partial = args[0].toLowerCase();
                for (final TeamsSubcommand sub : TeamsAPI.getSubcommands()) {
                    final String perm = sub.getPermission();
                    if (perm == null || sender.hasPermission(perm)) {
                        if (sub.getName().toLowerCase().startsWith(partial)) {
                            completions.add(sub.getName());
                        }
                    }
                }
            }
            return completions;
        }
        // args.length >= 2: route to the matched top-level command
        final FactionCommand cmd = commandRegistry.get(args[0].toLowerCase()).orElse(null);
        if (cmd != null) {
            // ctx.getArgs() = args[1..] (everything after the sub-command name)
            final CommandContext ctx = new CommandContext(
                plugin, sender,
                Arrays.asList(args).subList(1, args.length),
                repos, config, logger);
            return cmd.tabComplete(ctx);
        }
        // Check TeamsAPI subcommands for argument-level tab-completion
        if (teamsApiEnabled) {
            for (final TeamsSubcommand sub : TeamsAPI.getSubcommands()) {
                if (sub.getName().equalsIgnoreCase(args[0])) {
                    final String perm = sub.getPermission();
                    if (perm == null || sender.hasPermission(perm)) {
                        return sub.tabComplete(sender, args);
                    }
                    return List.of();
                }
            }
        }
        return List.of();
    }
}
