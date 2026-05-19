package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.command.sub.flag.CmdFlagList;
import com.pvpindex.factions.command.sub.flag.CmdFlagSet;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.FlagService;
import java.util.Optional;
import org.bukkit.entity.Player;

/**
 * {@code /f flag [list|set]} — Faction flags parent command.
 *
 * <p>No-argument invocation delegates to {@link CmdFlagList} to show the
 * current flag values. Sub-commands:
 * <ul>
 *   <li>{@code /f flag list} — show flags</li>
 *   <li>{@code /f flag set <flag> [on|off]} — toggle or set a flag</li>
 * </ul>
 */
public final class CmdFlag extends FactionCommand {

    private final FactionService factionService;
    private final CmdFlagList cmdFlagList;

    public CmdFlag(final FactionService factionService, final FlagService flagService) {
        super("flag");
        setPermission("factions.cmd.flag");
        setDescription("Manage faction flags.");
        setOptionalArgs("[list|set]");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.cmdFlagList = new CmdFlagList(factionService, flagService);
        addChild(cmdFlagList);
        addChild(new CmdFlagSet(factionService, flagService));
    }

    @Override
    protected void perform(final CommandContext ctx) {
        // No-arg: show flag list for the player's faction
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> factionOpt = CommandGuards.requireFaction(player, factionService);
        if (factionOpt.isEmpty()) {
            return;
        }
        cmdFlagList.execute(ctx);
    }
}
