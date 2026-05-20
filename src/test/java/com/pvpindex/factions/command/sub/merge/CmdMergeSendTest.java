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
@DisplayName("CmdMergeSend — /f merge send <faction>")
class CmdMergeSendTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private MergeService mergeService;
    @Mock private FactionModel senderFaction;
    @Mock private FactionModel targetFaction;

    private CmdMergeSend cmd;
    private final UUID playerUUID = UUID.randomUUID();
    private final String senderFactionId = UUID.randomUUID().toString();
    private final String targetFactionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        cmd = new CmdMergeSend(factionService, mergeService);

        when(config.isMergeEnabled()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(playerUUID);
        when(senderFaction.getId()).thenReturn(senderFactionId);
        when(senderFaction.getName()).thenReturn("Alpha");
        when(targetFaction.getId()).thenReturn(targetFactionId);
        when(targetFaction.getName()).thenReturn("Beta");

        when(factionService.getFactionByPlayer(playerUUID)).thenReturn(Optional.of(senderFaction));
        when(factionService.isOfficerOrAbove(playerUUID)).thenReturn(true);
    }

    @StorageTest
    @DisplayName("sender not in faction — error")
    void testSenderNotInFaction() {
        when(factionService.getFactionByPlayer(playerUUID)).thenReturn(Optional.empty());

        cmd.execute(ctx("Beta"));

        verify(mergeService, never()).sendMergeRequest(senderFactionId, targetFactionId, playerUUID);
    }

    @StorageTest
    @DisplayName("sender is not officer — error")
    void testSenderNotOfficer() {
        when(factionService.isOfficerOrAbove(playerUUID)).thenReturn(false);

        cmd.execute(ctx("Beta"));

        verify(mergeService, never()).sendMergeRequest(senderFactionId, targetFactionId, playerUUID);
    }

    @StorageTest
    @DisplayName("target faction not found — error")
    void testTargetNotFound() {
        when(factionService.getFactionByName("Unknown")).thenReturn(Optional.empty());

        cmd.execute(ctx("Unknown"));

        verify(player).sendMessage(argThat(componentContains("not found")));
        verify(mergeService, never()).sendMergeRequest(senderFactionId, targetFactionId, playerUUID);
    }

    @StorageTest
    @DisplayName("self-merge — error")
    void testSelfMerge() {
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.of(senderFaction));
        // same ID as sender
        when(targetFaction.getId()).thenReturn(senderFactionId);
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.of(senderFaction));

        cmd.execute(ctx("Alpha"));

        verify(mergeService, never()).sendMergeRequest(senderFactionId, senderFactionId, playerUUID);
    }

    @StorageTest
    @DisplayName("duplicate request — error")
    void testDuplicateRequest() {
        when(factionService.getFactionByName("Beta")).thenReturn(Optional.of(targetFaction));
        when(mergeService.sendMergeRequest(senderFactionId, targetFactionId, playerUUID)).thenReturn(false);

        cmd.execute(ctx("Beta"));

        verify(player).sendMessage(argThat(componentContains("already pending")));
    }

    @StorageTest
    @DisplayName("success — request sent")
    void testSuccess() {
        when(factionService.getFactionByName("Beta")).thenReturn(Optional.of(targetFaction));
        when(mergeService.sendMergeRequest(senderFactionId, targetFactionId, playerUUID)).thenReturn(true);

        cmd.execute(ctx("Beta"));

        verify(player).sendMessage(argThat(componentContains("Merge request sent")));
    }
}
