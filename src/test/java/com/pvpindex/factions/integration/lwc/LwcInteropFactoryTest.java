package com.pvpindex.factions.integration.lwc;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LwcInteropFactoryTest {

    @Mock private Plugin owner;
    @Mock private Server server;
    @Mock private PluginManager pluginManager;
    @Mock private FactionsConfig config;
    @Mock private Repositories repos;
    @Mock private Plugin lwcx;

    private final Logger logger = Logger.getLogger("test");

    @Test
    void returnsNoopWhenConfigDisabled() {
        when(config.isLwcEnabled()).thenReturn(false);

        final LwcInterop interop = LwcInteropFactory.create(owner, config, repos, logger);

        assertInstanceOf(NoopLwcInterop.class, interop);
    }

    @Test
    void returnsNoopWhenPluginMissing() {
        when(config.isLwcEnabled()).thenReturn(true);
        when(owner.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(pluginManager.getPlugin("LWC")).thenReturn(null);
        when(pluginManager.getPlugin("LWCX")).thenReturn(null);

        final LwcInterop interop = LwcInteropFactory.create(owner, config, repos, logger);

        assertInstanceOf(NoopLwcInterop.class, interop);
    }

    @Test
    void returnsLwcxInteropWhenPluginPresentAndEnabled() {
        when(config.isLwcEnabled()).thenReturn(true);
        when(owner.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(pluginManager.getPlugin("LWC")).thenReturn(null);
        when(pluginManager.getPlugin("LWCX")).thenReturn(lwcx);

        final LwcInterop interop = LwcInteropFactory.create(owner, config, repos, logger);

        assertInstanceOf(LwcxInterop.class, interop);
    }
}
