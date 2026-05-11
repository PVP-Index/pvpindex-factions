package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import org.bukkit.entity.Player;

/** {@code /f create <name>} — Create a new faction. */
public final class CmdCreate extends FactionCommand {

    private final FactionService factionService;

    public CmdCreate(final FactionService factionService) {
        super("create");
        setPermission("factions.cmd.create");
        setDescription("Create a new faction.");
        setRequiredArgs("<name>");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        if (factionService.isInFaction(player.getUniqueId())) {
            MsgUtil.send(player, "<red>You are already in a faction.");
            return;
        }
        final String name = ctx.arg(0);
        final PredefinedConfigManager predefined = PredefinedConfigManager.getInstance();
        if (predefined != null && predefined.isEnabled() && !predefined.isPredefinedName(name)) {
            MsgUtil.sendKey(
                player,
                "predefined.create-not-allowed",
                "<red>You can only create predefined factions on this server.");
            return;
        }
        if (name.length() < 3 || name.length() > 32) {
            MsgUtil.send(player, "<red>Faction name must be between 3 and 32 characters.");
            return;
        }
        try {
            if (factionService.createFaction(name, player.getUniqueId()).isPresent()) {
                MsgUtil.send(player, "<green>Faction <white>" + name + "<green> created!");
            } else {
                MsgUtil.send(player, "<red>A faction with that name already exists.");
            }
        } catch (IllegalStateException e) {
            MsgUtil.send(player, "<red>An internal error occurred. Please try again.");
        }
    }
}
