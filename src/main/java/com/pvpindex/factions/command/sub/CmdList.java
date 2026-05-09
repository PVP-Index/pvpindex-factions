package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** {@code /f list [page] [sort]}. */
public final class CmdList extends FactionCommand {

    private final FactionService factionService;

    public CmdList(final FactionService factionService) {
        super("list");
        setPermission("factions.cmd.list");
        setDescription("List factions.");
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
        } else if ("members".equals(sort)) {
            comparator = Comparator.comparingInt((FactionModel f) -> memberCount(ctx, f)).reversed();
        } else if ("land".equals(sort)) {
            comparator = Comparator.comparingInt((FactionModel f) -> landCount(ctx, f)).reversed();
        } else if ("power".equals(sort)) {
            comparator = Comparator.comparingDouble((FactionModel f) -> totalPower(ctx, f)).reversed();
        } else {
            comparator = Comparator.comparing(f -> f.getName().toLowerCase(Locale.ROOT));
        }
        factions.sort(comparator);
        if (factions.isEmpty()) {
            MsgUtil.send(ctx.getSender(), "<yellow>No factions found.");
            return;
        }
        final int pageSize = Math.max(1, ctx.getConfig().getListPageSize());
        final int start = Math.max(0, (page - 1) * pageSize);
        final int end = Math.min(factions.size(), start + pageSize);
        MsgUtil.send(ctx.getSender(), "<dark_gray>----------------------------------------");
        MsgUtil.send(ctx.getSender(), "<gold> Factions <gray>(page <white>" + page + "<gray>, sort <white>" + sort + "<gray>)");
        for (int i = start; i < end; i++) {
            final FactionModel faction = factions.get(i);
            MsgUtil.send(ctx.getSender(), "<yellow>#" + (i + 1) + " <white>" + faction.getName()
                + "<gray> | members <white>" + memberCount(ctx, faction)
                + "<gray> | land <white>" + landCount(ctx, faction)
                + "<gray> | bank <white>" + String.format(Locale.ROOT, "%.2f", faction.getBank()));
        }
        MsgUtil.send(ctx.getSender(), "<dark_gray>----------------------------------------");
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0 || argIndex == 1) {
            return List.of("members", "power", "land", "bank", "name");
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

    private ParsedArgs parseArgs(final String first, final String second) {
        if (first == null || first.isBlank()) {
            return new ParsedArgs(1, parseSort(second));
        }
        final String normalized = first.toLowerCase(Locale.ROOT);
        if ("members".equals(normalized)
            || "power".equals(normalized)
            || "land".equals(normalized)
            || "bank".equals(normalized)
            || "name".equals(normalized)) {
            return new ParsedArgs(1, parseSort(first));
        }
        return new ParsedArgs(parsePage(first), parseSort(second));
    }

    private String parseSort(final String input) {
        if (input == null || input.isBlank()) {
            return "name";
        }
        final String normalized = input.toLowerCase(Locale.ROOT);
        if ("members".equals(normalized)
            || "power".equals(normalized)
            || "land".equals(normalized)
            || "bank".equals(normalized)
            || "name".equals(normalized)) {
            return normalized;
        }
        return "name";
    }

    private int memberCount(final CommandContext ctx, final FactionModel faction) {
        try {
            final List<PlayerModel> members = ctx.getRepos().players().findByFactionId(faction.getId());
            return members.size();
        } catch (Exception ignored) {
            return 0;
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

    private record ParsedArgs(int page, String sort) { }

}
