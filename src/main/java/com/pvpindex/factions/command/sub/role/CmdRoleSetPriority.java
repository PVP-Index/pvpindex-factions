package com.pvpindex.factions.command.sub.role;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import org.bukkit.entity.Player;

/** {@code /f role setpriority <role> <priority>}. */
public final class CmdRoleSetPriority extends FactionCommand {

    private final FactionService factionService;

    public CmdRoleSetPriority(final FactionService factionService) {
        super("setpriority");
        setPermission("factions.cmd.role.edit");
        setDescription("Set role priority.");
        setRequiredArgs("<role>", "<priority>");
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
        if (factionService.setRolePriority(actor.getUniqueId(), ctx.arg(0), priority)) {
            MsgUtil.sendKey(actor, "custom.role.priority-success",
                "<green>Set role <white>{name}<green> priority to <white>{priority}<green>.",
                "name", ctx.arg(0), "priority", String.valueOf(priority));
            return;
        }
        MsgUtil.sendKey(actor, "custom.role.priority-failed", "<red>Could not update that role priority.");
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (!ctx.isPlayer()) {
            return List.of();
        }
        if (argIndex == 0) {
            return factionService.listRoles(((Player) ctx.getSender()).getUniqueId()).stream().map(RankModel::getName).toList();
        }
        return List.of();
    }
}
