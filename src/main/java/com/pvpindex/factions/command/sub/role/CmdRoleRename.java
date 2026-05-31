package com.pvpindex.factions.command.sub.role;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import org.bukkit.entity.Player;

/** {@code /f role rename <old> <new>}. */
public final class CmdRoleRename extends FactionCommand {

    private final FactionService factionService;

    public CmdRoleRename(final FactionService factionService) {
        super("rename");
        setPermission("factions.cmd.role.edit");
        setDescription("Rename a faction role.");
        setRequiredArgs("<role>", "<newName>");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player actor = (Player) ctx.getSender();
        if (!CommandGuards.requireOfficerOrAbove(actor, factionService)) {
            return;
        }
        if (factionService.renameRole(actor.getUniqueId(), ctx.arg(0), ctx.arg(1))) {
            MsgUtil.sendKey(actor, "custom.role.rename-success",
                "<green>Renamed role <white>{old}<green> to <white>{new}<green>.",
                "old", ctx.arg(0), "new", ctx.arg(1));
            return;
        }
        MsgUtil.sendKey(actor, "custom.role.rename-failed", "<red>Could not rename that role.");
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
