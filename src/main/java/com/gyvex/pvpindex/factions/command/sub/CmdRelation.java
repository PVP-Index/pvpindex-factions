package com.gyvex.pvpindex.factions.command.sub;

import com.gyvex.pvpindex.factions.Relation;
import com.gyvex.pvpindex.factions.command.CommandContext;
import com.gyvex.pvpindex.factions.command.CommandGuards;
import com.gyvex.pvpindex.factions.command.FactionCommand;
import com.gyvex.pvpindex.factions.command.sub.relation.CmdRelationList;
import com.gyvex.pvpindex.factions.command.sub.relation.CmdRelationWishes;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.entity.Player;

/** {@code /f relation <faction> <ally|truce|neutral|enemy>} */
public final class CmdRelation extends FactionCommand {

    private final FactionService factionService;

    public CmdRelation(final FactionService factionService) {
        super("relation");
        setPermission("factions.cmd.relation");
        setDescription("Set relation with another faction.");
        setRequiredArgs("<faction>", "<relation>");
        setRequiresPlayer(true);
        this.factionService = factionService;
        addChild(new CmdRelationList(factionService));
        addChild(new CmdRelationWishes(factionService));
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> source = CommandGuards.requireFaction(player, factionService);
        if (source.isEmpty()) {
            return;
        }
        if (!CommandGuards.requireOfficerOrAbove(player, factionService)) {
            return;
        }
        final Optional<FactionModel> target = factionService.getFactionByName(ctx.arg(0));
        if (target.isEmpty()) {
            MsgUtil.send(player, "<red>Faction not found.");
            return;
        }
        if (source.get().getId().equals(target.get().getId())) {
            MsgUtil.send(player, "<red>You cannot set a relation with your own faction.");
            return;
        }
        final Relation relation;
        try {
            relation = Relation.valueOf(ctx.arg(1).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            MsgUtil.send(player, "<red>Invalid relation. Use ally, truce, neutral, or enemy.");
            return;
        }
        if (relation == Relation.MEMBER) {
            MsgUtil.send(player, "<red>Invalid relation.");
            return;
        }
        final Optional<Relation> updated =
            factionService.setRelation(player.getUniqueId(), target.get().getName(), relation);
        if (updated.isEmpty()) {
            MsgUtil.send(player, "<red>Failed to set relation.");
            return;
        }
        MsgUtil.send(player, "<green>Relation with <white>" + target.get().getName()
            + "<green> set to <white>" + relation.displayName() + "<green>.");
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return factionService.getAllFactions().stream().map(FactionModel::getName).toList();
        }
        if (argIndex == 1) {
            return List.of("ally", "truce", "neutral", "enemy");
        }
        return List.of();
    }
}
