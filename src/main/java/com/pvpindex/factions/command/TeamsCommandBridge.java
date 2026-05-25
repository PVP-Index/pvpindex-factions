package com.pvpindex.factions.command;

import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * Isolation bridge for TeamsAPI command dispatch and tab-completion.
 *
 * <p>This interface is loaded unconditionally and carries no TeamsAPI references.
 * The implementation ({@code TeamsCommandBridgeImpl}) directly imports TeamsAPI
 * classes and must only be instantiated via reflection after TeamsAPI has been
 * confirmed present on the classpath — following the same pattern as
 * {@link com.pvpindex.factions.api.TeamsApiRegistrar}.
 */
public interface TeamsCommandBridge {

    /**
     * Attempt to dispatch args to a TeamsAPI subcommand.
     *
     * @param sender the command sender
     * @param args   the full argument array (args[0] = subcommand name)
     * @return {@code true} if a matching subcommand was found and handled
     */
    boolean dispatch(CommandSender sender, String[] args);

    /**
     * Collect TeamsAPI subcommand name completions matching a partial string.
     *
     * @param sender  the command sender (used for permission checks)
     * @param partial the partial string typed so far (lowercase)
     * @return list of matching subcommand names the sender may use
     */
    List<String> completeSubcommandNames(CommandSender sender, String partial);

    /**
     * Collect argument completions for a TeamsAPI subcommand.
     *
     * @param sender the command sender
     * @param args   the full argument array (args[0] = subcommand name)
     * @return argument completions, or an empty list if no match found
     */
    List<String> completeArgs(CommandSender sender, String[] args);
}
