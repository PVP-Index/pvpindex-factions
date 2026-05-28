package com.pvpindex.factions.command.sub.chest;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.engine.EngineTeamChests;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.TeamChestService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import org.bukkit.entity.Player;

/**
 * {@code /f chest} command group.
 */
public final class CmdChest extends FactionCommand {

    private final FactionService factionService;
    private final TeamChestService teamChestService;
    private final EngineTeamChests teamChestsEngine;
    private final FactionsConfig config;

    public CmdChest(final FactionService factionService, final TeamChestService teamChestService,
                    final EngineTeamChests teamChestsEngine, final FactionsConfig config) {
        super("chest");
        setPermission("factions.cmd.chest");
        setDescription("Open and manage faction team chests.");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.teamChestService = teamChestService;
        this.teamChestsEngine = teamChestsEngine;
        this.config = config;
        addChild(new CmdChestCreate(factionService, teamChestService, config));
        addChild(new CmdChestDelete(factionService, teamChestService));
        addChild(new CmdChestList(factionService, teamChestService));
        addChild(new CmdChestOpen(factionService, teamChestService, teamChestsEngine, config));
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> factionOpt = ChestCommandSupport.requireFaction(player, factionService);
        if (factionOpt.isEmpty()) {
            return;
        }
        final String chestName = config.getDefaultTeamChestName();
        final Optional<String> ensured = teamChestService.ensureChestExistsForOpen(factionOpt.get().getId(), chestName);
        if (ensured.isEmpty()) {
            MsgUtil.sendKey(
                player,
                "chest.limit-reached",
                "<red>Your faction has reached the maximum number of team chests ({max}).",
                "max",
                String.valueOf(config.getMaxTeamChests()));
            return;
        }
        final String title = "Faction Chest: " + ensured.get();
        if (!teamChestsEngine.openChest(player, factionOpt.get().getId(), ensured.get(), title)) {
            MsgUtil.sendKey(player, "chest.not-found", "<red>Chest <yellow>{name}</yellow> was not found.", "name", ensured.get());
            return;
        }
        MsgUtil.sendKey(player, "chest.opened", "<green>Opened chest <yellow>{name}</yellow>.", "name", ensured.get());
    }
}
