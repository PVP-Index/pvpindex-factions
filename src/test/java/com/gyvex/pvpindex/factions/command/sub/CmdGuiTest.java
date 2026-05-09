package com.gyvex.pvpindex.factions.command.sub;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gyvex.pvpindex.factions.command.CommandTestBase;
import com.gyvex.pvpindex.factions.config.GuiConfig;
import com.gyvex.pvpindex.factions.gui.FactionsGuiManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CmdGui - /f gui [menu]")
class CmdGuiTest extends CommandTestBase {

    @Mock private FactionsGuiManager guiManager;

    private CmdGui cmd;

    @BeforeEach
    void setUp() {
        final YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("gui.default-menu", "main");
        yaml.set("gui.menus.main.title", "Main");
        yaml.set("gui.menus.admin.title", "Admin");
        final GuiConfig guiConfig = new GuiConfig(yaml);
        cmd = new CmdGui(guiManager, guiConfig);
    }

    @Test
    @DisplayName("opens default menu without args")
    void opensDefaultWithoutArgs() {
        when(guiManager.openMenu(player, "main")).thenReturn(true);
        cmd.execute(ctx());
        verify(guiManager).openMenu(player, "main");
    }

    @Test
    @DisplayName("opens requested menu")
    void opensRequestedMenu() {
        when(guiManager.openMenu(player, "admin")).thenReturn(true);
        cmd.execute(ctx("admin"));
        verify(guiManager).openMenu(player, "admin");
    }
}
