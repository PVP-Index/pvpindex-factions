package com.pvpindex.factions.command.sub.admin;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.registry.CommandRegistry;
import com.pvpindex.factions.util.MsgUtil;

/** {@code /fa help} */
public final class CmdAdminHelp extends FactionCommand {

    private final CommandRegistry commandRegistry;

    public CmdAdminHelp(final CommandRegistry commandRegistry) {
        super("help");
        setPermission("factions.admin");
        setDescription("List admin commands.");
        this.commandRegistry = commandRegistry;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        MsgUtil.send(ctx.getSender(), "<gold>== Factions Admin ==");
        for (final FactionCommand cmd : commandRegistry.getAll()) {
            if (cmd.getPermission() != null && !ctx.getSender().hasPermission(cmd.getPermission())) {
                continue;
            }
            ctx.getSender().sendMessage(MsgUtil.helpEntry("/fa " + cmd.getName(), cmd.getDescription()));
        }
    }
}

