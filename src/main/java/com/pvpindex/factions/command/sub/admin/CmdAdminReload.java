package com.pvpindex.factions.command.sub.admin;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.config.MessagesConfig;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.util.MsgUtil;
import java.io.File;
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
        final File messagesFile = new File(ctx.getPlugin().getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            ctx.getPlugin().saveResource("messages.yml", false);
        }
        final FileConfiguration msgCfgRaw = YamlConfiguration.loadConfiguration(messagesFile);
        MsgUtil.setMessagesConfig(new MessagesConfig(msgCfgRaw));
        final PredefinedConfigManager predefined = PredefinedConfigManager.getInstance();
        if (predefined != null) {
            predefined.reload();
        }
        MsgUtil.sendKey(ctx.getSender(), "admin.reload", "<green>Configuration reloaded.");
    }
}
