package com.pvpindex.factions.command.sub.bank;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.BankTransactionModel;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/** {@code /f bank history [page]} — show faction bank transaction history. */
public final class CmdBankHistory extends FactionCommand {

    private final FactionService factionService;

    public CmdBankHistory(final FactionService factionService) {
        super("history");
        setPermission("factions.cmd.bank.history");
        setDescription("Show faction bank transaction history.");
        setOptionalArgs("[page]");
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
        final int pageSize = Math.max(1, ctx.getConfig().getBankHistoryPageSize());
        final int page = parsePage(ctx.arg(0));
        final int offset = (page - 1) * pageSize;
        try {
            final List<BankTransactionModel> rows = ctx.getRepos().bankTransactions()
                .findRecentByFactionId(factionOpt.get().getId(), pageSize, offset);
            if (rows.isEmpty()) {
                MsgUtil.send(player, "<yellow>No bank transactions found for this page.");
                return;
            }
            MsgUtil.send(player, "<gold>== Faction Bank History (Page " + page + ") ==");
            for (final BankTransactionModel row : rows) {
                final String amount = String.format(Locale.ROOT, "%.2f", row.getAmount());
                final String when = formatWhen(row.getCreatedAt());
                final String color = row.getType().contains("OUT") || row.getType().equals("WITHDRAW")
                    ? "<red>" : "<green>";
                final String detail = row.getNote() == null || row.getNote().isBlank()
                    ? row.getType() : row.getNote();
                MsgUtil.send(player, "<gray>" + when + " " + color + amount + "<gray> - " + detail
                    + " <dark_gray>by <white>" + formatActor(row.getActorUuid()));
            }
        } catch (StorageException e) {
            MsgUtil.send(player, "<red>Could not load bank history.");
        }
    }

    private int parsePage(final String input) {
        if (input == null || input.isBlank()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(input));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private String formatWhen(final long epochMs) {
        final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);
        fmt.setTimeZone(TimeZone.getDefault());
        return fmt.format(new Date(epochMs));
    }

    private String formatActor(final String actorUuid) {
        if (actorUuid == null || actorUuid.isBlank()) {
            return "Unknown";
        }
        try {
            final OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(actorUuid));
            return player.getName() == null ? actorUuid : player.getName();
        } catch (IllegalArgumentException ignored) {
            return actorUuid;
        }
    }
}
