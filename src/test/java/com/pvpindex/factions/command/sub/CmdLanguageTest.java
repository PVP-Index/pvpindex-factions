package com.pvpindex.factions.command.sub;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.config.MessagesConfig;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.repository.PlayerRepository;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CmdLanguage - /f language")
class CmdLanguageTest extends CommandTestBase {

    @Mock
    private PlayerRepository playerRepository;

    private CmdLanguage cmd;
    private PlayerModel playerModel;

    @BeforeEach
    void setUp() throws Exception {
        cmd = new CmdLanguage();
        playerModel = new PlayerModel("123e4567-e89b-12d3-a456-426614174000");
        when(repos.players()).thenReturn(playerRepository);
        when(player.getUniqueId()).thenReturn(java.util.UUID.fromString(playerModel.getId()));
        when(playerRepository.findOrCreate(playerModel.getId())).thenReturn(playerModel);
        lenient().when(config.isLanguagePlayerOverrideEnabled()).thenReturn(true);
        lenient().when(config.isLanguageCommandOpensGui()).thenReturn(false);
        lenient().when(config.isLanguageCommandOpensGuiAfterSet()).thenReturn(false);
        lenient().when(config.getLanguageVisibleLocales()).thenReturn(java.util.List.of());

        final YamlConfiguration en = new YamlConfiguration();
        en.set("general.no-permission", "No permission");
        final MessagesConfig messages = new MessagesConfig(Map.of("en", en, "de", en, "fr", en), "en");
        MsgUtil.setMessagesConfig(messages);
    }

    @StorageTest
    @DisplayName("shows status with no args")
    void showsStatus() {
        cmd.execute(ctx());
        verify(player).sendMessage(argThat(componentContains("Current language")));
    }

    @StorageTest
    @DisplayName("sets locale when supported")
    void setsLocale() throws Exception {
        cmd.execute(ctx("de"));
        verify(playerRepository).save(playerModel);
        verify(player).sendMessage(argThat(componentContains("Language updated")));
    }

    @StorageTest
    @DisplayName("resets locale")
    void resetsLocale() throws Exception {
        playerModel.setLocale("de");
        cmd.execute(ctx("reset"));
        verify(playerRepository).save(playerModel);
        verify(player, atLeastOnce()).sendMessage(argThat(componentContains("reset")));
    }
}
