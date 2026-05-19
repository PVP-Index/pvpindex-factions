package com.pvpindex.factions.command.sub.admin;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.repository.BoardRepository;
import com.pvpindex.factions.data.repository.FactionRepository;
import java.util.Optional;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import com.pvpindex.factions.command.StorageTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdAdminClaim — /fa claim <faction>")
class CmdAdminClaimTest extends CommandTestBase {


    @Mock private FactionRepository factionRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private FactionModel faction;
    @Mock private World world;
    @Mock private Chunk chunk;


    private CmdAdminClaim cmd;


    @BeforeEach
    void setUp() throws Exception {
        cmd = new CmdAdminClaim();
        when(repos.factions()).thenReturn(factionRepository);
        when(repos.board()).thenReturn(boardRepository);
        when(faction.getId()).thenReturn("faction-id");
        when(faction.getName()).thenReturn("Alpha");
        when(config.getLandMaxPerCommand()).thenReturn(10);


        // Location/chunk mocks
        final Location loc = Mockito.mock(Location.class);
        when(loc.getChunk()).thenReturn(chunk);
        when(player.getLocation()).thenReturn(loc);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(0);
        when(chunk.getZ()).thenReturn(0);
        when(world.getName()).thenReturn("world");
    }


    @StorageTest
    @DisplayName("faction not found — error message shown")
    void testFactionNotFound() throws Exception {
        when(factionRepository.findByName("Unknown")).thenReturn(Optional.empty());


        cmd.execute(ctx("Unknown"));


        verify(player).sendMessage(argThat(componentContains("not found")));
    }


    @StorageTest
    @DisplayName("claim single chunk — success message shown")
    void testClaimSingleChunk() throws Exception {
        when(factionRepository.findByName("Alpha")).thenReturn(Optional.of(faction));
        when(boardRepository.findByChunk("world", 0, 0)).thenReturn(Optional.empty());


        cmd.execute(ctx("Alpha"));


        verify(player).sendMessage(argThat(componentContains("Admin-claimed")));
    }
}
