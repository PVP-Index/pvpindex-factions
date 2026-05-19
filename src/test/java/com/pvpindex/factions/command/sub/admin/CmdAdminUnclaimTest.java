package com.pvpindex.factions.command.sub.admin;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.repository.BoardRepository;
import com.pvpindex.factions.data.repository.FactionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
@DisplayName("CmdAdminUnclaim — /fa unclaim <faction>")
class CmdAdminUnclaimTest extends CommandTestBase {

    @Mock private FactionRepository factionRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private FactionModel faction;
    @Mock private BoardEntry boardEntry;
    @Mock private World world;
    @Mock private Chunk chunk;

    private CmdAdminUnclaim cmd;
    private final String factionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        cmd = new CmdAdminUnclaim();
        when(repos.factions()).thenReturn(factionRepository);
        when(repos.board()).thenReturn(boardRepository);
        when(faction.getId()).thenReturn(factionId);
        when(faction.getName()).thenReturn("Alpha");
        when(config.getLandMaxPerCommand()).thenReturn(10);

        final Location loc = Mockito.mock(Location.class);
        when(loc.getChunk()).thenReturn(chunk);
        when(player.getLocation()).thenReturn(loc);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(0);
        when(chunk.getZ()).thenReturn(0);
        when(world.getName()).thenReturn("world");
    }

    @StorageTest
    @DisplayName("faction not found — error message")
    void testFactionNotFound() throws Exception {
        when(factionRepository.findByName("Unknown")).thenReturn(Optional.empty());

        cmd.execute(ctx("Unknown", "all"));

        verify(player).sendMessage(argThat(componentContains("not found")));
    }

    @StorageTest
    @DisplayName("all mode — unclaims all chunks")
    void testUnclaimAll() throws Exception {
        when(factionRepository.findByName("Alpha")).thenReturn(Optional.of(faction));
        when(boardEntry.getWorldName()).thenReturn("world");
        when(boardEntry.getChunkX()).thenReturn(3);
        when(boardEntry.getChunkZ()).thenReturn(4);
        when(boardRepository.findByFactionId(factionId)).thenReturn(List.of(boardEntry));

        cmd.execute(ctx("Alpha", "all"));

        verify(boardRepository).unclaimChunk("world", 3, 4);
        verify(player).sendMessage(argThat(componentContains("unclaimed")));
    }
}
