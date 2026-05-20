package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.sub.invite.CmdInviteDecline;
import com.pvpindex.factions.command.sub.invite.CmdInviteDeclineAll;
import com.pvpindex.factions.command.sub.invite.CmdInviteList;
import com.pvpindex.factions.command.sub.invite.CmdInviteRevoke;
import com.pvpindex.factions.command.sub.invite.CmdInviteAccept;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.InviteService;
import com.pvpindex.factions.util.MsgUtil;
import com.github.ezframework.jaloquent.exception.StorageException;
import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** {@code /f invite <player>} — Invite a player to your faction. */
public final class CmdInvite extends FactionCommand {

    private final FactionService factionService;
    private final InviteService inviteService;
    private final Repositories repos;

    public CmdInvite(
            final FactionService factionService,
            final InviteService inviteService,
            final Repositories repos) {
        super("invite");
        setPermission("factions.cmd.invite");
        setDescription("Invite a player to your faction.");
        setOptionalArgs("[player|list|revoke|accept|decline|declineall]");
        setRequiresPlayer(true);
        setAliases("inv");
        this.factionService = factionService;
        this.inviteService = inviteService;
        this.repos = repos;
        addChild(new CmdInviteList(factionService, inviteService));
        addChild(new CmdInviteRevoke(factionService, inviteService));
        addChild(new CmdInviteAccept(inviteService, factionService));
        addChild(new CmdInviteDecline(inviteService, factionService));
        addChild(new CmdInviteDeclineAll(inviteService));
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        if (ctx.getArgs().isEmpty()) {
            MsgUtil.sendKey(player, "general.invalid-args", "<red>Usage: {usage}", "usage", getUsage());
            return;
        }
        final Optional<FactionModel> factionOpt = CommandGuards.requireFaction(player, factionService);
        if (factionOpt.isEmpty()) {
            return;
        }
        if (!CommandGuards.requireOfficerOrAbove(player, factionService)) {
            return;
        }
        final Player target = Bukkit.getPlayer(ctx.arg(0));
        if (target == null) {
            MsgUtil.sendKey(player, "general.player-not-found",
                "<red>Player <yellow>{name}</yellow> not found.", "name", ctx.arg(0));
            return;
        }
        if (factionService.isInFaction(target.getUniqueId())) {
            MsgUtil.sendKey(player, "custom.invite.target-already-in-faction", "<red>That player is already in a faction.");
            return;
        }
        if (inviteService.sendInvite(
                factionOpt.get().getId(), player.getUniqueId(), target.getUniqueId())) {
            MsgUtil.sendKey(player, "custom.invite.sent", "<green>Invited <white>{player}<green>.", "player", target.getName());
            // Rich notification: [Accept] button that runs /f join <faction>
            try {
                final PlayerModel targetModel = repos.players().findOrCreate(target.getUniqueId().toString());
                if (targetModel.hasInviteNotifications()) {
                    MsgUtil.send(target, MsgUtil.inviteNotification(target, factionOpt.get().getName()));
                }
            } catch (StorageException ignored) {
                target.sendMessage(MsgUtil.inviteNotification(target, factionOpt.get().getName()));
            }
        } else {
            MsgUtil.sendKey(player, "custom.invite.already-pending", "<red>Could not invite that player (already pending?).");
        }
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
        }
        return List.of();
    }
}
