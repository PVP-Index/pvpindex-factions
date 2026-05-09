package com.pvpindex.factions.command.sub;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.integration.worldguard.TerritoryGuard;
import com.pvpindex.factions.service.FactionService;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CmdSetHomeTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private TerritoryGuard territoryGuard;
    @Mock private FactionModel faction;
    @Mock private Location location;

    private CmdSetHome cmd;
    private final UUID uuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cmd = new CmdSetHome(factionService, territoryGuard);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getLocation()).thenReturn(location);
        when(territoryGuard.canModifyTerritory(player, location)).thenReturn(true);
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
    }

    @Test
    void setsHome() {
        when(factionService.setFactionHome(uuid, location)).thenReturn(true);
        cmd.execute(ctx());
        verify(player).sendMessage(argThat(componentContains("home set")));
    }
}
