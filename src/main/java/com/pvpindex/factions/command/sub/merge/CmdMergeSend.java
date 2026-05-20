package com.pvpindex.factions.command.sub.merge;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.engine.FactionMemberNotifier;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.MergeService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import org.bukkit.entity.Player;

/** {@code /f merge send <faction>} — send a merge request to another faction. */
public final class CmdMergeSend extends FactionCommand {

    private final FactionService factionService;
    private final MergeService mergeService;

    public CmdMergeSend(final FactionService factionService, final MergeService mergeService) {
        super("send");
        setPermission("factions.cmd.merge");
        setDescription("Send a merge request to another faction.");
        setRequiredArgs("<faction>");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.mergeService = mergeService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();

        if (!ctx.getConfig().isMergeEnabled()) {
            MsgUtil.sendKey(player, "merge.disabled", "<red>Faction merging is not enabled on this server.");
            return;
        }

        final Optional<FactionModel> senderFaction = CommandGuards.requireFaction(player, factionService);
        if (senderFaction.isEmpty()) {
            return;
        }
        if (!CommandGuards.requireOfficerOrAbove(player, factionService)) {
            return;
        }

        final Optional<FactionModel> targetFaction = factionService.getFactionByName(ctx.arg(0));
        if (targetFaction.isEmpty()) {
            MsgUtil.send(player, MsgUtil.message("merge.target-not-found",
                "<red>Faction <yellow>{faction}<red> not found.")
                .replace("{faction}", ctx.arg(0)));
            return;
        }

        if (senderFaction.get().getId().equals(targetFaction.get().getId())) {
            MsgUtil.send(player, MsgUtil.message("merge.self-merge",
                "<red>You cannot merge your faction into itself."));
            return;
        }

        final boolean sent = mergeService.sendMergeRequest(
            senderFaction.get().getId(), targetFaction.get().getId(), player.getUniqueId());

        if (!sent) {
            MsgUtil.send(player, MsgUtil.message("merge.already-requested",
                "<red>A merge request to <yellow>{faction}<red> is already pending.")
                .replace("{faction}", targetFaction.get().getName()));
            return;
        }

        MsgUtil.send(player, MsgUtil.message("merge.request-sent",
            "<green>Merge request sent to <yellow>{faction}<green>.")
            .replace("{faction}", targetFaction.get().getName()));

        // Notify online members of the target faction
        final String notifyMsg = MsgUtil.message("merge.request-received",
            "<yellow>{faction}<green> has sent a merge request to your faction. "
            + "Use <white>/f merge accept {faction}<green> to absorb them.")
            .replace("{faction}", senderFaction.get().getName());
        FactionMemberNotifier.notifyMembers(
            null,
            ctx.getRepos(),
            ctx.getLogger(),
            targetFaction.get().getId(),
            member -> true,
            notifyMsg);
    }
}
