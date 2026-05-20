package com.pvpindex.factions.command.sub;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import org.bukkit.entity.Player;

/** {@code /f notify [status|invites|territory|tax|all] [on|off]}. */
public final class CmdNotify extends FactionCommand {

    public CmdNotify() {
        super("notify");
        setAliases("notifications");
        setPermission("factions.cmd.notify");
        setDescription("Manage your faction notification preferences.");
        setOptionalArgs("[status|invites|territory|tax|all] [on|off]");
        setRequiresPlayer(true);
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        try {
            final PlayerModel model = ctx.getRepos().players().findOrCreate(player.getUniqueId().toString());
            final String type = ctx.arg(0).isBlank() ? "status" : ctx.arg(0).toLowerCase();
            if ("status".equals(type)) {
                sendStatus(player, model);
                return;
            }
            final String value = ctx.arg(1).toLowerCase();
            if (!"on".equals(value) && !"off".equals(value)) {
                MsgUtil.sendKey(player, "custom.notify.usage",
                    "<red>Usage: /f notify [status|invites|territory|tax|all] [on|off]");
                return;
            }
            final boolean enabled = "on".equals(value);
            switch (type) {
                case "invites" -> model.setInviteNotifications(enabled);
                case "territory" -> model.setTerritoryTitles(enabled);
                case "tax" -> model.setBankTaxNotifications(enabled);
                case "all" -> {
                    model.setInviteNotifications(enabled);
                    model.setTerritoryTitles(enabled);
                    model.setBankTaxNotifications(enabled);
                }
                default -> {
                    MsgUtil.sendKey(player, "custom.notify.unknown-type",
                        "<red>Unknown notification type. Use: invites, territory, tax, all, status.");
                    return;
                }
            }
            ctx.getRepos().players().save(model);
            MsgUtil.sendKey(player, "custom.notify.updated",
                "<green>Notification setting updated: <white>{type}<green> -> <white>{value}",
                "type", type,
                "value", value);
            sendStatus(player, model);
        } catch (StorageException e) {
            MsgUtil.sendKey(player, "custom.notify.update-failed", "<red>Failed to update notification settings.");
        }
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return List.of("status", "invites", "territory", "tax", "all");
        }
        if (argIndex == 1) {
            return List.of("on", "off");
        }
        return List.of();
    }

    private void sendStatus(final Player player, final PlayerModel model) {
        MsgUtil.sendKey(player, "custom.notify.status-header", "<gold>Notification settings:");
        MsgUtil.sendKey(player, "custom.notify.status-line", "<gray>- {type}: <white>{value}",
            "type", "invites",
            "value", model.hasInviteNotifications() ? "on" : "off");
        MsgUtil.sendKey(player, "custom.notify.status-line", "<gray>- {type}: <white>{value}",
            "type", "territory",
            "value", model.hasTerritoryTitles() ? "on" : "off");
        MsgUtil.sendKey(player, "custom.notify.status-line", "<gray>- {type}: <white>{value}",
            "type", "tax",
            "value", model.hasBankTaxNotifications() ? "on" : "off");
    }
}
