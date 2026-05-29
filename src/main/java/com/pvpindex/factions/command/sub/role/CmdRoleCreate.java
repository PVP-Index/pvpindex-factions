package com.pvpindex.factions.command.sub.role;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import org.bukkit.entity.Player;

/** {@code /f role create <name> <priority> [prefix]}. */
public final class CmdRoleCreate extends FactionCommand {

    private final FactionService factionService;

    public CmdRoleCreate(final FactionService factionService) {
        super("create");
        setPermission("factions.cmd.role.create");
        setDescription("Create a custom faction role.");
        setRequiredArgs("<name>", "<priority>");
        setOptionalArgs("[prefix]");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player actor = (Player) ctx.getSender();
        if (!CommandGuards.requireOfficerOrAbove(actor, factionService)) {
            return;
        }
        final int priority;
        try {
            priority = Integer.parseInt(ctx.arg(1));
        } catch (NumberFormatException e) {
            MsgUtil.sendKey(actor, "custom.role.invalid-priority", "<red>Priority must be a number.");
            return;
        }
        final String prefix = ctx.getArgs().size() >= 3 ? ctx.arg(2) : null;
        if (factionService.createRole(actor.getUniqueId(), ctx.arg(0), priority, prefix)) {
            MsgUtil.sendKey(actor, "custom.role.create-success",
                "<green>Created role <white>{name}<green> with priority <white>{priority}<green>.",
                "name", ctx.arg(0),
                "priority", String.valueOf(priority));
            return;
        }
        MsgUtil.sendKey(actor, "custom.role.create-failed", "<red>Could not create that role.");
    }
}
