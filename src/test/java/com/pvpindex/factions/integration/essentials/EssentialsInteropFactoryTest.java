package com.pvpindex.factions.integration.essentials;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

import com.earth2me.essentials.IEssentials;
import com.pvpindex.factions.config.FactionsConfig;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EssentialsInteropFactoryTest {

    @Mock private Plugin owner;
    @Mock private Server server;
    @Mock private PluginManager pluginManager;
    @Mock private FactionsConfig config;
    @Mock private IEssentials essentials;

    private final Logger logger = Logger.getLogger("test");
    private final PluginDescriptionFile essentialsDesc;

    EssentialsInteropFactoryTest() throws InvalidDescriptionException {
        essentialsDesc = new PluginDescriptionFile(
            "Essentials", "2.21.0", "com.earth2me.essentials.Essentials");
    }

    @Test
    void returnsEssentialsXInteropWhenPresentAndEnabled() {
        when(config.isEssentialsXEnabled()).thenReturn(true);
        when(owner.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(pluginManager.getPlugin("Essentials")).thenReturn(essentials);
        when(essentials.getDescription()).thenReturn(essentialsDesc);

        final EssentialsInterop interop = EssentialsInteropFactory.create(owner, config, logger);

        assertInstanceOf(EssentialsXInterop.class, interop);
    }

    @Test
    void returnsNoopWhenPluginMissing() {
        when(config.isEssentialsXEnabled()).thenReturn(true);
        when(owner.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(pluginManager.getPlugin("Essentials")).thenReturn(null);

        final EssentialsInterop interop = EssentialsInteropFactory.create(owner, config, logger);

        assertInstanceOf(NoopEssentialsInterop.class, interop);
    }

    @Test
    void returnsNoopWhenConfigDisabled() {
        when(config.isEssentialsXEnabled()).thenReturn(false);

        final EssentialsInterop interop = EssentialsInteropFactory.create(owner, config, logger);

        assertInstanceOf(NoopEssentialsInterop.class, interop);
    }

    @Test
    void returnsNoopWhenPluginNotIEssentials() {
        final Plugin nonEssentials = org.mockito.Mockito.mock(Plugin.class);
        when(config.isEssentialsXEnabled()).thenReturn(true);
        when(owner.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(pluginManager.getPlugin("Essentials")).thenReturn(nonEssentials);

        final EssentialsInterop interop = EssentialsInteropFactory.create(owner, config, logger);

        assertInstanceOf(NoopEssentialsInterop.class, interop);
    }
}
