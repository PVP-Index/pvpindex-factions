package com.pvpindex.factions.command.sub.power;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.PowerHistoryModel;
import com.pvpindex.factions.util.MsgUtil;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * {@code /f powerhistory [<player>] [<page>]} — View power change events.
 *
 * <p>Records DEATH, KILL, and BUY events only (not incremental regen ticks).
 *
 * <ul>
 *   <li>{@code /f powerhistory} — own history, page 1
 *   <li>{@code /f powerhistory 2} — own history, page 2
 *   <li>{@code /f powerhistory Steve} — Steve's history (requires
 *       {@code factions.cmd.power.history.other})
 *   <li>{@code /f powerhistory Steve 2} — Steve's history, page 2
 * </ul>
 */
public final class CmdPowerHistory extends FactionCommand {

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("UTC"));

    public CmdPowerHistory() {
        super("powerhistory");
        setAliases("phist");
        setPermission("factions.cmd.power.history");
        setDescription("View power change history for yourself or another player.");
        setOptionalArgs("[<player>]", "[<page>]");
        setRequiresPlayer(false);
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final String arg0 = ctx.arg(0);
        final String arg1 = ctx.arg(1);

        final String targetUuid;
        final String targetName;
        int page = 1;

        if (arg0.isEmpty() || isPageNumber(arg0)) {
            // /f powerhistory [page] — own history
            if (!ctx.isPlayer()) {
                MsgUtil.sendKey(ctx.getSender(), "power.history-console-usage",
                    "<red>Specify a player: <yellow>/f powerhistory <player> [page]");
                return;
            }
            final Player self = (Player) ctx.getSender();
            targetUuid = self.getUniqueId().toString();
            targetName = self.getName();
            if (!arg0.isEmpty()) {
                page = parsePage(arg0);
            }
        } else {
            // /f powerhistory <player> [page] — another player's history
            if (!ctx.getSender().hasPermission("factions.cmd.power.history.other")) {
                MsgUtil.sendKey(ctx.getSender(), "general.no-permission",
                    "<red>You do not have permission to do that.");
                return;
            }
            @SuppressWarnings("deprecation")
            final OfflinePlayer op = Bukkit.getOfflinePlayer(arg0);
            if (!op.hasPlayedBefore()) {
                MsgUtil.sendKey(ctx.getSender(), "general.player-not-found",
                    "<red>Player <yellow>{name}</yellow> not found.", "name", arg0);
                return;
            }
            targetUuid = op.getUniqueId().toString();
            targetName = op.getName() != null ? op.getName() : arg0;
            if (!arg1.isEmpty()) {
                page = parsePage(arg1);
            }
        }

        final int offset = (page - 1) * PAGE_SIZE;
        try {
            final List<PowerHistoryModel> rows =
                ctx.getRepos().powerHistory().findRecentByPlayerUuid(targetUuid, PAGE_SIZE, offset);
            if (rows.isEmpty()) {
                MsgUtil.sendKey(ctx.getSender(), "power.history-empty",
                    "<yellow>No power history found for <white>{name}<yellow>.", "name", targetName);
                return;
            }
            MsgUtil.sendKey(ctx.getSender(), "power.history-header",
                "<gold>== Power History: <yellow>{name}<gold> (Page {page}) ==",
                "name", targetName, "page", String.valueOf(page));
            for (final PowerHistoryModel row : rows) {
                final String timeStr = DATE_FMT.format(Instant.ofEpochMilli(row.getCreatedAt()));
                final double delta = row.getDelta();
                final String afterStr = String.format(Locale.ROOT, "%.1f", row.getPowerAfter());
                final String reason = row.getReason();
                if (delta >= 0) {
                    final String deltaStr = "+" + String.format(Locale.ROOT, "%.1f", delta);
                    MsgUtil.sendKey(ctx.getSender(), "power.history-entry-gain",
                        "<dark_aqua>{time}  <white>{reason}  <green>{delta}  <gray>\u2192  <white>{power_after}",
                        "time", timeStr, "reason", reason, "delta", deltaStr, "power_after", afterStr);
                } else {
                    final String deltaStr = String.format(Locale.ROOT, "%.1f", delta);
                    MsgUtil.sendKey(ctx.getSender(), "power.history-entry-loss",
                        "<dark_aqua>{time}  <white>{reason}  <red>{delta}  <gray>\u2192  <white>{power_after}",
                        "time", timeStr, "reason", reason, "delta", deltaStr, "power_after", afterStr);
                }
            }
        } catch (com.github.ezframework.jaloquent.exception.StorageException e) {
            MsgUtil.sendKey(ctx.getSender(), "power.history-storage-error",
                "<red>A storage error occurred. Please try again.");
        }
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }

    private static boolean isPageNumber(final String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static int parsePage(final String s) {
        try {
            return Math.max(1, Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
