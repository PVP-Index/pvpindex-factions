package com.pvpindex.factions.command.sub.role;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import org.bukkit.entity.Player;

/** {@code /f role list}. */
public final class CmdRoleList extends FactionCommand {

    private final FactionService factionService;

    public CmdRoleList(final FactionService factionService) {
        super("list");
        setPermission("factions.cmd.role.list");
        setDescription("List all faction roles.");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player actor = (Player) ctx.getSender();
        if (CommandGuards.requireFaction(actor, factionService).isEmpty()) {
            return;
        }
        final List<RankModel> roles = factionService.listRoles(actor.getUniqueId());
        MsgUtil.sendKey(actor, "custom.role.list-header", "<gold>== Faction Roles ==");
        for (final RankModel role : roles) {
            final String prefix = role.getPrefix() == null ? "-" : role.getPrefix();
            MsgUtil.sendKey(actor, "custom.role.list-entry",
                "<gray>- <white>{name}<gray> | priority <white>{priority}<gray> | prefix <white>{prefix}",
                "name", role.getName(),
                "priority", String.valueOf(role.getPriority()),
                "prefix", prefix
            );
        }
    }
}
