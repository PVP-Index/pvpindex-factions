package com.pvpindex.factions.command.sub;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.Relation;
import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CmdInfo extends FactionCommand {

    private static final int DETAIL_LINES_PER_PAGE = 5;
    private static final long DAY_MILLIS = 86_400_000L;
    private final Map<String, String> lastFactionBySender = new ConcurrentHashMap<>();

    private final FactionService factionService;

    public CmdInfo(final FactionService factionService) {
        super("info");
        setDescription("Show information about a faction.");
        setOptionalArgs("[name]");
        setAliases("i", "show");
        this.factionService = factionService;
        addChild(new CmdInfoPage());
    }

    @Override
    protected void perform(final CommandContext ctx) {
        try {
            final Optional<FactionModel> factionOpt;
            if (!ctx.getArgs().isEmpty()) {
                factionOpt = factionService.getFactionByName(ctx.arg(0));
            } else if (ctx.isPlayer()) {
                factionOpt = factionService.getFactionByPlayer(((Player) ctx.getSender()).getUniqueId());
            } else {
                MsgUtil.send(ctx.getSender(), "<red>Usage: " + getUsage());
                return;
            }
            if (factionOpt.isEmpty()) {
                MsgUtil.send(ctx.getSender(), "<red>Faction not found.");
                return;
            }
            final FactionModel faction = factionOpt.get();
            final FactionInfoSnapshot snapshot = buildSnapshot(ctx, faction);
            rememberLastFaction(ctx.getSender(), faction);

            final CommandSender sender = ctx.getSender();
            MsgUtil.send(sender, MsgUtil.infoHeader(faction.getName()));
            MsgUtil.send(sender, "<dark_gray>------------------------------");
            MsgUtil.send(sender, "<gold> Leader: <white>" + formatLeader(faction));
            MsgUtil.send(sender, buildMembersLine(snapshot.members(), snapshot.maxMembers()));
            MsgUtil.send(sender, "<gold> Power: <white>" + String.format(Locale.ROOT, "%.1f", snapshot.totalPower()) + "/"
                + String.format(Locale.ROOT, "%.1f", snapshot.maxPower()));
            MsgUtil.send(sender, "<gold> Land: <white>" + snapshot.landCount());
            MsgUtil.send(sender, "<gold> Bank: <white>" + snapshot.bank());
            MsgUtil.send(sender, "<gold> Home: <white>" + formatHome(faction));
            sendRelationInfo(sender, ctx, snapshot.relations());
            if (!faction.getDescription().isBlank()) {
                MsgUtil.send(sender, "<gold> Description: <white>" + faction.getDescription());
            }
            final int totalPages = totalPages(snapshot.detailLines());
            MsgUtil.sendKey(sender, "info.details-hint",
                "<gray>Details available: <white>/f info page [1-{pages}]</white>",
                "pages", String.valueOf(totalPages));
            MsgUtil.send(sender, "<dark_gray>------------------------------");
        } catch (StorageException e) {
            MsgUtil.send(ctx.getSender(), "<red>An internal error occurred.");
            ctx.getLogger().severe("Failed to display faction info: " + e.getMessage());
        }
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return factionService.getAllFactions().stream().map(FactionModel::getName).toList();
        }
        return List.of();
    }

    private final class CmdInfoPage extends FactionCommand {
        CmdInfoPage() {
            super("page");
            setDescription("View additional faction info pages.");
            setRequiredArgs("<number>");
            setOptionalArgs("[name]");
        }

        @Override
        protected void perform(final CommandContext ctx) {
            try {
                final int requestedPage;
                try {
                    requestedPage = Integer.parseInt(ctx.arg(0));
                } catch (NumberFormatException e) {
                    MsgUtil.sendKey(ctx.getSender(), "info.invalid-page",
                        "<red>Invalid page. Use a number from <white>1</white> to <white>{max}</white>.", "max", "1");
                    return;
                }
                if (requestedPage < 1) {
                    MsgUtil.sendKey(ctx.getSender(), "info.invalid-page",
                        "<red>Invalid page. Use a number from <white>1</white> to <white>{max}</white>.", "max", "1");
                    return;
                }

                final Optional<FactionModel> factionOpt = resolvePageFaction(ctx);
                if (factionOpt.isEmpty()) {
                    MsgUtil.sendKey(ctx.getSender(), "info.page-no-context",
                        "<red>Run <white>/f info [faction]</white> first, then use <white>/f info page [N]</white>.");
                    return;
                }

                final FactionModel faction = factionOpt.get();
                final FactionInfoSnapshot snapshot = buildSnapshot(ctx, faction);
                rememberLastFaction(ctx.getSender(), faction);
                final int totalPages = totalPages(snapshot.detailLines());
                if (requestedPage > totalPages) {
                    MsgUtil.sendKey(ctx.getSender(), "info.invalid-page",
                        "<red>Invalid page. Use a number from <white>1</white> to <white>{max}</white>.",
                        "max", String.valueOf(totalPages));
                    return;
                }

                MsgUtil.sendKey(ctx.getSender(), "info.page-title",
                    "<gold>== <yellow>{name}</yellow> details <gray>(page {page}/{pages})</gray> ==",
                    "name", faction.getName(),
                    "page", String.valueOf(requestedPage),
                    "pages", String.valueOf(totalPages));

                final int start = (requestedPage - 1) * DETAIL_LINES_PER_PAGE;
                final int end = Math.min(snapshot.detailLines().size(), start + DETAIL_LINES_PER_PAGE);
                for (int i = start; i < end; i++) {
                    MsgUtil.send(ctx.getSender(), snapshot.detailLines().get(i));
                }

                if (requestedPage < totalPages) {
                    MsgUtil.sendKey(ctx.getSender(), "info.page-next", "<gray>Next: <white>/f info page {next}</white>",
                        "next", String.valueOf(requestedPage + 1));
                }
                if (requestedPage > 1) {
                    MsgUtil.sendKey(ctx.getSender(), "info.page-prev", "<gray>Previous: <white>/f info page {prev}</white>",
                        "prev", String.valueOf(requestedPage - 1));
                }
            } catch (StorageException e) {
                MsgUtil.send(ctx.getSender(), "<red>An internal error occurred.");
                ctx.getLogger().severe("Failed to display faction info page: " + e.getMessage());
            }
        }

        @Override
        protected List<String> complete(final CommandContext ctx, final int argIndex) {
            if (argIndex == 0) {
                return List.of("1", "2", "3", "4");
            }
            if (argIndex == 1) {
                return factionService.getAllFactions().stream().map(FactionModel::getName).toList();
            }
            return List.of();
        }
    }

    private String formatLeader(final FactionModel faction) {
        if (faction.getOwnerId() == null || faction.getOwnerId().isBlank()) {
            return "Unknown";
        }
        if (Bukkit.getServer() == null) {
            return faction.getOwnerId();
        }
        try {
            final UUID uuid = UUID.fromString(faction.getOwnerId());
            final OfflinePlayer owner = Bukkit.getOfflinePlayer(uuid);
            if (owner.getName() != null && !owner.getName().isBlank()) {
                return owner.getName();
            }
        } catch (IllegalArgumentException ignored) {
            // fall through
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

    private String buildMembersLine(final List<PlayerModel> members, final int maxMembers) {
        final List<String> names = new ArrayList<>();
        for (final PlayerModel member : members) {
            try {
                if (Bukkit.getServer() == null) {
                    names.add(member.getId());
                } else {
                    final OfflinePlayer offline = Bukkit.getOfflinePlayer(UUID.fromString(member.getId()));
                    names.add(offline.getName() == null ? member.getId() : offline.getName());
                }
            } catch (IllegalArgumentException ignored) {
                names.add(member.getId());
            }
        }
        final String hover = names.isEmpty() ? "No members" : "Members:<newline>- " + String.join("<newline>- ", names);
        return "<hover:show_text:'<gold>" + hover + "'>"
            + "<gold> Members: <white>" + members.size() + "/" + maxMembers + " <gray>(hover)</hover>";
    }

    private void sendRelationInfo(final CommandSender sender, final CommandContext ctx, final Map<String, Relation> relations) {
        sendRelationLine(sender, "Allies", Relation.ALLY, ctx.getConfig().isInfoShowAllies(), relations);
        sendRelationLine(sender, "Truces", Relation.TRUCE, ctx.getConfig().isInfoShowTruces(), relations);
        sendRelationLine(sender, "Neutrals", Relation.NEUTRAL, ctx.getConfig().isInfoShowNeutrals(), relations);
        sendRelationLine(sender, "Enemies", Relation.ENEMY, ctx.getConfig().isInfoShowEnemies(), relations);
    }

    private void sendRelationLine(final CommandSender sender, final String label, final Relation relation,
                                  final boolean enabled, final Map<String, Relation> relations) {
        if (!enabled) {
            return;
        }
        final List<String> names = new ArrayList<>();
        for (final Map.Entry<String, Relation> entry : relations.entrySet()) {
            if (entry.getValue() != relation) {
                continue;
            }
            final String target = factionService.getFactionById(entry.getKey()).map(FactionModel::getName).orElse(entry.getKey());
            names.add(target);
        }
        final String value = names.isEmpty() ? "None" : String.join(", ", names);
        MsgUtil.send(sender, "<gold> " + label + ": <white>" + value);
    }

    private Optional<FactionModel> resolvePageFaction(final CommandContext ctx) {
        final String namedFaction = ctx.arg(1);
        if (!namedFaction.isBlank()) {
            return factionService.getFactionByName(namedFaction);
        }
        final String factionId = lastFactionBySender.get(senderKey(ctx.getSender()));
        if (factionId == null || factionId.isBlank()) {
            return Optional.empty();
        }
        return factionService.getFactionById(factionId);
    }

    private void rememberLastFaction(final CommandSender sender, final FactionModel faction) {
        lastFactionBySender.put(senderKey(sender), faction.getId());
    }

    private String senderKey(final CommandSender sender) {
        if (sender instanceof Player player) {
            return "player:" + player.getUniqueId();
        }
        return "sender:" + sender.getName();
    }

    private int totalPages(final List<String> detailLines) {
        return Math.max(1, (int) Math.ceil(detailLines.size() / (double) DETAIL_LINES_PER_PAGE));
    }

    private FactionInfoSnapshot buildSnapshot(final CommandContext ctx, final FactionModel faction) throws StorageException {
        final List<PlayerModel> members = ctx.getRepos().players().findByFactionId(faction.getId());
        final Map<String, Relation> relations = parseRelations(faction.getRelationsJson());
        final int memberCount = members.size();
        final int maxMembers = ctx.getConfig().getMaxMembers();
        final int land = ctx.getRepos().board().countByFactionId(faction.getId());
        final String bank = String.format(Locale.ROOT, "%.2f", faction.getBank());

        double memberPower = 0.0;
        for (final PlayerModel member : members) {
            memberPower += member.getPower();
        }
        final double powerBoost = faction.getPowerBoost();
        final double totalPower = memberPower + powerBoost;
        final double maxPower = memberCount * ctx.getConfig().getMaxPower();

        final List<String> detailLines = buildDetailLines(
            ctx, faction, members, relations, memberPower, powerBoost, totalPower, land);
        return new FactionInfoSnapshot(members, relations, maxMembers, land, bank, totalPower, maxPower, detailLines);
    }

    private List<String> buildDetailLines(final CommandContext ctx, final FactionModel faction,
                                          final List<PlayerModel> members, final Map<String, Relation> relations,
                                          final double memberPower, final double powerBoost,
                                          final double totalPower, final int land) throws StorageException {
        final List<String> lines = new ArrayList<>();

        final List<String> onlineNames = new ArrayList<>();
        for (final PlayerModel member : members) {
            final Player online = safeGetOnlinePlayer(member.getId());
            if (online != null) {
                onlineNames.add(online.getName());
            }
        }
        final String onlineHover = onlineNames.isEmpty() ? "No members online"
            : "Online members:<newline>- " + String.join("<newline>- ", onlineNames);
        lines.add("<gold> Online Members: <white>" + onlineNames.size() + "/" + members.size()
            + " <gray><hover:show_text:'<gold>" + onlineHover + "'>(hover)</hover>");

        lines.add("<gold> Rank Distribution: <white>" + buildRankDistribution(ctx, faction, members));

        final long now = System.currentTimeMillis();
        final long inactiveThreshold = now - (7L * DAY_MILLIS);
        final long inactiveCount = members.stream()
            .filter(m -> m.getLastActivity() > 0 && m.getLastActivity() < inactiveThreshold).count();
        lines.add("<gold> Activity: <white>" + inactiveCount + " inactive >7d"
            + " <gray>| Last seen: <white>" + formatMostRecentActivity(members, now));

        lines.add("<gold> Relations Summary: <white>"
            + "A:" + countByRelation(relations, Relation.ALLY)
            + " T:" + countByRelation(relations, Relation.TRUCE)
            + " N:" + countByRelation(relations, Relation.NEUTRAL)
            + " E:" + countByRelation(relations, Relation.ENEMY));

        final int maxLand = computeMaxLand(ctx, totalPower);
        final String risk = faction.isRaidable() || land > maxLand ? "<red>RAIDABLE" : "<green>STABLE";
        lines.add("<gold> Claim Capacity: <white>" + land + "/" + maxLand + " <gray>(" + risk + "<gray>)");

        lines.add("<gold> Power Breakdown: <white>Total " + String.format(Locale.ROOT, "%.1f", totalPower)
            + " <gray>| Members " + String.format(Locale.ROOT, "%.1f", memberPower)
            + " <gray>| Boost " + String.format(Locale.ROOT, "%.1f", powerBoost));

        final List<PlayerModel> topContributors = members.stream()
            .sorted(Comparator.comparingDouble(PlayerModel::getPower).reversed())
            .limit(3)
            .toList();
        lines.add("<gold> Top Power Members: <white>" + formatTopMembers(topContributors));

        final String newest = members.stream()
            .filter(m -> m.getJoinedAt() > 0)
            .max(Comparator.comparingLong(PlayerModel::getJoinedAt))
            .map(m -> formatPlayerName(m.getId()) + " (" + formatAge(now - m.getJoinedAt()) + " ago)")
            .orElse("Unknown");
        lines.add("<gold> Newest Member: <white>" + newest);

        final String mostRecent = members.stream()
            .filter(m -> m.getLastActivity() > 0)
            .max(Comparator.comparingLong(PlayerModel::getLastActivity))
            .map(m -> formatPlayerName(m.getId()) + " (" + formatAge(now - m.getLastActivity()) + " ago)")
            .orElse("Unknown");
        lines.add("<gold> Most Recent Activity: <white>" + mostRecent);

        lines.add("<gold> Navigation: <white>/f info page [N] <gray>to browse all details");
        return lines;
    }

    private String buildRankDistribution(final CommandContext ctx, final FactionModel faction, final List<PlayerModel> members)
        throws StorageException {
        final Map<String, String> rankNamesById = new HashMap<>();
        for (final RankModel rank : ctx.getRepos().ranks().findByFactionId(faction.getId())) {
            rankNamesById.put(rank.getId(), rank.getName());
        }
        final Map<String, Long> counts = new LinkedHashMap<>();
        for (final PlayerModel member : members) {
            final String rankName = rankNamesById.getOrDefault(member.getRankId(), "Member");
            counts.put(rankName, counts.getOrDefault(rankName, 0L) + 1L);
        }
        if (counts.isEmpty()) {
            return "None";
        }
        return counts.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", "));
    }

    private int countByRelation(final Map<String, Relation> relations, final Relation relation) {
        int count = 0;
        for (final Relation value : relations.values()) {
            if (value == relation) {
                count++;
            }
        }
        return count;
    }

    private int computeMaxLand(final CommandContext ctx, final double totalPower) {
        final double landPerPower = ctx.getConfig().getLandPerPower();
        if (landPerPower <= 0) {
            return ctx.getConfig().getMaxLand();
        }
        return Math.min(ctx.getConfig().getMaxLand(), (int) (totalPower / landPerPower));
    }

    private Player safeGetOnlinePlayer(final String id) {
        if (Bukkit.getServer() == null) {
            return null;
        }
        try {
            return Bukkit.getPlayer(UUID.fromString(id));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String formatTopMembers(final List<PlayerModel> topContributors) {
        if (topContributors.isEmpty()) {
            return "None";
        }
        return topContributors.stream()
            .map(m -> formatPlayerName(m.getId()) + " (" + String.format(Locale.ROOT, "%.1f", m.getPower()) + ")")
            .collect(Collectors.joining(", "));
    }

    private String formatMostRecentActivity(final List<PlayerModel> members, final long now) {
        return members.stream()
            .filter(m -> m.getLastActivity() > 0)
            .max(Comparator.comparingLong(PlayerModel::getLastActivity))
            .map(m -> formatPlayerName(m.getId()) + " " + formatAge(now - m.getLastActivity()) + " ago")
            .orElse("Unknown");
    }

    private String formatPlayerName(final String playerId) {
        if (Bukkit.getServer() == null) {
            return playerId;
        }
        try {
            final OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(playerId));
            if (player.getName() != null && !player.getName().isBlank()) {
                return player.getName();
            }
        } catch (IllegalArgumentException ignored) {
        }
        return playerId;
    }

    private String formatAge(final long millis) {
        if (millis <= 0) {
            return "just now";
        }
        final long minutes = millis / 60_000L;
        if (minutes < 60) {
            return minutes + "m";
        }
        final long hours = minutes / 60L;
        if (hours < 48) {
            return hours + "h";
        }
        return (hours / 24L) + "d";
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

    private record FactionInfoSnapshot(
        List<PlayerModel> members,
        Map<String, Relation> relations,
        int maxMembers,
        int landCount,
        String bank,
        double totalPower,
        double maxPower,
        List<String> detailLines
    ) { }
}
