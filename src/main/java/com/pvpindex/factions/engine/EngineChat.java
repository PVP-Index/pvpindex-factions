package com.pvpindex.factions.engine;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

/**
 * Formats player chat messages with a faction prefix.
 *
 * <p>On Paper and Folia servers the renderer is injected via reflection so that
 * maven-shade-plugin never sees any {@code net.kyori.adventure} type references in this
 * class's bytecode.  Direct imports would be rewritten to the shaded
 * {@code com.pvpindex.lib.adventure} namespace, causing the lambda or inner-class method
 * descriptors to mismatch Paper's runtime {@code ChatRenderer$ViewerUnaware} interface and
 * producing {@code AbstractMethodError} or {@code ClassCastException} on every chat event.
 *
 * <p>Falls back to the deprecated {@link AsyncPlayerChatEvent} format-string API on plain
 * Spigot / Bukkit where the Paper chat API is absent.
 */
public final class EngineChat {

    private static final boolean PAPER_CHAT;

    static {
        boolean paperChat = false;
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            paperChat = true;
        } catch (ClassNotFoundException ignored) {
            // Running on Spigot/Bukkit — fall back to legacy listener.
        }
        PAPER_CHAT = paperChat;
    }

    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;

    public EngineChat(
            final Repositories repos, final FactionsConfig config, final Logger logger) {
        this.repos = repos;
        this.config = config;
        this.logger = logger;
    }

    /** Registers the appropriate chat listener for the current server platform. */
    public void register(final Plugin plugin) {
        if (PAPER_CHAT) {
            try {
                org.bukkit.Bukkit.getPluginManager()
                    .registerEvents(new PaperChatListener(), plugin);
            } catch (ReflectiveOperationException e) {
                logger.log(Level.SEVERE,
                    "Chat renderer init failed; faction chat tags disabled on Paper", e);
            }
        } else {
            org.bukkit.Bukkit.getPluginManager().registerEvents(new LegacyChatListener(), plugin);
        }
    }

    private String buildFactionTag(final Player player) throws StorageException {
        final Optional<PlayerModel> pm = repos.players().find(player.getUniqueId().toString());
        if (pm.isPresent() && pm.get().isInFaction()) {
            final Optional<FactionModel> faction = repos.factions().find(pm.get().getFactionId());
            return faction.map(FactionModel::getName)
                .map(name -> "<gray>[<white>" + name + "<gray>]</gray> ")
                .orElse("");
        }
        return "";
    }

    /**
     * Paper / Folia chat listener.
     *
     * <p>All adventure interactions are performed through reflection so that shade cannot
     * rewrite adventure type descriptors in this class's bytecode.  Reflection handles and
     * the MiniMessage instance are resolved once in the constructor and cached.
     */
    private final class PaperChatListener implements Listener {

        private final Class<?> vuIface;
        private final Method appendMethod;
        private final Method deserialize;
        private final Method empty;
        private final Method viewerUnawareFactory;
        private final Method rendererSetter;
        private final Object miniMessage;
        private final Object sep;
        private final Object emptyTagResolvers;

        PaperChatListener() throws ReflectiveOperationException {
            final ClassLoader cl = AsyncChatEvent.class.getClassLoader();
            // String.valueOf is a runtime call, so javac cannot constant-fold the
            // concatenations below.  Without this, the full "net.kyori.adventure.*"
            // literals would end up in the constant pool and maven-shade would rewrite
            // them to "com.pvpindex.lib.adventure.*" — a package that does not exist
            // in the plugin JAR because adventure is a provided (server-bundled) dep.
            final String adv = String.valueOf("net.kyori");
            vuIface =
                Class.forName("io.papermc.paper.chat.ChatRenderer$ViewerUnaware", true, cl);
            final Class<?> crClass =
                Class.forName("io.papermc.paper.chat.ChatRenderer", true, cl);
            final Class<?> mmClass =
                Class.forName(adv + ".adventure.text.minimessage.MiniMessage", true, cl);
            final Class<?> compClass =
                Class.forName(adv + ".adventure.text.Component", true, cl);
            final Class<?> compLikeClass =
                Class.forName(adv + ".adventure.text.ComponentLike", true, cl);
            final Class<?> tagResolverClass = Class.forName(
                adv + ".adventure.text.minimessage.tag.resolver.TagResolver", true, cl);
            miniMessage = mmClass.getMethod("miniMessage").invoke(null);
            empty = compClass.getMethod("empty");
            appendMethod = compClass.getMethod("append", compLikeClass);
            viewerUnawareFactory = crClass.getMethod("viewerUnaware", vuIface);
            rendererSetter = AsyncChatEvent.class.getMethod("renderer", crClass);
            // Adventure 4.20 removed single-param deserialize(String) from MiniMessage.
            // Use the explicitly declared default deserialize(String, TagResolver...) instead.
            emptyTagResolvers = Array.newInstance(tagResolverClass, 0);
            deserialize = mmClass.getMethod("deserialize", String.class, emptyTagResolvers.getClass());
            sep = deserialize.invoke(miniMessage, "<gray>: <white>", emptyTagResolvers);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onChat(final AsyncChatEvent event) {
            if (!config.isChatFormatEnabled()) {
                return;
            }
            try {
                final String tag = buildFactionTag(event.getPlayer());
                final Object prefix = tag.isEmpty()
                    ? empty.invoke(null)
                    : deserialize.invoke(miniMessage, tag, emptyTagResolvers);
                final Object viewerUnaware = Proxy.newProxyInstance(
                    vuIface.getClassLoader(),
                    new Class<?>[]{vuIface},
                    new RenderHandler(prefix, sep, appendMethod));
                rendererSetter.invoke(event, viewerUnawareFactory.invoke(null, viewerUnaware));
            } catch (StorageException e) {
                logger.log(Level.WARNING,
                    "Failed to format chat for " + event.getPlayer().getName(), e);
            } catch (ReflectiveOperationException e) {
                logger.log(Level.WARNING, "Chat renderer reflection failed", e);
            }
        }
    }

    /** InvocationHandler for the {@code ChatRenderer.ViewerUnaware} proxy. */
    private static final class RenderHandler implements InvocationHandler {

        private final Object prefix;
        private final Object sep;
        private final Method appendMethod;

        RenderHandler(final Object prefix, final Object sep, final Method appendMethod) {
            this.prefix = prefix;
            this.sep = sep;
            this.appendMethod = appendMethod;
        }

        @Override
        public Object invoke(
                final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            // render(Player source, Component displayName, Component message)
            Object result = appendMethod.invoke(prefix, args[1]);
            result = appendMethod.invoke(result, sep);
            return appendMethod.invoke(result, args[2]);
        }
    }

    /**
     * Fallback chat listener for Spigot and Bukkit servers.
     *
     * <p>Uses the deprecated {@link AsyncPlayerChatEvent} format string since Paper's
     * Adventure-based chat API is not available on plain Spigot.
     */
    private final class LegacyChatListener implements Listener {

        @SuppressWarnings({"deprecation", "removal"})
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onChat(final AsyncPlayerChatEvent event) {
            if (!config.isChatFormatEnabled()) {
                return;
            }
            try {
                final String tag = buildFactionTag(event.getPlayer());
                event.setFormat(tag + "%s: %s");
            } catch (StorageException e) {
                logger.log(Level.WARNING,
                    "Failed to format chat for " + event.getPlayer().getName(), e);
            }
        }
    }
}
