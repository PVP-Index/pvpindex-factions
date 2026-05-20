package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/** {@code /f leader <player> [confirm]}. */
public final class CmdLeader extends FactionCommand {

    private final FactionService factionService;

    public CmdLeader(final FactionService factionService) {
        super("leader");
        setPermission("factions.cmd.leader");
        setDescription("Transfer faction ownership to another member.");
        setRequiredArgs("<player>");
        setOptionalArgs("[confirm]");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player owner = (Player) ctx.getSender();
        if (!CommandGuards.requireOwner(owner, factionService)) {
            return;
        }
        final OfflinePlayer target = Bukkit.getOfflinePlayer(ctx.arg(0));
        if (target.getUniqueId().equals(owner.getUniqueId()) && !"confirm".equalsIgnoreCase(ctx.arg(1))) {
            MsgUtil.sendKey(owner, "custom.member.leader-confirm-self",
                "<red>Use /f leader {name} confirm to re-affirm self leadership.", "name", owner.getName());
            return;
        }
        if (factionService.transferOwnership(owner.getUniqueId(), target.getUniqueId())) {
            MsgUtil.sendKey(owner, "custom.member.leader-transferred",
                "<green>Faction ownership transferred to <white>{name}<green>.", "name", ctx.arg(0));
            return;
        }
        MsgUtil.sendKey(owner, "custom.member.leader-transfer-failed", "<red>Could not transfer ownership.");
    }
}
