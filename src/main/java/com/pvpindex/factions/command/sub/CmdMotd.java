package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import org.bukkit.entity.Player;

/** {@code /f motd [clear | <text...>]} — View or set the faction message of the day. */
public final class CmdMotd extends FactionCommand {

    private final FactionService factionService;

    public CmdMotd(final FactionService factionService) {
        super("motd");
        setPermission("factions.cmd.motd");
        setDescription("View or set the faction message of the day.");
        setOptionalArgs("[clear | <text...>]");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> factionOpt =
            CommandGuards.requireFaction(player, factionService);
        if (factionOpt.isEmpty()) {
            return;
        }

        if (ctx.getArgs().isEmpty()) {
            // View current MOTD
            final String motd = factionOpt.get().getMotd();
            if (motd == null || motd.isEmpty()) {
                MsgUtil.sendKey(player, "motd.none", "<gray>Your faction has no MOTD set.");
            } else {
                MsgUtil.sendKey(player, "motd.header", "<gold>== <yellow>Faction MOTD</yellow> ==");
                MsgUtil.sendKey(player, "motd.display", "<gray>{motd}", "motd", motd);
            }
            return;
        }

        if (!CommandGuards.requireOfficerOrAbove(player, factionService)) {
            return;
        }

        if ("clear".equalsIgnoreCase(ctx.arg(0))) {
            if (factionService.setFactionMotd(player.getUniqueId(), "")) {
                MsgUtil.sendKey(player, "motd.cleared", "<yellow>Faction MOTD cleared.");
            } else {
                MsgUtil.sendKey(player, "motd.set-failed", "<red>Could not update faction MOTD.");
            }
            return;
        }

        final String motd = String.join(" ", ctx.getArgs()).trim();
        if (motd.length() > 250) {
            MsgUtil.sendKey(player, "motd.too-long", "<red>MOTD is too long (max 250 chars).");
            return;
        }
        if (factionService.setFactionMotd(player.getUniqueId(), motd)) {
            MsgUtil.sendKey(player, "motd.set", "<green>Faction MOTD updated.");
        } else {
            MsgUtil.sendKey(player, "motd.set-failed", "<red>Could not update faction MOTD.");
        }
    }
}
