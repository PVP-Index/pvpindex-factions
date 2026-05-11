package com.pvpindex.factions.command.sub.relation;

import com.pvpindex.factions.Relation;
import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.entity.Player;

/** {@code /f relation list [relation]}. */
public final class CmdRelationList extends FactionCommand {

    private final FactionService factionService;

    public CmdRelationList(final FactionService factionService) {
        super("list");
        setPermission("factions.cmd.relation");
        setDescription("List faction relations.");
        setOptionalArgs("[ally|truce|neutral|enemy]");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> source = CommandGuards.requireFaction(player, factionService);
        if (source.isEmpty()) {
            return;
        }
        final Relation filter = parseRelation(ctx.arg(0));
        final Map<String, Relation> relations = parseRelations(source.get().getRelationsJson());
        MsgUtil.send(player, "<gold>== Faction Relations ==");
        int shown = 0;
        for (final Map.Entry<String, Relation> entry : relations.entrySet()) {
            if (filter != null && entry.getValue() != filter) {
                continue;
            }
            final Optional<FactionModel> targetFaction = factionService.getFactionById(entry.getKey());
            final String target = targetFaction.map(FactionModel::getName).orElse(entry.getKey());
            final String status = relationStatus(source.get(), targetFaction.orElse(null), entry.getValue());
            MsgUtil.send(player, "<yellow>- <white>" + target + "<gray>: " + entry.getValue().colorTag()
                + entry.getValue().displayName() + "<gray> (" + status + ")");
            shown++;
        }
        if (shown == 0) {
            MsgUtil.send(player, "<gray>No relation entries.");
        }
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return List.of("ally", "truce", "neutral", "enemy");
        }
        return List.of();
    }

    private Relation parseRelation(final String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return Relation.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Map<String, Relation> parseRelations(final String json) {
        final Map<String, Relation> out = new HashMap<>();
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return out;
        }
        final String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return out;
        }
        final String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return out;
        }
        for (final String rawEntry : body.split(",")) {
            final String[] kv = rawEntry.split(":", 2);
            if (kv.length != 2) {
                continue;
            }
            final String key = stripQuotes(kv[0].trim());
            final String value = stripQuotes(kv[1].trim());
            try {
                out.put(key, Relation.valueOf(value));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid stored values.
            }
        }
        return out;
    }

    private String relationStatus(final FactionModel source, final FactionModel target, final Relation relation) {
        if (target == null) {
            return "unknown";
        }
        if (relation != Relation.ALLY && relation != Relation.TRUCE) {
            return "active";
        }
        final Map<String, Relation> targetMap = parseRelations(target.getRelationsJson());
        return targetMap.get(source.getId()) == relation ? "mutual" : "pending";
    }

    private String stripQuotes(final String value) {
        String out = value;
        if (out.startsWith("\"")) out = out.substring(1);
        if (out.endsWith("\"")) out = out.substring(0, out.length() - 1);
        return out;
    }
}
