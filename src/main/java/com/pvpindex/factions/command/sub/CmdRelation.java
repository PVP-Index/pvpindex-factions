package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.Relation;
import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.command.sub.relation.CmdRelationList;
import com.pvpindex.factions.command.sub.relation.CmdRelationWishes;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.config.NotificationsConfig;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.engine.FactionMemberNotifier;
import com.pvpindex.factions.integration.discordsrv.DiscordSrvNotifier;
import com.pvpindex.factions.integration.ezcountdown.EzCountdownNotifier;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.entity.Player;

/** {@code /f relation <faction> <ally|truce|neutral|enemy>} */
public final class CmdRelation extends FactionCommand {

    private final FactionService factionService;
    private final EzCountdownNotifier ezCountdownNotifier;
    private final NotificationsConfig notificationsConfig;
    private final DiscordSrvNotifier discordSrvNotifier;
    private final FactionsConfig factionsConfig;

    public CmdRelation(final FactionService factionService) {
        this(factionService, null, null, null, null);
    }

    public CmdRelation(final FactionService factionService, final EzCountdownNotifier ezCountdownNotifier) {
        this(factionService, ezCountdownNotifier, null, null, null);
    }

    public CmdRelation(
            final FactionService factionService,
            final EzCountdownNotifier ezCountdownNotifier,
            final NotificationsConfig notificationsConfig) {
        this(factionService, ezCountdownNotifier, notificationsConfig, null, null);
    }

    public CmdRelation(
            final FactionService factionService,
            final EzCountdownNotifier ezCountdownNotifier,
            final NotificationsConfig notificationsConfig,
            final DiscordSrvNotifier discordSrvNotifier,
            final FactionsConfig factionsConfig) {
        super("relation");
        setAliases("relationship");
        setPermission("factions.cmd.relation");
        setDescription("Set relation with another faction.");
        setRequiredArgs("<faction>", "<relation>");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.ezCountdownNotifier = ezCountdownNotifier;
        this.notificationsConfig = notificationsConfig;
        this.discordSrvNotifier = discordSrvNotifier;
        this.factionsConfig = factionsConfig;
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
            MsgUtil.sendKey(player, "relation.set-failed", "<red>Failed to set relation.");
            return;
        }
        final String sourceName = factionDisplayName(source.get());
        final String targetName = factionDisplayName(target.get());
        final boolean mutual = isMutual(source.get(), target.get(), relation);
        if (relation == Relation.ALLY || relation == Relation.TRUCE) {
            if (mutual) {
                MsgUtil.sendKey(
                    player,
                    "relation.mutual-established",
                    "<green>Mutual <white>{relation}<green> established with <white>{faction}<green>.",
                    "relation", relation.displayName(),
                    "faction", targetName);
                notifyFactionMembers(
                    ctx,
                    target.get().getId(),
                    MsgUtil.replace(
                        MsgUtil.message(
                            "relation.mutual-established-target",
                            "<green><white>{faction}<green> and your faction are now <white>{relation}<green>."),
                        "faction", sourceName,
                        "relation", relation.displayName()));
                sendRelationAnnouncement(sourceName, targetName, relation);
            } else {
                MsgUtil.sendKey(
                    player,
                    "relation.pending-wish",
                    "<yellow>Relation wish set to <white>{relation}<yellow> for <white>{faction}"
                        + "<yellow>. They must set the same relation to confirm.",
                    "relation", relation.displayName(),
                    "faction", targetName);
                notifyFactionMembers(
                    ctx,
                    target.get().getId(),
                    MsgUtil.replace(
                        MsgUtil.message(
                            "relation.pending-received",
                            "<yellow><white>{faction}<yellow> requested <white>{relation}<yellow> with your faction."),
                        "faction", sourceName,
                        "relation", relation.displayName()));
            }
            return;
        }
        MsgUtil.sendKey(
            player,
            "relation.set",
            "<green>You set your relation with <yellow>{faction}</yellow> to <yellow>{relation}</yellow>.",
            "faction", targetName,
            "relation", relation.displayName());
        notifyFactionMembers(
            ctx,
            target.get().getId(),
            MsgUtil.replace(
                MsgUtil.message(
                    "relation.updated-by-other",
                    "<yellow><white>{faction}<yellow> set relation to <white>{relation}<yellow>."),
                "faction", sourceName,
                "relation", relation.displayName()));
        if (relation == Relation.ENEMY) {
            sendRelationAnnouncement(sourceName, targetName, relation);
        }
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

    private boolean isMutual(final FactionModel source, final FactionModel target, final Relation relation) {
        final Map<String, Relation> sourceMap = parseRelations(source.getRelationsJson());
        final Map<String, Relation> targetMap = parseRelations(target.getRelationsJson());
        return sourceMap.get(target.getId()) == relation && targetMap.get(source.getId()) == relation;
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

    private void notifyFactionMembers(final CommandContext ctx, final String factionId, final String message) {
        FactionMemberNotifier.notifyMembers(
            null,
            ctx.getRepos(),
            ctx.getLogger(),
            factionId,
            member -> true,
            message);
    }

    private void sendRelationAnnouncement(
            final String sourceName,
            final String targetName,
            final Relation relation) {
        final String messageKey;
        final String defaultText;
        if (relation == Relation.ENEMY) {
            messageKey = "ezcountdown.relation-enemy";
            defaultText = "<red>\u2694 {source} declared war on {target}!";
        } else {
            messageKey = "ezcountdown.relation-" + relation.name().toLowerCase(java.util.Locale.ROOT);
            defaultText = "<green>\ud83e\udd1d {source} and {target} are now " + relation.displayName() + "!";
        }
        final String message = MsgUtil.replace(
            MsgUtil.message(messageKey, defaultText),
            "source", sourceName,
            "target", targetName);
        final boolean useEzCountdown = ezCountdownNotifier != null
            && ezCountdownNotifier.isEnabled()
            && notificationsConfig != null
            && notificationsConfig.isEzCountdownEnabled();
        if (useEzCountdown) {
            ezCountdownNotifier.sendAnnouncement(
                message,
                notificationsConfig.getEzCountdownDurationSeconds(),
                notificationsConfig.getEzCountdownDisplayTypes());
        } else {
            org.bukkit.Bukkit.getOnlinePlayers()
                .forEach(p -> MsgUtil.send(p, message));
        }

        if (discordSrvNotifier != null && discordSrvNotifier.isEnabled() && factionsConfig != null) {
            final String template;
            final boolean enabled;
            if (relation == Relation.ENEMY) {
                enabled = factionsConfig.isDiscordSrvRelationEnemyEnabled();
                template = factionsConfig.getDiscordSrvRelationEnemyMessage();
            } else if (relation == Relation.ALLY) {
                enabled = factionsConfig.isDiscordSrvRelationAllyEnabled();
                template = factionsConfig.getDiscordSrvRelationAllyMessage();
            } else {
                enabled = factionsConfig.isDiscordSrvRelationTruceEnabled();
                template = factionsConfig.getDiscordSrvRelationTruceMessage();
            }
            if (enabled) {
                discordSrvNotifier.sendMessage(
                    template.replace("{source}", sourceName).replace("{target}", targetName));
            }
        }
    }

    private String factionDisplayName(final FactionModel faction) {
        if (faction.getName() != null && !faction.getName().isBlank()) {
            return faction.getName();
        }
        return faction.getId();
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
