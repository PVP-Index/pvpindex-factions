package com.pvpindex.factions.command.sub.role;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/** {@code /f role assign <player> <role>}. */
public final class CmdRoleAssign extends FactionCommand {

    private final FactionService factionService;

    public CmdRoleAssign(final FactionService factionService) {
        super("assign");
        setPermission("factions.cmd.role.assign");
        setDescription("Assign a role to a faction member.");
        setRequiredArgs("<player>", "<role>");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player actor = (Player) ctx.getSender();
        if (!CommandGuards.requireOfficerOrAbove(actor, factionService)) {
            return;
        }
        final OfflinePlayer target = Bukkit.getOfflinePlayer(ctx.arg(0));
        if (factionService.assignRole(actor.getUniqueId(), target.getUniqueId(), ctx.arg(1))) {
            MsgUtil.sendKey(actor, "custom.role.assign-success",
                "<green>Assigned role <white>{role}<green> to <white>{player}<green>.",
                "role", ctx.arg(1),
                "player", ctx.arg(0));
            return;
        }
        MsgUtil.sendKey(actor, "custom.role.assign-failed", "<red>Could not assign that role.");
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (!ctx.isPlayer()) {
            return List.of();
        }
        if (argIndex == 0) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (argIndex == 1) {
            return factionService.listRoles(((Player) ctx.getSender()).getUniqueId()).stream().map(RankModel::getName).toList();
        }
        return List.of();
    }
}
