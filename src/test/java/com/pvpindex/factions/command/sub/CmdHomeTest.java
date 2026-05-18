package com.pvpindex.factions.command.sub;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.integration.essentials.EssentialsInterop;
import com.pvpindex.factions.service.FactionService;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CmdHomeTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private FactionModel faction;
    @Mock private World world;
    @Mock private EssentialsInterop essentialsInterop;

    private CmdHome cmd;
    private final UUID uuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cmd = new CmdHome(factionService, essentialsInterop);
        when(player.getUniqueId()).thenReturn(uuid);
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
    }

    @Test
    void teleportsToHome() {
        final Location home = new Location(world, 10, 65, 10);
        when(factionService.getFactionHome(uuid)).thenReturn(Optional.of(home));
        when(essentialsInterop.teleport(
            org.mockito.ArgumentMatchers.eq(player),
            org.mockito.ArgumentMatchers.any(Location.class),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any())).thenReturn(false);

        cmd.execute(ctx());

        verify(player).teleport(org.mockito.ArgumentMatchers.any(Location.class));
        verify(player).sendMessage(argThat(componentContains("Teleported")));
    }

    @Test
    void rejectsWithoutHome() {
        when(factionService.getFactionHome(uuid)).thenReturn(Optional.empty());

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("not set a home")));
    }

    @Test
    void usesEssentialsInteropWhenActive() {
        final Location home = new Location(world, 10, 65, 10);
        when(factionService.getFactionHome(uuid)).thenReturn(Optional.of(home));
        when(essentialsInterop.teleport(
            org.mockito.ArgumentMatchers.eq(player),
            org.mockito.ArgumentMatchers.any(Location.class),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any())).thenReturn(true);

        cmd.execute(ctx());

        verify(essentialsInterop).teleport(
            org.mockito.ArgumentMatchers.eq(player),
            org.mockito.ArgumentMatchers.any(Location.class),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(player, org.mockito.Mockito.never()).teleport(home);
    }

    @Test
    void rejectsJailedPlayer() {
        when(essentialsInterop.isJailed(player)).thenReturn(true);

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("jailed")));
        org.mockito.Mockito.verify(player, org.mockito.Mockito.never())
            .teleport(org.mockito.ArgumentMatchers.any(Location.class));
    }
}
