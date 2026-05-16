package com.pvpindex.factions.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.repository.FactionRepository;
import com.pvpindex.factions.data.repository.PlayerRepository;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Smoke tests for {@link EngineChat}'s dual-listener design.
 *
 * <p>Both the Paper ({@code AsyncChatEvent}) and legacy ({@code AsyncPlayerChatEvent}) inner
 * listeners are exercised directly via reflection so the tests do not depend on a live Bukkit
 * server or plugin manager.
 */
@SuppressWarnings("deprecation")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EngineChat listener smoke tests")
class EngineChatListenerTest {

    @Mock private Repositories repos;
    @Mock private PlayerRepository playerRepo;
    @Mock private FactionRepository factionRepo;
    @Mock private FactionsConfig config;
    @Mock private Player player;

    private static final Logger LOGGER = Logger.getLogger("test");
    private static final UUID PLAYER_UUID = UUID.randomUUID();
    private static final String FACTION_ID = UUID.randomUUID().toString();
    private static final String FACTION_NAME = "Gyvex";

    private EngineChat engine;

    @BeforeEach
    void setUp() {
        when(repos.players()).thenReturn(playerRepo);
        when(repos.factions()).thenReturn(factionRepo);
        when(player.getUniqueId()).thenReturn(PLAYER_UUID);
        engine = new EngineChat(repos, config, LOGGER);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Instantiates a private inner listener class by name.
     * Non-static inner classes receive the enclosing {@link EngineChat} instance
     * as their sole constructor parameter.
     */
    private Listener makeListener(final String simpleName) throws Exception {
        for (final Class<?> inner : EngineChat.class.getDeclaredClasses()) {
            if (inner.getSimpleName().equals(simpleName)) {
                final Constructor<?> ctor = inner.getDeclaredConstructor(EngineChat.class);
                ctor.setAccessible(true);
                return (Listener) ctor.newInstance(engine);
            }
        }
        throw new IllegalArgumentException("Inner class not found: " + simpleName);
    }

    /** Reflectively invokes {@code onChat(eventType)} on the given listener. */
    private void fireOnChat(
            final Listener listener, final Class<?> eventType, final Object event)
            throws Exception {
        final Method method = listener.getClass().getDeclaredMethod("onChat", eventType);
        method.setAccessible(true);
        method.invoke(listener, event);
    }

    /**
     * Constructs a real {@link AsyncChatEvent} backed by the {@code player} mock.
     * {@code AsyncChatEvent} is final and cannot be mocked; constructing it directly
     * is the only reliable approach.
     */
    private AsyncChatEvent makePaperEvent(final ChatRenderer initial) {
        return new AsyncChatEvent(
                false, player, new HashSet<>(), initial,
                Component.text("hello"), Component.text("hello"), null);
    }

    /**
     * Constructs a real {@link AsyncPlayerChatEvent} backed by the {@code player} mock.
     * {@code PlayerEvent#getPlayer()} is a final method; constructing the event directly
     * lets it return the mock without stubbing.
     */
    private AsyncPlayerChatEvent makeLegacyEvent() {
        return new AsyncPlayerChatEvent(false, player, "hello", new HashSet<>());
    }

    private void stubPlayerInFaction() throws StorageException {
        final PlayerModel pm = new PlayerModel(PLAYER_UUID.toString());
        pm.setFactionId(FACTION_ID);
        when(playerRepo.find(PLAYER_UUID.toString())).thenReturn(Optional.of(pm));
        final FactionModel faction = new FactionModel(FACTION_ID);
        faction.setName(FACTION_NAME);
        when(factionRepo.find(FACTION_ID)).thenReturn(Optional.of(faction));
    }

    // =========================================================================
    // PaperChatListener  (Paper / AsyncChatEvent path)
    // =========================================================================

    @Test
    @DisplayName("Paper: renderer unchanged when chat format is disabled")
    void paperSkipsRendererWhenFormatDisabled() throws Exception {
        when(config.isChatFormatEnabled()).thenReturn(false);
        final ChatRenderer initial = (src, dn, msg, v) -> msg;
        final AsyncChatEvent event = makePaperEvent(initial);
        final Listener listener = makeListener("PaperChatListener");
        fireOnChat(listener, AsyncChatEvent.class, event);
        assertSame(initial, event.renderer());
    }

    @Test
    @DisplayName("Paper: renderer replaced for player with no faction")
    void paperSetsRendererForFactionlessPlayer() throws Exception {
        when(config.isChatFormatEnabled()).thenReturn(true);
        when(playerRepo.find(PLAYER_UUID.toString())).thenReturn(Optional.empty());
        final ChatRenderer initial = (src, dn, msg, v) -> msg;
        final AsyncChatEvent event = makePaperEvent(initial);
        final Listener listener = makeListener("PaperChatListener");
        fireOnChat(listener, AsyncChatEvent.class, event);
        assertNotSame(initial, event.renderer());
    }

    @Test
    @DisplayName("Paper: renderer replaced and faction looked up for faction member")
    void paperSetsRendererForFactionMember() throws Exception {
        when(config.isChatFormatEnabled()).thenReturn(true);
        stubPlayerInFaction();
        final ChatRenderer initial = (src, dn, msg, v) -> msg;
        final AsyncChatEvent event = makePaperEvent(initial);
        final Listener listener = makeListener("PaperChatListener");
        fireOnChat(listener, AsyncChatEvent.class, event);
        assertNotSame(initial, event.renderer());
        verify(factionRepo).find(FACTION_ID);
    }

    // =========================================================================
    // LegacyChatListener  (Spigot / AsyncPlayerChatEvent path)
    // =========================================================================

    @Test
    @DisplayName("Legacy: format unchanged when chat format is disabled")
    void legacySkipsSetFormatWhenFormatDisabled() throws Exception {
        when(config.isChatFormatEnabled()).thenReturn(false);
        final AsyncPlayerChatEvent event = makeLegacyEvent();
        final String initialFormat = event.getFormat();
        final Listener listener = makeListener("LegacyChatListener");
        fireOnChat(listener, AsyncPlayerChatEvent.class, event);
        assertEquals(initialFormat, event.getFormat());
    }

    @Test
    @DisplayName("Legacy: format contains Bukkit placeholders for player with no faction")
    void legacySetsFormatForFactionlessPlayer() throws Exception {
        when(config.isChatFormatEnabled()).thenReturn(true);
        when(playerRepo.find(PLAYER_UUID.toString())).thenReturn(Optional.empty());
        final AsyncPlayerChatEvent event = makeLegacyEvent();
        final Listener listener = makeListener("LegacyChatListener");
        fireOnChat(listener, AsyncPlayerChatEvent.class, event);
        assertTrue(event.getFormat().contains("%s"), "format must contain %s placeholder");
    }

    @Test
    @DisplayName("Legacy: format contains faction name for faction member")
    void legacySetsFormatContainingFactionNameForMember() throws Exception {
        when(config.isChatFormatEnabled()).thenReturn(true);
        stubPlayerInFaction();
        final AsyncPlayerChatEvent event = makeLegacyEvent();
        final Listener listener = makeListener("LegacyChatListener");
        fireOnChat(listener, AsyncPlayerChatEvent.class, event);
        final String format = event.getFormat();
        assertTrue(format.contains(FACTION_NAME), "format must contain faction name");
        assertTrue(format.contains("%s"), "format must contain %s placeholder");
    }
}
