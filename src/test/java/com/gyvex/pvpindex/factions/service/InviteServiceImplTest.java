package com.gyvex.pvpindex.factions.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gyvex.pvpindex.factions.config.FactionsConfig;
import com.gyvex.pvpindex.factions.data.Repositories;
import com.gyvex.pvpindex.factions.data.model.InvitationModel;
import com.gyvex.pvpindex.factions.data.repository.FactionRepository;
import com.gyvex.pvpindex.factions.data.repository.InvitationRepository;
import com.gyvex.pvpindex.factions.data.repository.RankRepository;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
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
@DisplayName("InviteServiceImpl expiry handling")
class InviteServiceImplTest {

    @Mock private FactionServiceImpl factionService;
    @Mock private Repositories repos;
    @Mock private FactionsConfig config;
    @Mock private InvitationRepository invitations;
    @Mock private FactionRepository factions;
    @Mock private RankRepository ranks;

    private InviteServiceImpl service;

    @BeforeEach
    void setUp() {
        when(repos.invitations()).thenReturn(invitations);
        when(repos.factions()).thenReturn(factions);
        when(repos.ranks()).thenReturn(ranks);
        when(config.getInviteTtlHours()).thenReturn(72);
        service = new InviteServiceImpl(factionService, repos, config, Logger.getLogger("test"));
    }

    @Test
    @DisplayName("listActiveInvitesForPlayer prunes expired and returns only active")
    void listActivePrunesExpired() throws Exception {
        final UUID player = UUID.randomUUID();
        final long now = System.currentTimeMillis();

        final InvitationModel expired = new InvitationModel("expired");
        expired.setInviteeId(player.toString());
        expired.setFactionId(UUID.randomUUID().toString());
        expired.setCreatedAt(now - (73L * 60L * 60L * 1000L));

        final InvitationModel active = new InvitationModel("active");
        active.setInviteeId(player.toString());
        active.setFactionId(UUID.randomUUID().toString());
        active.setCreatedAt(now - (1L * 60L * 60L * 1000L));

        when(invitations.findByInviteeId(player.toString())).thenReturn(List.of(expired, active));

        final List<InvitationModel> result = service.listActiveInvitesForPlayer(player);

        assertTrue(result.size() == 1);
        assertEquals("active", result.get(0).getId());
        verify(invitations).delete("expired");
    }

    @Test
    @DisplayName("pruneExpiredInvitesForPlayer returns removed count")
    void pruneExpiredCount() throws Exception {
        final UUID player = UUID.randomUUID();
        final long now = System.currentTimeMillis();

        final InvitationModel expired = new InvitationModel("expired");
        expired.setInviteeId(player.toString());
        expired.setFactionId(UUID.randomUUID().toString());
        expired.setCreatedAt(now - (96L * 60L * 60L * 1000L));

        when(invitations.findByInviteeId(player.toString())).thenReturn(List.of(expired));

        final int removed = service.pruneExpiredInvitesForPlayer(player);

        assertEquals(1, removed);
        verify(invitations).delete("expired");
    }

    @Test
    @DisplayName("declineAllInvites only counts active invites after pruning")
    void declineAllAfterPrune() throws Exception {
        final UUID player = UUID.randomUUID();
        final long now = System.currentTimeMillis();

        final InvitationModel expired = new InvitationModel("expired");
        expired.setInviteeId(player.toString());
        expired.setCreatedAt(now - (100L * 60L * 60L * 1000L));

        final InvitationModel active = new InvitationModel("active");
        active.setInviteeId(player.toString());
        active.setCreatedAt(now - (2L * 60L * 60L * 1000L));

        when(invitations.findByInviteeId(player.toString()))
            .thenReturn(List.of(expired, active))
            .thenReturn(List.of(active));

        final int removed = service.declineAllInvites(player);

        assertEquals(1, removed);
        verify(invitations).delete("expired");
        verify(invitations).deleteByInviteeId(player.toString());
    }
}
