package com.pvpindex.factions.command.sub;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.service.FactionService;
import java.util.UUID;
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
@DisplayName("CmdDesc — /f desc <text>")
class CmdDescTest extends CommandTestBase {


    @Mock private FactionService factionService;


    private CmdDesc cmd;
    private final UUID uuid = UUID.randomUUID();


    @BeforeEach
    void setUp() {
        cmd = new CmdDesc(factionService);
        when(player.getUniqueId()).thenReturn(uuid);
    }


    @StorageTest
    @DisplayName("success — description updated")
    void testDescriptionUpdated() {
        when(factionService.isOwner(uuid)).thenReturn(true);
        when(factionService.setFactionDescription(uuid, "A cool faction")).thenReturn(true);


        cmd.execute(ctx("A", "cool", "faction"));


        verify(player).sendMessage(argThat(componentContains("updated")));
    }


    @StorageTest
    @DisplayName("not owner — rejected")
    void testNotOwner() {
        when(factionService.isOwner(uuid)).thenReturn(false);


        cmd.execute(ctx("some text"));


        verify(player).sendMessage(argThat(componentContains("owner")));
        verify(factionService, never()).setFactionDescription(any(), any());
    }


    @StorageTest
    @DisplayName("description too long — rejected")
    void testDescriptionTooLong() {
        when(factionService.isOwner(uuid)).thenReturn(true);
        final String tooLong = "a".repeat(251);


        cmd.execute(ctx(tooLong));


        verify(player).sendMessage(argThat(componentContains("too long")));
        verify(factionService, never()).setFactionDescription(any(), any());
    }


    @StorageTest
    @DisplayName("service failure — error message sent")
    void testServiceFailure() {
        when(factionService.isOwner(uuid)).thenReturn(true);
        when(factionService.setFactionDescription(eq(uuid), any())).thenReturn(false);


        cmd.execute(ctx("Some description"));


        verify(player).sendMessage(argThat(componentContains("Could not")));
    }
}
