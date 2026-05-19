package com.pvpindex.factions.integration.discordsrv;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiscordSrvNotifierTest {

    @Mock private PluginManager pluginManager;

    private final Logger logger = Logger.getLogger("test");

    @Test
    void isEnabledFalseBeforeSetup() {
        assertFalse(new DiscordSrvNotifier(logger, "").isEnabled());
    }

    @Test
    void sendMessageNoopsWhenNotEnabled() {
        // must not throw even when disabled
        new DiscordSrvNotifier(logger, "").sendMessage("test");
    }

    @Test
    void setupReturnsFalseWhenDiscordSrvAbsent() {
        when(pluginManager.getPlugin("DiscordSRV")).thenReturn(null);

        final DiscordSrvNotifier notifier = new DiscordSrvNotifier(logger, "");
        assertFalse(notifier.setup(pluginManager));
        assertFalse(notifier.isEnabled());
    }

    @Test
    void setupReturnsFalseWhenClassNotOnClasspath() {
        // DiscordSRV is a softdep not on the test classpath,
        // so Class.forName(DSRV_CLASS) will throw ClassNotFoundException.
        final Plugin fakePlugin = Mockito.mock(Plugin.class);
        when(pluginManager.getPlugin("DiscordSRV")).thenReturn(fakePlugin);

        final DiscordSrvNotifier notifier = new DiscordSrvNotifier(logger, "");
        assertFalse(notifier.setup(pluginManager));
        assertFalse(notifier.isEnabled());
    }
}
