package com.pvpindex.factions.command.sub.admin;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.repository.BoardRepository;
import java.util.Optional;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdAdminWarzone — /fa warzone")
class CmdAdminWarzoneTest extends CommandTestBase {

    @Mock private BoardRepository boardRepository;
    @Mock private BoardEntry boardEntry;
    @Mock private World world;
    @Mock private Chunk chunk;

    private CmdAdminWarzone cmd;

    @BeforeEach
    void setUp() {
        cmd = new CmdAdminWarzone();
        when(repos.board()).thenReturn(boardRepository);
        when(config.getLandMaxPerCommand()).thenReturn(10);

        final Location loc = Mockito.mock(Location.class);
        when(loc.getChunk()).thenReturn(chunk);
        when(player.getLocation()).thenReturn(loc);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(5);
        when(chunk.getZ()).thenReturn(6);
        when(world.getName()).thenReturn("world");
    }

    @StorageTest
    @DisplayName("default mode — assigns current chunk as war zone")
    void testAssignSingleChunk() throws Exception {
        cmd.execute(ctx());

        verify(boardRepository).claimChunk("world", 5, 6, FactionModel.WARZONE_ID);
        verify(player).sendMessage(argThat(componentContains("war zone")));
    }

    @StorageTest
    @DisplayName("remove mode — chunk is warzone — removed")
    void testRemoveWarzoneChunk() throws Exception {
        when(boardRepository.findByChunk("world", 5, 6)).thenReturn(Optional.of(boardEntry));
        when(boardEntry.getFactionId()).thenReturn(FactionModel.WARZONE_ID);

        cmd.execute(ctx("remove"));

        verify(boardRepository).unclaimChunk(eq("world"), eq(5), eq(6));
        verify(player).sendMessage(argThat(componentContains("Removed")));
    }

    @StorageTest
    @DisplayName("remove mode — chunk is not warzone — error message")
    void testRemoveNonWarzone() throws Exception {
        when(boardRepository.findByChunk("world", 5, 6)).thenReturn(Optional.empty());

        cmd.execute(ctx("remove"));

        verify(player).sendMessage(argThat(componentContains("not a war zone")));
    }
}
