package com.pvpindex.factions.command.sub.admin;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.FactionAuditAction;
import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.command.sub.CmdAudit;
import com.pvpindex.factions.data.model.AuditLogModel;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@code /fa audit <faction> [page] [--action=<action>]} — admin audit log view.
 *
 * <p>Admins can view the audit log of any faction and optionally filter by
 * action type. Console-friendly: does not require the sender to be a player.
 */
public final class CmdAdminAudit extends FactionCommand {

    private final FactionService factionService;

    public CmdAdminAudit(final FactionService factionService) {
        super("audit");
        setPermission("factions.admin");
        setDescription("View any faction's audit log.");
        setRequiredArgs("<faction>");
        setOptionalArgs("[page]", "[--action=<action>]");
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final String factionName = ctx.arg(0);
        final Optional<FactionModel> factionOpt = factionService.getFactionByName(factionName);
        if (factionOpt.isEmpty()) {
            MsgUtil.sendKey(ctx.getSender(), "general.faction-not-found",
                "<red>Faction <yellow>{name}</yellow> not found.", "name", factionName);
            return;
        }

        final List<String> remaining = ctx.getArgs().size() > 1
            ? ctx.getArgs().subList(1, ctx.getArgs().size()) : List.of();
        final ParsedCommandArgs parsed = parseArguments(remaining, Set.of("action"));
        if (parsed.hasError()) {
            MsgUtil.send(ctx.getSender(), parsed.errorMessage());
            return;
        }

        final int pageSize = Math.max(1, ctx.getConfig().getAuditPageSize());
        final String pageArg = parsed.positionalArgs().isEmpty() ? "" : parsed.positionalArgs().get(0);
        final int page = parsePage(pageArg);
        final int offset = (page - 1) * pageSize;
        final String actionFilter = parsed.optionValue("action");

        final String factionId = factionOpt.get().getId();
        final String displayName = factionOpt.get().getName();

        try {
            final List<AuditLogModel> entries = fetchEntries(ctx, factionId, actionFilter, pageSize, offset);
            if (entries == null) {
                return;
            }
            final String filterNote = actionFilter != null && !actionFilter.isBlank()
                ? " [" + actionFilter + "]" : "";
            MsgUtil.send(ctx.getSender(),
                "<gold>== Audit Log: <yellow>" + displayName + "<gold>" + filterNote + " (Page " + page + ") ==");
            if (entries.isEmpty()) {
                MsgUtil.send(ctx.getSender(), "<yellow>No audit entries found on this page.");
                return;
            }
            for (final AuditLogModel entry : entries) {
                MsgUtil.send(ctx.getSender(),
                    "<dark_aqua>" + CmdAudit.formatTime(entry.getCreatedAt())
                    + "  <white>" + CmdAudit.resolveActor(entry.getActorUuid())
                    + "  <aqua>" + entry.getAction()
                    + "  <gray>" + entry.getDetail());
            }
        } catch (StorageException e) {
            MsgUtil.send(ctx.getSender(), "<red>Could not load audit log.");
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
