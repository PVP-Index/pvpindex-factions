package com.pvpindex.factions.command.sub.predefined;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import org.bukkit.entity.Player;

/** {@code /f predefined sethome <faction>}. */
public final class CmdPredefinedSetHome extends FactionCommand {

    public CmdPredefinedSetHome() {
        super("sethome");
        setPermission("factions.cmd.predefined.sethome");
        setDescription("Save current location as predefined faction home.");
        setRequiredArgs("<faction>");
        setRequiresPlayer(true);
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final PredefinedConfigManager manager = PredefinedCommandSupport.requireEnabled(ctx);
        if (manager == null) {
            return;
        }
        final Player player = (Player) ctx.getSender();
        final String faction = ctx.arg(0);
        if (!manager.isPredefinedName(faction)) {
            MsgUtil.sendKey(
                player,
                "predefined.unknown",
                "<red>Unknown predefined faction: <white>{faction}",
                "faction",
                faction);
            return;
        }
        manager.setHome(faction, player.getLocation());
        MsgUtil.sendKey(
            player,
            "predefined.home-saved",
            "<green>Saved predefined home for <yellow>{faction}</yellow>.",
            "faction",
            faction);
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
