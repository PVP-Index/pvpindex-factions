package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import org.bukkit.entity.Player;

/** {@code /f leave} — Leave your current faction. */
public final class CmdLeave extends FactionCommand {

    private final FactionService factionService;

    public CmdLeave(final FactionService factionService) {
        super("leave");
        setPermission("factions.cmd.leave");
        setDescription("Leave your current faction.");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> factionOpt = factionService.getFactionByPlayer(player.getUniqueId());
        if (factionOpt.isEmpty()) {
            MsgUtil.send(player, "<red>You are not in a faction.");
            return;
        }
        if (factionOpt.get().isOwner(player.getUniqueId())) {
            MsgUtil.send(player,
                "<red>You are the owner. Transfer ownership or disband with /f disband.");
            return;
        }
        if (factionService.removeMember(factionOpt.get().getId(), player.getUniqueId())) {
            MsgUtil.send(player, "<yellow>You left <white>" + factionOpt.get().getName() + "<yellow>.");
        } else {
            MsgUtil.send(player, "<red>Failed to leave faction.");
        }
    }
}
