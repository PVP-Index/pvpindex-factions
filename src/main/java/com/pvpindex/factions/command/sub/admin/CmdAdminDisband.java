package com.pvpindex.factions.command.sub.admin;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Optional;

/** {@code /fa disband <faction>}. */
public final class CmdAdminDisband extends FactionCommand {

    private final FactionService factionService;

    public CmdAdminDisband(final FactionService factionService) {
        super("disband");
        setPermission("factions.cmd.disband.other");
        setDescription("Disband any faction.");
        setRequiredArgs("<faction>");
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Optional<FactionModel> faction = factionService.getFactionByName(ctx.arg(0));
        if (faction.isEmpty()) {
            MsgUtil.send(ctx.getSender(), "<red>Faction not found.");
            return;
        }
        final PredefinedConfigManager predefined = PredefinedConfigManager.getInstance();
        if (predefined != null
            && predefined.isEnabled()
            && predefined.isBlockDisband()
            && predefined.isPredefinedName(faction.get().getName())) {
            MsgUtil.sendKey(
                ctx.getSender(),
                "predefined.disband-blocked",
                "<red>Predefined factions cannot be disbanded.");
            return;
        }
        if (factionService.disbandFaction(faction.get().getId())) {
            MsgUtil.send(ctx.getSender(), "<yellow>Disbanded faction <white>" + faction.get().getName() + "<yellow>.");
            return;
        }
        MsgUtil.send(ctx.getSender(), "<red>Failed to disband faction.");
    }
}
