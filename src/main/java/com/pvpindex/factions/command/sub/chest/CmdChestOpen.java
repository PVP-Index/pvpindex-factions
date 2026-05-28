package com.pvpindex.factions.command.sub.chest;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.engine.EngineTeamChests;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.TeamChestService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.entity.Player;

public final class CmdChestOpen extends FactionCommand {

    private final FactionService factionService;
    private final TeamChestService teamChestService;
    private final EngineTeamChests teamChestsEngine;
    private final FactionsConfig config;

    public CmdChestOpen(final FactionService factionService, final TeamChestService teamChestService,
                        final EngineTeamChests teamChestsEngine, final FactionsConfig config) {
        super("open");
        setPermission("factions.cmd.chest");
        setDescription("Open a named faction team chest.");
        setRequiresPlayer(true);
        setRequiredArgs("<name>");
        this.factionService = factionService;
        this.teamChestService = teamChestService;
        this.teamChestsEngine = teamChestsEngine;
        this.config = config;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> factionOpt = ChestCommandSupport.requireFaction(player, factionService);
        if (factionOpt.isEmpty()) {
            return;
        }
        final String requestedName = ctx.arg(0).toLowerCase(Locale.ROOT);
        final Optional<String> ensured = teamChestService.ensureChestExistsForOpen(factionOpt.get().getId(), requestedName);
        if (ensured.isEmpty()) {
            MsgUtil.sendKey(
                player,
                "chest.limit-reached",
                "<red>Your faction has reached the maximum number of team chests ({max}).",
                "max",
                String.valueOf(config.getMaxTeamChests()));
            return;
        }
        final String chestName = ensured.get();
        final String title = "Faction Chest: " + chestName;
        if (!teamChestsEngine.openChest(player, factionOpt.get().getId(), chestName, title)) {
            MsgUtil.sendKey(player, "chest.not-found", "<red>Chest <yellow>{name}</yellow> was not found.", "name", chestName);
            return;
        }
        MsgUtil.sendKey(player, "chest.opened", "<green>Opened chest <yellow>{name}</yellow>.", "name", chestName);
    }
}
