package com.pvpindex.factions.command.sub.flag;

import com.pvpindex.factions.FactionFlag;
import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.FlagService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.entity.Player;

/**
 * {@code /f flag set <flag> [on|off]} — Toggle or explicitly set a faction flag.
 *
 * <p>Requires officer-or-above. If no value is given the flag is toggled.
 */
public final class CmdFlagSet extends FactionCommand {

    private final FactionService factionService;
    private final FlagService flagService;

    public CmdFlagSet(final FactionService factionService, final FlagService flagService) {
        super("set");
        setPermission("factions.cmd.flag.set");
        setDescription("Set a faction flag.");
        setRequiredArgs("<flag>");
        setOptionalArgs("[on|off]");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.flagService = flagService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> factionOpt = CommandGuards.requireFaction(player, factionService);
        if (factionOpt.isEmpty()) {
            return;
        }
        if (!CommandGuards.requireOfficerOrAbove(player, factionService)) {
            return;
        }
        final FactionModel faction = factionOpt.get();
        final String flagId = ctx.arg(0).toLowerCase(Locale.ROOT);
        final Optional<FactionFlag> flagOpt = FactionFlag.byId(flagId);
        if (flagOpt.isEmpty()) {
            MsgUtil.sendKey(player, "flag.invalid",
                    "<red>Unknown flag '<white>{flag}</white>'. Valid: pvp, friendly-fire, "
                    + "explosions, fire-spread, open",
                    "flag", flagId);
            return;
        }
        final FactionFlag flag = flagOpt.get();
        if (!flagService.isFlagEditable(flag)) {
            MsgUtil.sendKey(player, "flag.not-editable",
                    "<red>Flag '<white>{flag}</white>' is locked by the server administrator.",
                    "flag", flag.getId());
            return;
        }
        final boolean newValue;
        if (ctx.getArgs().size() < 2) {
            // toggle
            newValue = !flagService.getFlag(faction, flag);
        } else {
            final String raw = ctx.arg(1).toLowerCase(Locale.ROOT);
            if ("on".equals(raw) || "true".equals(raw) || "yes".equals(raw)) {
                newValue = true;
            } else if ("off".equals(raw) || "false".equals(raw) || "no".equals(raw)) {
                newValue = false;
            } else {
                MsgUtil.send(player, "<red>Value must be <white>on</white> or <white>off</white>.");
                return;
            }
        }
        flagService.setFlag(faction, flag, newValue);
        if (newValue) {
            MsgUtil.sendKey(player, "flag.set-on",
                    "<green>Flag '<white>{flag}</white>' is now <green>ON</green>.",
                    "flag", flag.getId());
        } else {
            MsgUtil.sendKey(player, "flag.set-off",
                    "<red>Flag '<white>{flag}</white>' is now <red>OFF</red>.",
                    "flag", flag.getId());
        }
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return FactionFlag.ids();
        }
        if (argIndex == 1) {
            return List.of("on", "off");
        }
        return List.of();
    }
}
