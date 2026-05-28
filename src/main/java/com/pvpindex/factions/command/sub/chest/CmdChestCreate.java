package com.pvpindex.factions.command.sub.chest;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.TeamChestService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.entity.Player;

public final class CmdChestCreate extends FactionCommand {

    private final FactionService factionService;
    private final TeamChestService teamChestService;
    private final FactionsConfig config;

    public CmdChestCreate(final FactionService factionService, final TeamChestService teamChestService,
                          final FactionsConfig config) {
        super("create");
        setPermission("factions.cmd.chest.create");
        setDescription("Create a faction team chest.");
        setRequiresPlayer(true);
        setRequiredArgs("<name>");
        this.factionService = factionService;
        this.teamChestService = teamChestService;
        this.config = config;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> factionOpt = ChestCommandSupport.requireFaction(player, factionService);
        if (factionOpt.isEmpty()) {
            return;
        }
        if (!CommandGuards.requireOfficerOrAbove(player, factionService)) {
            return;
        }
        final String name = ctx.arg(0).toLowerCase(Locale.ROOT);
        if (!isValidName(name)) {
            MsgUtil.sendKey(player, "chest.invalid-name", "<red>Invalid chest name.");
            return;
        }
        final List<String> current = teamChestService.getChestNames(factionOpt.get().getId());
        final boolean exists = current.stream().anyMatch(chest -> chest.equalsIgnoreCase(name));
        if (exists) {
            MsgUtil.sendKey(player, "chest.already-exists", "<red>Chest <yellow>{name}</yellow> already exists.", "name", name);
            return;
        }
        if (current.size() >= config.getMaxTeamChests()) {
            MsgUtil.sendKey(
                player,
                "chest.limit-reached",
                "<red>Your faction has reached the maximum number of team chests ({max}).",
                "max",
                String.valueOf(config.getMaxTeamChests()));
            return;
        }
        if (!teamChestService.createChest(factionOpt.get().getId(), name)) {
            MsgUtil.sendKey(player, "chest.create-failed", "<red>Could not create chest <yellow>{name}</yellow>.", "name", name);
            return;
        }
        MsgUtil.sendKey(player, "chest.created", "<green>Created chest <yellow>{name}</yellow>.", "name", name);
    }

    private boolean isValidName(final String name) {
        return name != null && !name.isBlank() && name.length() <= 32 && name.matches("[a-z0-9_-]+");
    }
}
