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

/** {@code /f merge accept <faction>} — accept a pending merge request from another faction. */
public final class CmdMergeAccept extends FactionCommand {

    private final FactionService factionService;
    private final MergeService mergeService;

    public CmdMergeAccept(final FactionService factionService, final MergeService mergeService) {
        super("accept");
        setPermission("factions.cmd.merge");
        setDescription("Accept a merge request from another faction.");
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

        final Optional<FactionModel> targetFaction = CommandGuards.requireFaction(player, factionService);
        if (targetFaction.isEmpty()) {
            return;
        }
        if (!CommandGuards.requireOfficerOrAbove(player, factionService)) {
            return;
        }

        final Optional<FactionModel> senderFaction = factionService.getFactionByName(ctx.arg(0));
        if (senderFaction.isEmpty()) {
            MsgUtil.send(player, MsgUtil.message("merge.target-not-found",
                "<red>Faction <yellow>{faction}<red> not found.")
                .replace("{faction}", ctx.arg(0)));
            return;
        }

        final Optional<FactionModel> merged = mergeService.acceptMergeRequest(
            senderFaction.get().getId(), targetFaction.get().getId(), player.getUniqueId());

        if (merged.isEmpty()) {
            MsgUtil.send(player, MsgUtil.message("merge.no-request-found",
                "<red>No pending merge request from <yellow>{faction}<red> was found.")
                .replace("{faction}", senderFaction.get().getName()));
            return;
        }

        // Notify all current members of the merged (target) faction
        final String notifyMsg = MsgUtil.message("merge.accepted",
            "<yellow>{faction}<green> has merged into your faction!")
            .replace("{faction}", senderFaction.get().getName());
        FactionMemberNotifier.notifyMembers(
            null,
            ctx.getRepos(),
            ctx.getLogger(),
            merged.get().getId(),
            member -> true,
            notifyMsg);

        MsgUtil.send(player, MsgUtil.message("merge.merged-into",
            "<green>Successfully merged <yellow>{faction}<green> into your faction.")
            .replace("{faction}", senderFaction.get().getName()));
    }
}
