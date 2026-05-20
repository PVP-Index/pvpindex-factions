package com.pvpindex.factions.command.sub;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.config.MessagesConfig;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.util.MsgUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.bukkit.entity.Player;

/** {@code /f language} - view or change personal language. */
public final class CmdLanguage extends FactionCommand {

    public CmdLanguage() {
        super("language");
        setPermission("factions.cmd.language");
        setDescription("Show or set your language.");
        setOptionalArgs("[code|reset]");
        setRequiresPlayer(true);
        setAliases("lang", "locale");
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final MessagesConfig messages = MsgUtil.getMessagesConfig();
        if (messages == null) {
            MsgUtil.send(player, MsgUtil.message(player, "language.system-unavailable",
                "<red>Language system is not available."));
            return;
        }
        final PlayerModel pm;
        try {
            pm = ctx.getRepos().players().findOrCreate(player.getUniqueId().toString());
        } catch (StorageException e) {
            MsgUtil.send(player, MsgUtil.message(player, "language.profile-load-failed",
                "<red>Unable to load your profile right now."));
            return;
        }

        if (ctx.getArgs().isEmpty()) {
            sendStatus(player, messages, pm.getLocale());
            return;
        }

        final String input = ctx.arg(0);
        if ("reset".equalsIgnoreCase(input)) {
            pm.setLocale(null);
            savePlayer(ctx, player, pm);
            MsgUtil.send(player, MsgUtil.message(player, "language.reset-success",
                "<green>Your language has been reset to server default."));
            sendStatus(player, messages, null);
            return;
        }

        final String normalized = MessagesConfig.normalizeLocale(input);
        if (!messages.isSupportedLocale(normalized)) {
            MsgUtil.send(player, MsgUtil.replace(
                MsgUtil.message(player, "language.invalid-code",
                    "<red>Unsupported language code: <white>{code}</white>."),
                "code", input));
            sendStatus(player, messages, pm.getLocale());
            return;
        }
        pm.setLocale(normalized);
        savePlayer(ctx, player, pm);
        MsgUtil.send(player, MsgUtil.replace(
            MsgUtil.message(player, "language.set-success",
                "<green>Language updated to <white>{code}</white>."),
            "code", normalized));
        sendStatus(player, messages, normalized);
    }

    private void savePlayer(final CommandContext ctx, final Player player, final PlayerModel model) {
        try {
            ctx.getRepos().players().save(model);
        } catch (StorageException e) {
            MsgUtil.send(player, MsgUtil.message(player, "language.save-failed",
                "<red>Could not save your language preference."));
        }
    }

    private void sendStatus(final Player player, final MessagesConfig messages, final String override) {
        final String defaultLocale = messages.getDefaultLocale();
        final String current = override == null || override.isBlank() ? defaultLocale : override;
        MsgUtil.send(player, MsgUtil.replace(
            MsgUtil.message(player, "language.current",
                "<gold>Current language: <white>{code}</white>"),
            "code", current));
        MsgUtil.send(player, MsgUtil.replace(
            MsgUtil.message(player, "language.default",
                "<gray>Server default: <white>{code}</white>"),
            "code", defaultLocale));
        final List<String> locales = new ArrayList<>(messages.getAvailableLocales());
        locales.sort(Comparator.naturalOrder());
        MsgUtil.send(player, MsgUtil.replace(
            MsgUtil.message(player, "language.available",
                "<gray>Available: <white>{list}</white>"),
            "list", String.join(", ", locales)));
        MsgUtil.send(player, MsgUtil.message(player, "language.usage",
            "<gray>Usage: <white>/f language <code></white> or <white>/f language reset</white>"));
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex != 0) {
            return List.of();
        }
        final MessagesConfig messages = MsgUtil.getMessagesConfig();
        if (messages == null) {
            return List.of("reset");
        }
        final Set<String> locales = messages.getAvailableLocales();
        final List<String> out = new ArrayList<>(locales);
        out.add("reset");
        return out;
    }
}
