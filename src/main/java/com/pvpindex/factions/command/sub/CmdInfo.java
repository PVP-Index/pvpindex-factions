package com.pvpindex.factions.command.sub;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.Relation;
import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * {@code /f info [factionName]} — Display faction information.
 *
 * <p>Usable by console if a faction name argument is provided. The faction
 * name in the header is a rich {@link net.kyori.adventure.text.Component}
 * — clicking it suggests {@code /f info <name>} to refresh the view.
 */
public final class CmdInfo extends FactionCommand {

    private final FactionService factionService;

    public CmdInfo(final FactionService factionService) {
        super("info");
        setDescription("Show information about a faction.");
        setOptionalArgs("[name]");
        setAliases("i", "show");
        // Not player-only: console can view info when a name is supplied
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        try {
            final Optional<FactionModel> factionOpt;
            if (!ctx.getArgs().isEmpty()) {
                factionOpt = factionService.getFactionByName(ctx.arg(0));
            } else if (ctx.isPlayer()) {
                factionOpt = factionService.getFactionByPlayer(
                    ((Player) ctx.getSender()).getUniqueId());
            } else {
                ctx.getSender().sendMessage(MsgUtil.parse("<red>Usage: " + getUsage()));
                return;
            }
            if (factionOpt.isEmpty()) {
                MsgUtil.send(ctx.getSender(), "<red>Faction not found.");
                return;
            }
            final FactionModel faction = factionOpt.get();
            final List<PlayerModel> members = ctx.getRepos().players().findByFactionId(faction.getId());
            final int memberCount = members.size();
            final int maxMembers = ctx.getConfig().getMaxMembers();
            final int land = ctx.getRepos().board().countByFactionId(faction.getId());
            final String bank = String.format(Locale.ROOT, "%.2f", faction.getBank());

            double totalPower = faction.getPowerBoost();
            for (final PlayerModel member : members) {
                totalPower += member.getPower();
            }
            final double maxPower = memberCount * ctx.getConfig().getMaxPower();

            final CommandSender sender = ctx.getSender();
            sender.sendMessage(MsgUtil.infoHeader(faction.getName()));
            MsgUtil.send(sender, "<dark_gray>------------------------------");
            MsgUtil.send(sender, "<gold> Leader: <white>" + formatLeader(faction));
            sender.sendMessage(buildMembersLine(members, maxMembers));
            MsgUtil.send(sender, "<gold> Power: <white>"
                + String.format(Locale.ROOT, "%.1f", totalPower) + "/"
                + String.format(Locale.ROOT, "%.1f", maxPower));
            MsgUtil.send(sender, "<gold> Land: <white>" + land);
            MsgUtil.send(sender, "<gold> Bank: <white>" + bank);
            MsgUtil.send(sender, "<gold> Home: <white>" + formatHome(faction));
            sendRelationInfo(sender, ctx, faction);
            if (!faction.getDescription().isBlank()) {
                MsgUtil.send(sender, "<gold> Description: <white>" + faction.getDescription());
            }
            MsgUtil.send(sender, "<dark_gray>------------------------------");
        } catch (StorageException e) {
            MsgUtil.send(ctx.getSender(), "<red>An internal error occurred.");
            ctx.getLogger().severe("Failed to display faction info: " + e.getMessage());
        }
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return factionService.getAllFactions().stream()
                .map(FactionModel::getName)
                .toList();
        }
        return List.of();
    }

    private String formatLeader(final FactionModel faction) {
        if (faction.getOwnerId() == null || faction.getOwnerId().isBlank()) {
            return "Unknown";
        }
        try {
            final UUID uuid = UUID.fromString(faction.getOwnerId());
            final OfflinePlayer owner = Bukkit.getOfflinePlayer(uuid);
            if (owner.getName() != null && !owner.getName().isBlank()) {
                return owner.getName();
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to raw id if not a UUID.
        }
        return faction.getOwnerId();
    }

    private String formatHome(final FactionModel faction) {
        if (!faction.hasHome()) {
            return "Not set";
        }
        return faction.getHomeWorld() + " ("
            + String.format(Locale.ROOT, "%.1f", faction.getHomeX()) + ", "
            + String.format(Locale.ROOT, "%.1f", faction.getHomeY()) + ", "
            + String.format(Locale.ROOT, "%.1f", faction.getHomeZ()) + ")";
    }

    private Component buildMembersLine(final List<PlayerModel> members, final int maxMembers) {
        final List<String> names = new ArrayList<>();
        for (final PlayerModel member : members) {
            try {
                final OfflinePlayer offline = Bukkit.getOfflinePlayer(UUID.fromString(member.getId()));
                names.add(offline.getName() == null ? member.getId() : offline.getName());
            } catch (IllegalArgumentException ignored) {
                names.add(member.getId());
            }
        }
        final String hover = names.isEmpty()
            ? "<gray>No members"
            : "<gold>Members:<newline><white>- " + String.join("<newline>- ", names);
        return MsgUtil.parse("<gold> Members: <white>" + members.size() + "/" + maxMembers
                + " <gray>(hover)")
            .hoverEvent(HoverEvent.showText(MsgUtil.parse(hover)));
    }

    private void sendRelationInfo(final CommandSender sender, final CommandContext ctx, final FactionModel faction) {
        final Map<String, Relation> relations = parseRelations(faction.getRelationsJson());
        sendRelationLine(sender, "Allies", Relation.ALLY, ctx.getConfig().isInfoShowAllies(), relations);
        sendRelationLine(sender, "Truces", Relation.TRUCE, ctx.getConfig().isInfoShowTruces(), relations);
        sendRelationLine(sender, "Neutrals", Relation.NEUTRAL, ctx.getConfig().isInfoShowNeutrals(), relations);
        sendRelationLine(sender, "Enemies", Relation.ENEMY, ctx.getConfig().isInfoShowEnemies(), relations);
    }

    private void sendRelationLine(
            final CommandSender sender,
            final String label,
            final Relation relation,
            final boolean enabled,
            final Map<String, Relation> relations) {
        if (!enabled) {
            return;
        }
        final List<String> names = new ArrayList<>();
        for (final Map.Entry<String, Relation> entry : relations.entrySet()) {
            if (entry.getValue() != relation) {
                continue;
            }
            final String target = factionService.getFactionById(entry.getKey())
                .map(FactionModel::getName)
                .orElse(entry.getKey());
            names.add(target);
        }
        final String value = names.isEmpty() ? "None" : String.join(", ", names);
        MsgUtil.send(sender, "<gold> " + label + ": <white>" + value);
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

    private String stripQuotes(final String value) {
        String out = value;
        if (out.startsWith("\"")) {
            out = out.substring(1);
        }
        if (out.endsWith("\"")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }
}
