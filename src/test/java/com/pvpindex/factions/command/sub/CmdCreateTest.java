package com.pvpindex.factions.command.sub;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.service.FactionService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import com.pvpindex.factions.command.StorageTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdCreate — /f create <name>")
class CmdCreateTest extends CommandTestBase {


    @Mock private FactionService factionService;
    @Mock private FactionModel faction;


    private CmdCreate cmd;
    private final UUID uuid = UUID.randomUUID();


    @BeforeEach
    void setUp() {
        cmd = new CmdCreate(factionService);
        when(player.getUniqueId()).thenReturn(uuid);
    }


    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        PredefinedConfigManager.setInstance(null);
    }


    @StorageTest
    @DisplayName("success — creates faction and confirms")
    void testCreateSuccess() {
        when(factionService.isInFaction(uuid)).thenReturn(false);
        when(factionService.createFaction(eq("Alpha"), eq(uuid))).thenReturn(Optional.of(faction));


        cmd.execute(ctx("Alpha"));


        verify(player).sendMessage(argThat(componentContains("created")));
    }


    @StorageTest
    @DisplayName("already in faction — rejected")
    void testAlreadyInFaction() {
        when(factionService.isInFaction(uuid)).thenReturn(true);


        cmd.execute(ctx("Alpha"));


        verify(player).sendMessage(argThat(componentContains("already in a faction")));
        verify(factionService, never()).createFaction(anyString(), any());
    }


    @StorageTest
    @DisplayName("name too short — rejected")
    void testNameTooShort() {
        when(factionService.isInFaction(uuid)).thenReturn(false);


        cmd.execute(ctx("AB"));


        verify(player).sendMessage(argThat(componentContains("3 and 32")));
        verify(factionService, never()).createFaction(anyString(), any());
    }


    @StorageTest
    @DisplayName("name too long — rejected")
    void testNameTooLong() {
        when(factionService.isInFaction(uuid)).thenReturn(false);


        cmd.execute(ctx("A".repeat(33)));


        verify(player).sendMessage(argThat(componentContains("3 and 32")));
    }


    @StorageTest
    @DisplayName("createFaction returns empty — name already taken message shown")
    void testCreateFails() {
        when(factionService.isInFaction(uuid)).thenReturn(false);
        when(factionService.createFaction(anyString(), eq(uuid))).thenReturn(Optional.empty());


        cmd.execute(ctx("Alpha"));


        verify(player).sendMessage(argThat(componentContains("already exists")));
    }


    @StorageTest
    @DisplayName("predefined enabled denies non-whitelisted faction names")
    void testPredefinedWhitelistBlocksUnknownName() throws IOException {
        final Path dir = Files.createTempDirectory("predefined-create-test");
        final PredefinedConfigManager manager = new PredefinedConfigManager(dir.toFile(), logger);
        manager.initialize();
        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dir.resolve("pre-defined.yml").toFile());
        cfg.set("enabled", true);
        cfg.set("factions.France.name", "France");
        cfg.save(dir.resolve("pre-defined.yml").toFile());
        manager.reload();
        PredefinedConfigManager.setInstance(manager);


        when(factionService.isInFaction(uuid)).thenReturn(false);
        cmd.execute(ctx("Denmark"));


        verify(player).sendMessage(argThat(componentContains("only create predefined factions")));
        verify(factionService, never()).createFaction(anyString(), any());
    }


    @StorageTest
    @DisplayName("createFaction throws — internal error message shown")
    void testCreateDbError() {
        when(factionService.isInFaction(uuid)).thenReturn(false);
        when(factionService.createFaction(anyString(), eq(uuid)))
            .thenThrow(new IllegalStateException("db error"));


        cmd.execute(ctx("Alpha"));


        verify(player).sendMessage(argThat(componentContains("internal error")));
    }


    @StorageTest
    @DisplayName("missing arg — usage shown")
    void testMissingArg() {
        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("Usage")));
        verify(factionService, never()).isInFaction(any());
    }


    @StorageTest
    @DisplayName("console sender — rejected")
    void testConsoleSender() {
        final CommandSender console = org.mockito.Mockito.mock(CommandSender.class);
        org.mockito.Mockito.lenient().when(console.hasPermission(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);


        cmd.execute(ctx(console, "Alpha"));


        verify(console).sendMessage(argThat(componentContains("player")));
        verify(factionService, never()).isInFaction(any());
    }


    @StorageTest
    @DisplayName("no permission — rejected")
    void testNoPermission() {
        when(player.hasPermission("factions.cmd.create")).thenReturn(false);


        cmd.execute(ctx("Alpha"));


        verify(player).sendMessage(argThat(componentContains("permission")));
        verify(factionService, never()).isInFaction(any());
    }
}
