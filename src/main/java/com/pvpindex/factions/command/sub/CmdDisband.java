package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import org.bukkit.entity.Player;

/** {@code /f disband} — Disband your faction (owner only). */
public final class CmdDisband extends FactionCommand {

    private final FactionService factionService;

    public CmdDisband(final FactionService factionService) {
        super("disband");
        setPermission("factions.cmd.disband");
        setDescription("Disband your faction (owner only).");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> factionOpt = CommandGuards.requireFaction(player, factionService);
        if (factionOpt.isEmpty()) {
            return;
        }
        if (!CommandGuards.requireOwner(player, factionService)) {
            return;
        }
        final PredefinedConfigManager predefined = PredefinedConfigManager.getInstance();
        if (predefined != null
            && predefined.isEnabled()
            && predefined.isBlockDisband()
            && predefined.isPredefinedName(factionOpt.get().getName())) {
            MsgUtil.sendKey(
                player,
                "predefined.disband-blocked",
                "<red>Predefined factions cannot be disbanded.");
            return;
        }
        if (factionService.disbandFaction(factionOpt.get().getId())) {
            MsgUtil.send(player, "<yellow>Your faction has been disbanded.");
        } else {
            MsgUtil.send(player, "<red>Failed to disband faction.");
        }
    }
}
