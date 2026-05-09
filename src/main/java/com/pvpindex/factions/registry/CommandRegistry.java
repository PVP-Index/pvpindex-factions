package com.pvpindex.factions.registry;

import com.pvpindex.factions.command.FactionCommand;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;

/**
 * Registry for top-level {@link FactionCommand} instances, keyed by
 * lower-case name.
 *
 * <p>Extends the generic {@link Registry} and adds a convenience
 * {@link #register(FactionCommand)} overload that uses the command's own name
 * as the key, plus a {@link #getAll()} accessor.
 */
public class CommandRegistry extends Registry<String, FactionCommand> {

    /**
     * Register a command under its primary name and all of its aliases.
     *
     * @param cmd the command to register
     */
    public void register(final FactionCommand cmd) {
        register(cmd.getName().toLowerCase(), cmd);
        for (final String alias : cmd.getAliases()) {
            register(alias.toLowerCase(), cmd);
        }
    }

    /**
     * Return an ordered, deduplicated view of all registered commands.
     * Each command appears exactly once regardless of how many aliases it has.
     *
     * @return immutable list of unique {@link FactionCommand} instances in registration order
     */
    public Collection<FactionCommand> getAll() {
        return super.store().values().stream()
            .distinct()
            .toList();
    }

    /**
     * Return all command names (primary names and aliases) that start with
     * {@code partial} and are accessible by {@code sender}.
     *
     * <p>Used by {@link com.pvpindex.factions.command.FactionTabCompleter} to
     * provide alias-aware tab-completion at argument position 0.
     *
     * @param partial case-insensitive prefix to filter by
     * @param sender  the command sender (used for permission check)
     * @return filtered list of completion strings
     */
    public List<String> completionNames(final String partial, final CommandSender sender) {
        return super.store().entrySet().stream()
            .filter(e -> e.getKey().startsWith(partial))
            .filter(e -> e.getValue().getPermission() == null
                || sender.hasPermission(e.getValue().getPermission()))
            .map(Map.Entry::getKey)
            .toList();
    }
}
