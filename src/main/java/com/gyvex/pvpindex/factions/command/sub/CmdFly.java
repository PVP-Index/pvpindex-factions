package com.gyvex.pvpindex.factions.command.sub;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.gyvex.pvpindex.factions.command.CommandContext;
import com.gyvex.pvpindex.factions.command.CommandGuards;
import com.gyvex.pvpindex.factions.command.FactionCommand;
import com.gyvex.pvpindex.factions.data.model.BoardEntry;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import org.bukkit.entity.Player;

/** {@code /f fly} — toggle faction flight. */
public final class CmdFly extends FactionCommand {

    private final FactionService factionService;

    public CmdFly(final FactionService factionService) {
        super("fly");
        setPermission("factions.cmd.fly");
        setDescription("Toggle faction fly.");
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
        if (!ctx.getConfig().isFlyEnabled()) {
            MsgUtil.send(player, "<red>Faction fly is disabled.");
            return;
        }
        if (ctx.getConfig().isFlyRequireOwnTerritory() && !inOwnTerritory(ctx, player, factionOpt.get().getId())) {
            MsgUtil.send(player, "<red>You can only use faction fly in your own territory.");
            return;
        }
        final boolean newState = !factionService.isFactionFlyEnabled(player.getUniqueId());
        factionService.setFactionFlyEnabled(player.getUniqueId(), newState);
        player.setAllowFlight(newState);
        if (!newState && player.isFlying()) {
            player.setFlying(false);
        }
        MsgUtil.send(player, newState ? "<green>Faction fly enabled." : "<yellow>Faction fly disabled.");
    }

    private boolean inOwnTerritory(final CommandContext ctx, final Player player, final String factionId) {
        try {
            final Optional<BoardEntry> entry = ctx.getRepos().board().findByChunk(
                player.getWorld().getName(),
                player.getLocation().getChunk().getX(),
                player.getLocation().getChunk().getZ());
            return entry.isPresent() && factionId.equals(entry.get().getFactionId());
        } catch (StorageException e) {
            ctx.getLogger().warning("Failed to resolve territory for /f fly: " + e.getMessage());
            return false;
        }
    }
}

