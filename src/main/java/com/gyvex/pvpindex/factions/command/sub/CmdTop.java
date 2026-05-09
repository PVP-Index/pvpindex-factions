package com.gyvex.pvpindex.factions.command.sub;

import com.gyvex.pvpindex.factions.command.CommandContext;
import com.gyvex.pvpindex.factions.command.FactionCommand;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.data.model.PlayerModel;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** {@code /f top [page] [power|bank|land]}. */
public final class CmdTop extends FactionCommand {

    private final FactionService factionService;

    public CmdTop(final FactionService factionService) {
        super("top");
        setPermission("factions.cmd.top");
        setDescription("Show top factions by power.");
        setOptionalArgs("[page]", "[sort]");
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final ParsedArgs parsed = parseArgs(ctx.arg(0), ctx.arg(1));
        final int page = parsed.page();
        final String sort = parsed.sort();
        final List<FactionModel> factions = new ArrayList<>(factionService.getAllFactions());
        final Comparator<FactionModel> comparator;
        if ("bank".equals(sort)) {
            comparator = Comparator.comparingDouble(FactionModel::getBank).reversed();
        } else if ("land".equals(sort)) {
            comparator = Comparator.comparingInt((FactionModel f) -> landCount(ctx, f)).reversed();
        } else {
            comparator = Comparator.comparingDouble((FactionModel f) -> totalPower(ctx, f)).reversed();
        }
        factions.sort(comparator);
        if (factions.isEmpty()) {
            MsgUtil.send(ctx.getSender(), "<yellow>No factions found.");
            return;
        }
        final int pageSize = Math.max(1, ctx.getConfig().getTopPageSize());
        final int start = Math.max(0, (page - 1) * pageSize);
        final int end = Math.min(factions.size(), start + pageSize);
        MsgUtil.send(ctx.getSender(), "<dark_gray>----------------------------------------");
        MsgUtil.send(ctx.getSender(),
            "<gold> Faction Top <gray>(page <white>" + page + "<gray>, sort <white>" + sort + "<gray>)");
        for (int i = start; i < end; i++) {
            final FactionModel faction = factions.get(i);
            MsgUtil.send(ctx.getSender(), "<yellow>#" + (i + 1) + " <white>" + faction.getName()
                + "<gray> | power <white>" + String.format(Locale.ROOT, "%.1f", totalPower(ctx, faction))
                + "<gray> | land <white>" + landCount(ctx, faction)
                + "<gray> | bank <white>" + String.format(Locale.ROOT, "%.2f", faction.getBank()));
        }
        MsgUtil.send(ctx.getSender(), "<dark_gray>----------------------------------------");
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0 || argIndex == 1) {
            return List.of("power", "bank", "land");
        }
        return List.of();
    }

    private int parsePage(final String s) {
        try {
            return Math.max(1, Integer.parseInt(s));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private int landCount(final CommandContext ctx, final FactionModel faction) {
        try {
            return ctx.getRepos().board().countByFactionId(faction.getId());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private double totalPower(final CommandContext ctx, final FactionModel faction) {
        try {
            double total = faction.getPowerBoost();
            for (final PlayerModel member : ctx.getRepos().players().findByFactionId(faction.getId())) {
                total += member.getPower();
            }
            return total;
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private String parseSort(final String input) {
        final String normalized = input == null ? "" : input.toLowerCase(Locale.ROOT);
        if ("bank".equals(normalized) || "land".equals(normalized)) {
            return normalized;
        }
        return "power";
    }

    private ParsedArgs parseArgs(final String first, final String second) {
        if (first == null || first.isBlank()) {
            return new ParsedArgs(1, parseSort(second));
        }
        final String normalized = first.toLowerCase(Locale.ROOT);
        if ("power".equals(normalized) || "bank".equals(normalized) || "land".equals(normalized)) {
            return new ParsedArgs(1, parseSort(first));
        }
        return new ParsedArgs(parsePage(first), parseSort(second));
    }

    private record ParsedArgs(int page, String sort) { }
}
