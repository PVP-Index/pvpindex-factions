package com.pvpindex.factions.integration.discordsrv;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

/**
 * Reflection-based notifier for DiscordSRV v1.
 *
 * <p>Safe to load unconditionally — no direct DiscordSRV or JDA imports.
 * All DiscordSRV classes are resolved at runtime via reflection so startup
 * succeeds when DiscordSRV is absent.
 *
 * <p>Call {@link #setup()} once at startup; guard all message sends with
 * {@link #isEnabled()}.
 */
public final class DiscordSrvNotifier {

    static final String DSRV_CLASS = "github.scarsz.discordsrv.DiscordSRV";

    private final Logger logger;
    private final String channelId;
    private boolean available;

    public DiscordSrvNotifier(final Logger logger, final String channelId) {
        this.logger = logger;
        this.channelId = channelId;
    }

    /**
     * Verifies that DiscordSRV is loaded and its main class is accessible.
     *
     * @return {@code true} if DiscordSRV is present and ready
     */
    public boolean setup() {
        return setup(Bukkit.getPluginManager());
    }

    /**
     * Package-private overload used in unit tests to inject a mock {@link PluginManager}.
     */
    boolean setup(final PluginManager pluginManager) {
        if (pluginManager.getPlugin("DiscordSRV") == null) {
            return false;
        }
        try {
            Class.forName(DSRV_CLASS);
            available = true;
            return true;
        } catch (ClassNotFoundException e) {
            logger.warning("DiscordSRV plugin found but class not loadable: " + e.getMessage());
            return false;
        }
    }

    /** @return {@code true} if DiscordSRV was found and {@link #setup()} succeeded. */
    public boolean isEnabled() {
        return available;
    }

    /**
     * Posts a plain-text or Discord-markdown message to the configured channel.
     *
     * <p>Falls back to DiscordSRV's main linked channel when {@code channel-id} is
     * empty. Silently no-ops if DiscordSRV is absent or the channel cannot be
     * resolved.
     *
     * @param message plain-text or Discord-markdown string (no MiniMessage tags)
     */
    public void sendMessage(final String message) {
        if (!available) {
            return;
        }
        try {
            final Class<?> dsrvClass = Class.forName(DSRV_CLASS);
            final Object dsrv = dsrvClass.getMethod("getPlugin").invoke(null);
            final Object channel = resolveChannel(dsrv);
            if (channel == null) {
                return;
            }
            // JDA 4: MessageChannel.sendMessage(CharSequence) returns a RestAction
            final Method sendMsg = channel.getClass().getMethod("sendMessage", CharSequence.class);
            final Object restAction = sendMsg.invoke(channel, message);
            restAction.getClass().getMethod("queue").invoke(restAction);
        } catch (Exception e) {
            logger.warning("DiscordSRV message send failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private Object resolveChannel(final Object dsrv) throws Exception {
        if (channelId != null && !channelId.isBlank()) {
            final Object jda = dsrv.getClass().getMethod("getJda").invoke(dsrv);
            if (jda != null) {
                final Object channel = jda.getClass()
                    .getMethod("getTextChannelById", String.class)
                    .invoke(jda, channelId);
                if (channel != null) {
                    return channel;
                }
                logger.warning("DiscordSRV: channel '" + channelId + "' not found — using main channel.");
            }
        }
        return dsrv.getClass().getMethod("getMainTextChannel").invoke(dsrv);
    }
}
