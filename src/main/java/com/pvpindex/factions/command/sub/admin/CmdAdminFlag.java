package com.pvpindex.factions.command.sub.admin;

import com.pvpindex.factions.FactionFlag;
import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.FlagService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * {@code /fa flag <faction> <flag> [on|off]} — Admin override for faction flags.
 *
 * <p>Admins may set any flag regardless of the {@code player-editable} config.
 */
public final class CmdAdminFlag extends FactionCommand {

    private final FactionService factionService;
    private final FlagService flagService;

    public CmdAdminFlag(final FactionService factionService, final FlagService flagService) {
        super("flag");
        setPermission("factions.admin");
        setDescription("Set a faction flag (admin override).");
        setRequiredArgs("<faction> <flag>");
        setOptionalArgs("[on|off]");
        this.factionService = factionService;
        this.flagService = flagService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final String factionName = ctx.arg(0);
        final Optional<FactionModel> factionOpt = factionService.getFactionByName(factionName);
        if (factionOpt.isEmpty()) {
            MsgUtil.send(ctx.getSender(), "<red>Faction '<white>" + factionName + "</white>' not found.");
            return;
        }
        final FactionModel faction = factionOpt.get();
        final String flagId = ctx.arg(1).toLowerCase(Locale.ROOT);
        final Optional<FactionFlag> flagOpt = FactionFlag.byId(flagId);
        if (flagOpt.isEmpty()) {
            MsgUtil.sendKey(ctx.getSender(), "flag.invalid",
                    "<red>Unknown flag '<white>{flag}</white>'.", "flag", flagId);
            return;
        }
        final FactionFlag flag = flagOpt.get();
        final boolean newValue;
        if (ctx.getArgs().size() < 3) {
            newValue = !flagService.getFlag(faction, flag);
        } else {
            final String raw = ctx.arg(2).toLowerCase(Locale.ROOT);
            if ("on".equals(raw) || "true".equals(raw) || "yes".equals(raw)) {
                newValue = true;
            } else if ("off".equals(raw) || "false".equals(raw) || "no".equals(raw)) {
                newValue = false;
            } else {
                MsgUtil.send(ctx.getSender(), "<red>Value must be <white>on</white> or <white>off</white>.");
                return;
            }
        }
        flagService.setFlag(faction, flag, newValue);
        final String state = newValue ? "<green>ON</green>" : "<red>OFF</red>";
        MsgUtil.sendKey(ctx.getSender(), "flag.admin-override",
                "<gray>[Admin] Set '<white>{flag}</white>' to " + state + " for <white>{faction}</white>.",
                "flag", flag.getId(), "faction", faction.getName());
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 1) {
            return FactionFlag.ids();
        }
        if (argIndex == 2) {
            return List.of("on", "off");
        }
        return List.of();
    }
}
