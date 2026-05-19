package com.pvpindex.factions.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.FactionAuditAction;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.AuditLogModel;
import com.pvpindex.factions.data.repository.AuditLogRepository;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuditServiceImpl")
class AuditServiceImplTest {

    @Mock private Repositories repos;
    @Mock private AuditLogRepository auditLogRepo;
    @Mock private Logger logger;

    private AuditServiceImpl service;

    @BeforeEach
    void setUp() {
        when(repos.auditLogs()).thenReturn(auditLogRepo);
        service = new AuditServiceImpl(repos, logger);
    }

    @StorageTest
    @DisplayName("record persists entry with correct factionId, action, actorUuid, and detail")
    void testRecordSuccess() throws StorageException {
        final UUID actor = UUID.randomUUID();
        service.record("faction-1", actor, FactionAuditAction.MEMBER_KICK, "Player1");

        final ArgumentCaptor<AuditLogModel> captor = ArgumentCaptor.forClass(AuditLogModel.class);
        verify(auditLogRepo).save(captor.capture());
        final AuditLogModel saved = captor.getValue();
        assertEquals("faction-1", saved.getFactionId());
        assertEquals(FactionAuditAction.MEMBER_KICK.getId(), saved.getAction());
        assertEquals(actor.toString(), saved.getActorUuid());
        assertEquals("Player1", saved.getDetail());
    }

    @StorageTest
    @DisplayName("record stores null actorUuid when actorUUID argument is null")
    void testRecordNullActor() throws StorageException {
        service.record("faction-1", null, FactionAuditAction.BANK_DEPOSIT, "50.00");

        final ArgumentCaptor<AuditLogModel> captor = ArgumentCaptor.forClass(AuditLogModel.class);
        verify(auditLogRepo).save(captor.capture());
        assertNull(captor.getValue().getActorUuid());
    }

    @StorageTest
    @DisplayName("record logs a warning but does not propagate StorageException")
    void testRecordStorageException() throws StorageException {
        doThrow(new StorageException("DB error")).when(auditLogRepo).save(any());

        service.record("faction-1", UUID.randomUUID(), FactionAuditAction.CLAIM, "0,0");

        verify(logger).log(eq(Level.WARNING), anyString(), any(Throwable.class));
    }
}
