package com.pvpindex.factions.command.sub.predefined;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import org.bukkit.entity.Player;

/** {@code /f predefined create <faction>}. */
public final class CmdPredefinedCreate extends FactionCommand {

    private final FactionService factionService;

    public CmdPredefinedCreate(final FactionService factionService) {
        super("create");
        setPermission("factions.cmd.predefined.create");
        setDescription("Create a predefined faction.");
        setRequiredArgs("<faction>");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final PredefinedConfigManager manager = PredefinedCommandSupport.requireEnabled(ctx);
        if (manager == null) {
            return;
        }
        final Player player = (Player) ctx.getSender();
        if (factionService.isInFaction(player.getUniqueId())) {
            MsgUtil.send(player, "<red>You are already in a faction.");
            return;
        }
        final String name = ctx.arg(0);
        if (!manager.isPredefinedName(name)) {
            MsgUtil.sendKey(
                player,
                "predefined.unknown",
                "<red>Unknown predefined faction: <white>{faction}",
                "faction",
                name);
            return;
        }
        if (factionService.createFaction(name, player.getUniqueId()).isPresent()) {
            MsgUtil.sendKey(
                player,
                "faction.created",
                "<green>Faction <yellow>{name}</yellow> created.",
                "name",
                name);
            return;
        }
        MsgUtil.sendKey(player, "faction.name-taken", "<red>That faction name is already taken.");
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        final PredefinedConfigManager manager = PredefinedConfigManager.getInstance();
        if (manager == null || !manager.isEnabled()) {
            return List.of();
        }
        if (argIndex == 0) {
            return manager.presetNames().stream().sorted().toList();
        }
        return List.of();
    }
}
