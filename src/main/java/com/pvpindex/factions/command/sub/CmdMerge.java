package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.command.sub.merge.CmdMergeAccept;
import com.pvpindex.factions.command.sub.merge.CmdMergeSend;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.MergeService;
import com.pvpindex.factions.util.MsgUtil;
import org.bukkit.entity.Player;

/** {@code /f merge [send|accept]} — parent command for faction merge operations. */
public final class CmdMerge extends FactionCommand {

    public CmdMerge(final FactionService factionService, final MergeService mergeService) {
        super("merge");
        setPermission("factions.cmd.merge");
        setDescription("Send or accept faction merge requests.");
        setRequiresPlayer(true);
        addChild(new CmdMergeSend(factionService, mergeService));
        addChild(new CmdMergeAccept(factionService, mergeService));
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        MsgUtil.sendKey(player, "custom.merge.help-title", "<gold>== Faction Merge ==");
        MsgUtil.sendKey(player, "custom.merge.help-send", "<yellow>/f merge send <faction> <gray>- Send a merge request");
        MsgUtil.sendKey(player, "custom.merge.help-accept", "<yellow>/f merge accept <faction> <gray>- Accept a merge request");
    }
}
