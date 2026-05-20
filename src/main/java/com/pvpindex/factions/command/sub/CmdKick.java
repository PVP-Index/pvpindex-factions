package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
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
            MsgUtil.sendKey(actor, "general.player-not-found",
                "<red>Player <yellow>{name}</yellow> not found.", "name", ctx.arg(0));
            return;
        }
        if (actor.getUniqueId().equals(target.getUniqueId())) {
            MsgUtil.sendKey(actor, "member.cannot-kick-self", "<red>You cannot kick yourself.");
            return;
        }
        if (factionService.isOwner(target.getUniqueId())) {
            MsgUtil.sendKey(actor, "member.cannot-kick-leader", "<red>You cannot kick the faction leader.");
            return;
        }
        if (factionService.kickMember(actor.getUniqueId(), target.getUniqueId())) {
            final String targetName = target.getName() == null ? ctx.arg(0) : target.getName();
            final String kickerName = actor.getName() == null ? "Unknown" : actor.getName();
            MsgUtil.sendKey(actor, "custom.member.kick-actor", "<yellow>Kicked <white>{player}<yellow>.",
                "player", targetName);
            final String factionName = factionService.getFactionByPlayer(actor.getUniqueId())
                .map(f -> f.getName())
                .orElse("faction");
            MsgUtil.sendKey(target, "member.kicked",
                "<red>You were kicked from <yellow>{faction}</yellow> by <yellow>{kicker}</yellow>.",
                "faction", factionName,
                "kicker", kickerName);
            return;
        }
        final String targetName = target.getName() == null ? ctx.arg(0) : target.getName();
        MsgUtil.sendKey(actor, "member.not-member", "<red>{player} is not a member of your faction.", "player", targetName);
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex != 0) {
            return List.of();
        }
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }
}
