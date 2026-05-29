package com.pvpindex.factions.command.sub.role;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import org.bukkit.entity.Player;

/** {@code /f role delete <role>}. */
public final class CmdRoleDelete extends FactionCommand {

    private final FactionService factionService;

    public CmdRoleDelete(final FactionService factionService) {
        super("delete");
        setPermission("factions.cmd.role.delete");
        setDescription("Delete a custom role.");
        setRequiredArgs("<role>");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player actor = (Player) ctx.getSender();
        if (!CommandGuards.requireOfficerOrAbove(actor, factionService)) {
            return;
        }
        if (factionService.deleteRole(actor.getUniqueId(), ctx.arg(0))) {
            MsgUtil.sendKey(actor, "custom.role.delete-success",
                "<yellow>Deleted role <white>{name}<yellow>.",
                "name", ctx.arg(0));
            return;
        }
        MsgUtil.sendKey(actor, "custom.role.delete-failed", "<red>Could not delete that role.");
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
