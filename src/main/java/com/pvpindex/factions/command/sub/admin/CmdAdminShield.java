package com.pvpindex.factions.command.sub.admin;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Optional;

/**
 * {@code /fa shield <faction> clear|<start-hour> <duration-hours>}
 *
 * <p>Sets or clears the daily war-shield window for a named faction.
 * The start hour is in UTC (0–23) and the duration must be between 1 and the configured
 * maximum (default 8). Both the start and end of the window are in whole UTC hours.
 */
public final class CmdAdminShield extends FactionCommand {

    public CmdAdminShield() {
        super("shield");
        setPermission("factions.cmd.shield");
        setDescription("Set or clear a faction's daily war shield window.");
        setRequiredArgs("<faction>");
        setOptionalArgs("<clear|<start-hour (0-23)> <duration-hours>>");
        setRequiresPlayer(false);
    }

    @Override
    protected void perform(final CommandContext ctx) {
        if (!ctx.getConfig().isWarShieldEnabled()) {
            MsgUtil.sendKey(ctx.getSender(), "shield.feature-disabled",
                "<red>War shields are not enabled on this server.");
            return;
        }

        final String factionName = ctx.arg(0);
        if (factionName.isBlank()) {
            MsgUtil.send(ctx.getSender(), "<red>Usage: /fa shield <faction>"
                + " <clear|<start-hour (0-23)> <duration-hours>>");
            return;
        }

        Optional<FactionModel> faction = Optional.empty();
        try {
            faction = ctx.getRepos().factions().findAll().stream()
                    .filter(f -> f.getName().equalsIgnoreCase(factionName))
                    .findFirst();
        } catch (Exception e) {
            MsgUtil.send(ctx.getSender(), "<red>Failed to look up faction.");
            return;
        }

        if (faction.isEmpty()) {
            MsgUtil.send(ctx.getSender(), "<red>Faction '<white>" + factionName + "<red>' not found.");
            return;
        }

        final FactionModel target = faction.get();
        final String action = ctx.arg(1).toLowerCase();

        if ("clear".equals(action)) {
            target.setShieldStartHour(null);
            target.setShieldDurationHours(0);
            try {
                ctx.getRepos().factions().save(target);
            } catch (Exception e) {
                MsgUtil.send(ctx.getSender(), "<red>Failed to save faction.");
                return;
            }
            MsgUtil.send(ctx.getSender(), MsgUtil.replace(
                MsgUtil.message("shield.cleared",
                    "<yellow>War shield cleared for <white>{faction}<yellow>."),
                "faction", target.getName()));
            return;
        }

        // Parse start hour
        final int startHour;
        try {
            startHour = Integer.parseInt(action);
        } catch (NumberFormatException e) {
            MsgUtil.sendKey(ctx.getSender(), "shield.invalid-hour",
                "<red>Start hour must be 0\u201323, or use 'clear'.");
            return;
        }
        if (startHour < 0 || startHour > 23) {
            MsgUtil.sendKey(ctx.getSender(), "shield.invalid-hour",
                "<red>Start hour must be 0\u201323.");
            return;
        }

        // Parse duration
        final int maxDuration = ctx.getConfig().getWarShieldMaxDurationHours();
        final int duration;
        try {
            duration = Integer.parseInt(ctx.arg(2));
        } catch (NumberFormatException e) {
            MsgUtil.send(ctx.getSender(), MsgUtil.replace(
                MsgUtil.message("shield.invalid-duration",
                    "<red>Duration must be 1\u2013{max} hours."),
                "max", String.valueOf(maxDuration)));
            return;
        }
        if (duration < 1 || duration > maxDuration) {
            MsgUtil.send(ctx.getSender(), MsgUtil.replace(
                MsgUtil.message("shield.invalid-duration",
                    "<red>Duration must be 1\u2013{max} hours."),
                "max", String.valueOf(maxDuration)));
            return;
        }

        target.setShieldStartHour(startHour);
        target.setShieldDurationHours(duration);
        try {
            ctx.getRepos().factions().save(target);
        } catch (Exception e) {
            MsgUtil.send(ctx.getSender(), "<red>Failed to save faction.");
            return;
        }
        final String msg = MsgUtil.replace(
            MsgUtil.replace(
                MsgUtil.replace(
                    MsgUtil.message("shield.set",
                        "<green>War shield set for <yellow>{faction}</yellow>:"
                        + " <white>{start}:00 UTC</white>"
                        + " for <white>{duration}h</white>."),
                    "faction", target.getName()),
                "start", String.valueOf(startHour)),
            "duration", String.valueOf(duration));
        MsgUtil.send(ctx.getSender(), msg);
    }
}
