package com.pvpindex.factions.command;

import com.pvpindex.factions.util.MsgUtil;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.api.TeamsSubcommand;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * TeamsAPI-backed implementation of {@link TeamsCommandBridge}.
 *
 * <p><strong>Never reference this class directly from bootstrap or any other code
 * that is loaded unconditionally.</strong> It must only be instantiated via
 * {@code Class.forName("com.pvpindex.factions.command.TeamsCommandBridgeImpl")}
 * after TeamsAPI has been confirmed present on the classpath. Loading this class
 * when TeamsAPI is absent will throw {@link NoClassDefFoundError}.
 */
public final class TeamsCommandBridgeImpl implements TeamsCommandBridge {

    @Override
    public boolean dispatch(final CommandSender sender, final String[] args) {
        for (final TeamsSubcommand sub : TeamsAPI.getSubcommands()) {
            if (sub.getName().equalsIgnoreCase(args[0])) {
                final String perm = sub.getPermission();
                if (perm != null && !sender.hasPermission(perm)) {
                    MsgUtil.send(sender, MsgUtil.message(
                        sender,
                        "general.no-permission",
                        "<red>You do not have permission to use this command."));
                    return true;
                }
                if (!sub.execute(sender, args)) {
                    MsgUtil.sendKey(sender, "general.invalid-args", "<red>Usage: {usage}", "usage", sub.getUsage());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> completeSubcommandNames(final CommandSender sender, final String partial) {
        final List<String> result = new ArrayList<>();
        for (final TeamsSubcommand sub : TeamsAPI.getSubcommands()) {
            final String perm = sub.getPermission();
            if (perm == null || sender.hasPermission(perm)) {
                if (sub.getName().toLowerCase().startsWith(partial)) {
                    result.add(sub.getName());
                }
            }
        }
        return result;
    }

    @Override
    public List<String> completeArgs(final CommandSender sender, final String[] args) {
        for (final TeamsSubcommand sub : TeamsAPI.getSubcommands()) {
            if (sub.getName().equalsIgnoreCase(args[0])) {
                final String perm = sub.getPermission();
                if (perm == null || sender.hasPermission(perm)) {
                    return sub.tabComplete(sender, args);
                }
                return List.of();
            }
        }
        return List.of();
    }
}
