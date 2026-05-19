package com.pvpindex.factions.command.sub;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.data.model.AuditLogModel;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.repository.AuditLogRepository;
import com.pvpindex.factions.service.FactionService;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CmdAuditTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private FactionModel faction;
    @Mock private AuditLogRepository auditLogRepo;

    private CmdAudit cmd;
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        cmd = new CmdAudit(factionService);
        when(player.getUniqueId()).thenReturn(actorId);
        when(repos.auditLogs()).thenReturn(auditLogRepo);
        when(config.getAuditPageSize()).thenReturn(10);
        when(faction.getId()).thenReturn("f-1");
        when(faction.getName()).thenReturn("Avengers");

        final Server mockServer = Mockito.mock(Server.class);
        final Field serverField = org.bukkit.Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, mockServer);
    }

    @AfterEach
    void tearDown() throws Exception {
        final Field serverField = org.bukkit.Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, null);
    }

    @StorageTest
    void notInFactionShowsError() throws Exception {
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.empty());

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("not in a faction")));
        verify(auditLogRepo, never()).findByFaction(anyString(), anyInt(), anyInt());
    }

    @StorageTest
    void notOfficerShowsError() throws Exception {
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.of(faction));
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(false);

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("officers or above")));
        verify(auditLogRepo, never()).findByFaction(anyString(), anyInt(), anyInt());
    }

    @StorageTest
    void emptyResultsShowsNoEntriesMessage() throws Exception {
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.of(faction));
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(true);
        when(auditLogRepo.findByFaction("f-1", 10, 0)).thenReturn(List.of());

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("No audit")));
    }

    @StorageTest
    void showsHeaderAndEntryForResults() throws Exception {
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.of(faction));
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(true);

        final AuditLogModel entry = new AuditLogModel(UUID.randomUUID().toString());
        entry.setFactionId("f-1");
        entry.setActorUuid(null);
        entry.setAction("kick");
        entry.setDetail("Player1");
        entry.setCreatedAt(System.currentTimeMillis());
        when(auditLogRepo.findByFaction("f-1", 10, 0)).thenReturn(List.of(entry));

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("Audit Log")));
        verify(player).sendMessage(argThat(componentContains("kick")));
    }

    @StorageTest
    void invalidActionFilterShowsError() throws Exception {
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.of(faction));
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(true);

        cmd.execute(ctx("1", "--action=not-a-real-action"));

        verify(player).sendMessage(argThat(componentContains("Unknown action")));
        verify(auditLogRepo, never()).findByFaction(anyString(), anyInt(), anyInt());
        verify(auditLogRepo, never()).findByFactionAndAction(anyString(), anyString(), anyInt(), anyInt());
    }
}
