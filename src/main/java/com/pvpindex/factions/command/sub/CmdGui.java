package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.config.GuiConfig;
import com.pvpindex.factions.gui.FactionsGuiManager;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/** {@code /f gui [menu]}. */
public final class CmdGui extends FactionCommand {

    private final FactionsGuiManager guiManager;
    private final GuiConfig guiConfig;

    public CmdGui(final FactionsGuiManager guiManager, final GuiConfig guiConfig) {
        super("gui");
        setAliases("menu");
        setPermission("factions.cmd.gui");
        setDescription("Open the factions GUI.");
        setOptionalArgs("[menu]");
        setRequiresPlayer(true);
        this.guiManager = guiManager;
        this.guiConfig = guiConfig;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = ctx.requirePlayer();
        if (player == null) {
            return;
        }
        final String menu = ctx.arg(0).isBlank() ? guiConfig.getDefaultMenu() : ctx.arg(0);
        if (!guiManager.openMenu(player, menu)) {
            MsgUtil.send(player, "<red>Unknown GUI menu: <yellow>" + menu);
        }
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex != 0) {
            return List.of();
        }
        final ConfigurationSection menus = guiConfig.raw().getConfigurationSection("gui.menus");
        if (menus == null) {
            return List.of();
        }
        return menus.getKeys(false).stream().toList();
    }
}
