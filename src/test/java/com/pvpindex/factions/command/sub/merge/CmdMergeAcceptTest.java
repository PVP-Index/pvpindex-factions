package com.pvpindex.factions.command.sub.merge;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.MergeService;
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
@DisplayName("CmdMergeAccept — /f merge accept <faction>")
class CmdMergeAcceptTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private MergeService mergeService;
    @Mock private FactionModel targetFaction;
    @Mock private FactionModel senderFaction;

    private CmdMergeAccept cmd;
    private final UUID playerUUID = UUID.randomUUID();
    private final String targetFactionId = UUID.randomUUID().toString();
    private final String senderFactionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        cmd = new CmdMergeAccept(factionService, mergeService);

        when(config.isMergeEnabled()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(playerUUID);
        when(targetFaction.getId()).thenReturn(targetFactionId);
        when(targetFaction.getName()).thenReturn("Beta");
        when(senderFaction.getId()).thenReturn(senderFactionId);
        when(senderFaction.getName()).thenReturn("Alpha");

        when(factionService.getFactionByPlayer(playerUUID)).thenReturn(Optional.of(targetFaction));
        when(factionService.isOfficerOrAbove(playerUUID)).thenReturn(true);
    }

    @StorageTest
    @DisplayName("acceptor not in faction — error")
    void testAcceptorNotInFaction() {
        when(factionService.getFactionByPlayer(playerUUID)).thenReturn(Optional.empty());

        cmd.execute(ctx("Alpha"));

        verify(mergeService, never()).acceptMergeRequest(senderFactionId, targetFactionId, playerUUID);
    }

    @StorageTest
    @DisplayName("acceptor is not officer — error")
    void testAcceptorNotOfficer() {
        when(factionService.isOfficerOrAbove(playerUUID)).thenReturn(false);

        cmd.execute(ctx("Alpha"));

        verify(mergeService, never()).acceptMergeRequest(senderFactionId, targetFactionId, playerUUID);
    }

    @StorageTest
    @DisplayName("sender faction not found — error")
    void testSenderNotFound() {
        when(factionService.getFactionByName("Unknown")).thenReturn(Optional.empty());

        cmd.execute(ctx("Unknown"));

        verify(player).sendMessage(argThat(componentContains("not found")));
    }

    @StorageTest
    @DisplayName("no pending request — error")
    void testNoPendingRequest() {
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.of(senderFaction));
        when(mergeService.acceptMergeRequest(senderFactionId, targetFactionId, playerUUID))
            .thenReturn(Optional.empty());

        cmd.execute(ctx("Alpha"));

        verify(player).sendMessage(argThat(componentContains("No pending merge request")));
    }

    @StorageTest
    @DisplayName("success — merge completed")
    void testSuccess() {
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.of(senderFaction));
        when(mergeService.acceptMergeRequest(senderFactionId, targetFactionId, playerUUID))
            .thenReturn(Optional.of(targetFaction));

        cmd.execute(ctx("Alpha"));

        verify(player).sendMessage(argThat(componentContains("merged")));
    }
}
