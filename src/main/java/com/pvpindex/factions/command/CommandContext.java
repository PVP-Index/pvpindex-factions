package com.pvpindex.factions.command;

import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Immutable context passed to every sub-command.
 */
public final class CommandContext {

    private final Plugin plugin;
    private final CommandSender sender;
    private final List<String> args;
    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;

    public CommandContext(
            final Plugin plugin,
            final CommandSender sender,
            final List<String> args,
            final Repositories repos,
            final FactionsConfig config,
            final Logger logger) {
        this.plugin = plugin;
        this.sender = sender;
        this.args = List.copyOf(args);
        this.repos = repos;
        this.config = config;
        this.logger = logger;
    }

    public Plugin getPlugin() { return plugin; }
    public CommandSender getSender() { return sender; }
    public List<String> getArgs() { return args; }
    public Repositories getRepos() { return repos; }
    public FactionsConfig getConfig() { return config; }
    public Logger getLogger() { return logger; }

    /** @return {@code true} if the sender is an online {@link Player}. */
    public boolean isPlayer() {
        return sender instanceof Player;
    }

    /**
     * Cast sender to {@link Player}, or send an error and return {@code null}.
     *
     * @return Player or {@code null}
     */
    public Player requirePlayer() {
        if (sender instanceof Player player) return player;
        MsgUtil.sendKey(sender, "general.player-only", "<red>This command can only be used by a player.");
        return null;
    }

    /** Shorthand for {@link #getArgs()}.get(index). Returns empty string if out of bounds. */
    public String arg(final int index) {
        return index < args.size() ? args.get(index) : "";
    }

    /**
     * Return a new {@code CommandContext} with the first argument removed.
     *
     * <p>Used by {@link FactionCommand} to forward execution to a child
     * command after consuming {@code args[0]} as the child's name.
     *
     * @return a new context with {@code args.subList(1, args.size())}
     */
    public CommandContext shift() {
        final List<String> shifted = args.isEmpty() ? List.of() : args.subList(1, args.size());
        return new CommandContext(plugin, sender, shifted, repos, config, logger);
    }
}
