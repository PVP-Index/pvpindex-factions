package com.gyvex.pvpindex.factions.command.sub;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.gyvex.pvpindex.factions.command.CommandTestBase;
import com.gyvex.pvpindex.factions.data.model.BoardEntry;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.data.model.PlayerModel;
import com.gyvex.pvpindex.factions.data.repository.BoardRepository;
import com.gyvex.pvpindex.factions.data.repository.FactionRepository;
import com.gyvex.pvpindex.factions.data.repository.PlayerRepository;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdMap - /f map [on|off|once]")
class CmdMapTest extends CommandTestBase {

    @Mock private PlayerRepository playerRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private FactionRepository factionRepository;
    @Mock private Location location;
    @Mock private Chunk chunk;
    @Mock private World world;

    private CmdMap cmd;
    private final UUID uuid = UUID.randomUUID();

    @BeforeEach
    void setUp() throws StorageException {
        cmd = new CmdMap();
        when(repos.players()).thenReturn(playerRepository);
        when(repos.board()).thenReturn(boardRepository);
        when(repos.factions()).thenReturn(factionRepository);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getLocation()).thenReturn(location);
        when(location.getChunk()).thenReturn(chunk);
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(chunk.getX()).thenReturn(10);
        when(chunk.getZ()).thenReturn(20);
        when(playerRepository.find(uuid.toString())).thenReturn(Optional.empty());
        when(boardRepository.findByChunk("world", 10, 20)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("map on persists territory title preference")
    void mapOnEnablesTerritoryTitles() throws StorageException {
        final PlayerModel model = new PlayerModel(uuid.toString());
        when(playerRepository.findOrCreate(uuid.toString())).thenReturn(model);

        cmd.execute(ctx("on"));

        verify(playerRepository).save(model);
        verify(player).sendMessage(argThat(componentContains("enabled")));
    }

    @Test
    @DisplayName("map once renders stable symbol legend")
    void mapOnceShowsStableLegendSymbol() {
        cmd.execute(ctx("once"));
        verify(player, atLeastOnce()).sendMessage(argThat(componentContains("Legend")));
        verify(player, atLeastOnce()).sendMessage(argThat(componentContains("■")));
    }

    @Test
    @DisplayName("map tiles include hover and click interactions")
    void mapTilesAreInteractive() throws StorageException {
        final String factionId = UUID.randomUUID().toString();
        final BoardEntry boardEntry = new BoardEntry(BoardEntry.buildId("world", 11, 20));
        boardEntry.setFactionId(factionId);
        final FactionModel faction = new FactionModel(factionId);
        faction.setName("Alpha");
        when(boardRepository.findByChunk("world", 11, 20)).thenReturn(Optional.of(boardEntry));
        when(factionRepository.find(factionId)).thenReturn(Optional.of(faction));

        cmd.execute(ctx("once"));

        verify(player, atLeastOnce()).sendMessage(argThat((Component c) -> hasInteractiveEvent(c)));
    }

    @Test
    @DisplayName("wilderness tile click runs claim-at command")
    void wildernessTileRunsClaimAt() {
        cmd.execute(ctx("once"));
        verify(player, atLeastOnce()).sendMessage(argThat((Component c) -> hasClaimAtClick(c)));
    }

    private boolean hasInteractiveEvent(final Component component) {
        if (component.clickEvent() != null || component.hoverEvent() != null) {
            return true;
        }
        for (final Component child : component.children()) {
            if (hasInteractiveEvent(child)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasClaimAtClick(final Component component) {
        final ClickEvent click = component.clickEvent();
        if (click != null && click.action() == ClickEvent.Action.RUN_COMMAND
                && click.value().startsWith("/f claim at ")) {
            return true;
        }
        for (final Component child : component.children()) {
            if (hasClaimAtClick(child)) {
                return true;
            }
        }
        return false;
    }
}
