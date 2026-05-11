package com.pvpindex.factions.command.sub.predefined;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import org.bukkit.entity.Player;

/** {@code /f predefined claim <faction>}. */
public final class CmdPredefinedClaim extends FactionCommand {

    public CmdPredefinedClaim() {
        super("claim");
        setPermission("factions.cmd.predefined.claim");
        setDescription("Save current chunk as predefined claim.");
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
        final org.bukkit.Chunk chunk = player.getLocation().getChunk();
        manager.addClaim(faction, chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        MsgUtil.sendKey(
            player,
            "predefined.claim-saved",
            "<green>Saved predefined claim for <yellow>{faction}</yellow> at <white>{x}</white>,<white>{z}</white>.",
            "faction",
            faction,
            "x",
            String.valueOf(chunk.getX()),
            "z",
            String.valueOf(chunk.getZ()));
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
