package com.gyvex.pvpindex.factions.command;

import com.gyvex.pvpindex.factions.config.FactionsConfig;
import com.gyvex.pvpindex.factions.data.Repositories;
import com.gyvex.pvpindex.factions.registry.CommandRegistry;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Root executor for {@code /fa}. */
public final class AdminCommandExecutor implements CommandExecutor {

    private final Plugin plugin;
    private final CommandRegistry commandRegistry;
    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;

    public AdminCommandExecutor(
            final Plugin plugin,
            final CommandRegistry commandRegistry,
            final Repositories repos,
            final FactionsConfig config,
            final Logger logger) {
        this.plugin = plugin;
        this.commandRegistry = commandRegistry;
        this.repos = repos;
        this.config = config;
        this.logger = logger;
    }

    @Override
    public boolean onCommand(
            final @NotNull CommandSender sender,
            final @NotNull Command command,
            final @NotNull String label,
            final @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        final FactionCommand cmd = commandRegistry.get(args[0].toLowerCase()).orElse(null);
        if (cmd == null) {
            sender.sendMessage(MsgUtil.unknownCommand(args[0]));
            return true;
        }
        final List<String> subArgs = args.length > 1
            ? Arrays.asList(args).subList(1, args.length)
            : List.of();
        cmd.execute(new CommandContext(plugin, sender, subArgs, repos, config, logger));
        return true;
    }

    private void sendHelp(final CommandSender sender) {
        commandRegistry.get("help")
            .ifPresent(help -> help.execute(
                new CommandContext(plugin, sender, List.of(), repos, config, logger)));
    }
}

