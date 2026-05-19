package com.pvpindex.factions.command.sub;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.FactionAuditAction;
import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.AuditLogModel;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * {@code /f audit [page] [--action=<action>]} — view the faction audit log.
 *
 * <p>Requires officer rank or above. Supports optional page number and
 * {@code --action} filter to narrow results to a specific action type.
 */
public final class CmdAudit extends FactionCommand {

    private final FactionService factionService;

    public CmdAudit(final FactionService factionService) {
        super("audit");
        setPermission("factions.cmd.audit");
        setDescription("View faction audit log.");
        setOptionalArgs("[page]", "[--action=<action>]");
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
        if (!factionService.isOfficerOrAbove(player.getUniqueId())) {
            MsgUtil.sendKey(player, "general.must-be-officer", "<red>Only officers or above can do that.");
            return;
        }

        final ParsedCommandArgs parsed = parseArguments(ctx.getArgs(), Set.of("action"));
        if (parsed.hasError()) {
            MsgUtil.send(player, parsed.errorMessage());
            return;
        }

        final int pageSize = Math.max(1, ctx.getConfig().getAuditPageSize());
        final String pageArg = parsed.positionalArgs().isEmpty() ? "" : parsed.positionalArgs().get(0);
        final int page = parsePage(pageArg);
        final int offset = (page - 1) * pageSize;
        final String actionFilter = parsed.optionValue("action");

        final String factionId = factionOpt.get().getId();
        final String factionName = factionOpt.get().getName();

        try {
            final List<AuditLogModel> entries = fetchEntries(ctx, factionId, actionFilter, pageSize, offset);
            if (entries == null) {
                return;
            }
            final String filterNote = actionFilter != null && !actionFilter.isBlank()
                ? " [" + actionFilter + "]" : "";
            MsgUtil.send(player,
                "<gold>== Faction Audit Log: <yellow>" + factionName + "<gold>" + filterNote + " (Page " + page + ") ==");
            if (entries.isEmpty()) {
                MsgUtil.send(player, "<yellow>No audit entries found on this page.");
                return;
            }
            for (final AuditLogModel entry : entries) {
                renderEntry(player, entry);
            }
        } catch (StorageException e) {
            MsgUtil.send(player, "<red>Could not load audit log.");
            ctx.getLogger().warning("Failed to load audit log for faction " + factionId + ": " + e.getMessage());
        }
    }

    private List<AuditLogModel> fetchEntries(
            final CommandContext ctx,
            final String factionId,
            final String actionFilter,
            final int pageSize,
            final int offset) throws StorageException {
        if (actionFilter != null && !actionFilter.isBlank()) {
            final Optional<FactionAuditAction> actionOpt = FactionAuditAction.fromId(actionFilter);
            if (actionOpt.isEmpty()) {
                MsgUtil.send(ctx.getSender(), "<red>Unknown action '<white>" + actionFilter
                    + "<red>'. Valid: <gray>" + FactionAuditAction.validIds());
                return null;
            }
            return ctx.getRepos().auditLogs().findByFactionAndAction(
                factionId, actionOpt.get().getId(), pageSize, offset);
        }
        return ctx.getRepos().auditLogs().findByFaction(factionId, pageSize, offset);
    }

    /** Render a single audit entry line to the player. */
    public static void renderEntry(final Player player, final AuditLogModel entry) {
        MsgUtil.send(player, "<dark_aqua>" + formatTime(entry.getCreatedAt())
            + "  <white>" + resolveActor(entry.getActorUuid())
            + "  <aqua>" + entry.getAction()
            + "  <gray>" + entry.getDetail());
    }

    /** Format epoch millis to a short date-time string. */
    public static String formatTime(final long epochMs) {
        final SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm", Locale.ROOT);
        fmt.setTimeZone(TimeZone.getDefault());
        return fmt.format(new Date(epochMs));
    }

    /** Resolve actor UUID string to a player name, or return "System" for null actors. */
    public static String resolveActor(final String actorUuid) {
        if (actorUuid == null || actorUuid.isBlank()) {
            return "System";
        }
        try {
            final OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(actorUuid));
            return op.getName() != null ? op.getName() : actorUuid.substring(0, 8);
        } catch (IllegalArgumentException ignored) {
            return actorUuid;
        }
    }

    private static int parsePage(final String input) {
        if (input == null || input.isBlank()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(input));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }
}
