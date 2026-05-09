package com.gyvex.pvpindex.factions.command.sub;

import com.gyvex.pvpindex.factions.command.CommandContext;
import com.gyvex.pvpindex.factions.command.CommandGuards;
import com.gyvex.pvpindex.factions.command.FactionCommand;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** {@code /f kick <player>} — kick a lower-rank member from your faction. */
public final class CmdKick extends FactionCommand {

    private final FactionService factionService;

    public CmdKick(final FactionService factionService) {
        super("kick");
        setPermission("factions.cmd.kick");
        setDescription("Kick a member from your faction.");
        setRequiredArgs("<player>");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player actor = (Player) ctx.getSender();
        if (CommandGuards.requireFaction(actor, factionService).isEmpty()) {
            return;
        }
        if (!CommandGuards.requireOfficerOrAbove(actor, factionService)) {
            return;
        }
        final Player target = Bukkit.getPlayerExact(ctx.arg(0));
        if (target == null) {
            MsgUtil.send(actor, "<red>Player not found or not online.");
            return;
        }
        if (actor.getUniqueId().equals(target.getUniqueId())) {
            MsgUtil.send(actor, "<red>You cannot kick yourself.");
            return;
        }
        if (factionService.isOwner(target.getUniqueId())) {
            MsgUtil.send(actor, "<red>You cannot kick the faction leader.");
            return;
        }
        if (factionService.kickMember(actor.getUniqueId(), target.getUniqueId())) {
            MsgUtil.send(actor, "<yellow>Kicked <white>" + target.getName() + "<yellow>.");
            MsgUtil.send(target, "<red>You were kicked from your faction.");
            return;
        }
        MsgUtil.send(actor, "<red>Could not kick that player.");
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex != 0) {
            return List.of();
        }
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }
}

