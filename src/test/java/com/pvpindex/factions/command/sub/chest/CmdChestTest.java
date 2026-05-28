package com.pvpindex.factions.command.sub.chest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.engine.EngineTeamChests;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.TeamChestService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdChest")
class CmdChestTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private TeamChestService chestService;
    @Mock private EngineTeamChests engine;
    @Mock private FactionsConfig cfg;
    @Mock private FactionModel faction;

    private CmdChest cmd;
    private final UUID uuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cmd = new CmdChest(factionService, chestService, engine, cfg);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn("f1");
        when(cfg.getDefaultTeamChestName()).thenReturn("default");
        when(cfg.getMaxTeamChests()).thenReturn(1);
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(engine.openChest(player, "f1", "default", "Faction Chest: default")).thenReturn(true);
        when(engine.openChest(player, "f1", "named", "Faction Chest: named")).thenReturn(true);
    }

    @StorageTest
    void defaultOpenWorks() {
        when(chestService.ensureChestExistsForOpen("f1", "default")).thenReturn(Optional.of("default"));
        cmd.execute(ctx());
        verify(engine).openChest(player, "f1", "default", "Faction Chest: default");
    }

    @StorageTest
    void listShowsNames() {
        when(chestService.getChestNames("f1")).thenReturn(List.of("default"));
        cmd.execute(ctx("list"));
        verify(player).sendMessage(org.mockito.ArgumentMatchers.argThat(componentContains("Team Chests")));
    }

    @StorageTest
    void createRequiresOfficer() {
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(false);
        cmd.execute(ctx("create", "named"));
        verify(chestService, never()).createChest(anyString(), anyString());
    }

    @StorageTest
    void openNamedAutoCreates() {
        when(chestService.ensureChestExistsForOpen("f1", "named")).thenReturn(Optional.of("named"));
        cmd.execute(ctx("open", "named"));
        verify(engine).openChest(player, "f1", "named", "Faction Chest: named");
    }
}
