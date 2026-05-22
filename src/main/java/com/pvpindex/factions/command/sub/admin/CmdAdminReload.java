package com.pvpindex.factions.command.sub.admin;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.config.MessagesConfig;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.util.MsgUtil;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/** {@code /fa reload}. */
public final class CmdAdminReload extends FactionCommand {

    public CmdAdminReload() {
        super("reload");
        setPermission("factions.admin");
        setDescription("Reload plugin config from disk.");
    }

    @Override
    protected void perform(final CommandContext ctx) {
        ctx.getPlugin().reloadConfig();
        String defaultLocale = "en";
        if (ctx.getPlugin().getConfig() != null) {
            final String configured = ctx.getPlugin().getConfig().getString("factions.language.default", "en");
            if (configured != null && !configured.isBlank()) {
                defaultLocale = configured;
            }
        } else if (ctx.getConfig() != null && ctx.getConfig().getDefaultLanguage() != null) {
            defaultLocale = ctx.getConfig().getDefaultLanguage();
        }
        final MessagesConfig messagesConfig = loadMessagesConfig(ctx, defaultLocale);
        messagesConfig.setRepositories(ctx.getRepos());
        MsgUtil.setMessagesConfig(messagesConfig);
        final PredefinedConfigManager predefined = PredefinedConfigManager.getInstance();
        if (predefined != null) {
            predefined.reload();
        }
        MsgUtil.sendKey(ctx.getSender(), "admin.reload", "<green>Configuration reloaded.");
    }

    private MessagesConfig loadMessagesConfig(final CommandContext ctx, final String defaultLocale) {
        final File dataFolder = ctx.getPlugin().getDataFolder();
        final File messagesDir = new File(dataFolder, "messages");
        if (!messagesDir.exists()) {
            messagesDir.mkdirs();
        }
        final Set<String> shippedLocales = Set.of("en", "es", "de", "fr", "pt-BR", "zh", "ru", "ja");
        for (final String locale : shippedLocales) {
            final String name = "messages/messages_" + locale + ".yml";
            final File dest = new File(dataFolder, name);
            if (!dest.exists()) {
                ctx.getPlugin().saveResource(name, false);
            }
        }
        final File legacyMessages = new File(dataFolder, "messages.yml");
        if (!legacyMessages.exists()) {
            ctx.getPlugin().saveResource("messages.yml", false);
        }
        final Map<String, FileConfiguration> bundles = new LinkedHashMap<>();
        final File[] bundleFiles = messagesDir.listFiles((dir, name) -> name.startsWith("messages_") && name.endsWith(".yml"));
        if (bundleFiles != null) {
            for (final File file : bundleFiles) {
                final String raw = file.getName().substring("messages_".length(), file.getName().length() - 4);
                bundles.put(MessagesConfig.normalizeLocale(raw), YamlConfiguration.loadConfiguration(file));
            }
        }
        if (!bundles.containsKey("en")) {
            bundles.put("en", YamlConfiguration.loadConfiguration(legacyMessages));
        }
        return new MessagesConfig(bundles, defaultLocale);
    }
}
