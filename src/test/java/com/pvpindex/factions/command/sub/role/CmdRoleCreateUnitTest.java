package com.pvpindex.factions.command.sub.role;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.service.FactionService;
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
@DisplayName("CmdRoleCreate — unit")
class CmdRoleCreateUnitTest extends CommandTestBase {

    @Mock private FactionService factionService;

    private CmdRoleCreate cmd;
    private final UUID uuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cmd = new CmdRoleCreate(factionService);
        when(player.getUniqueId()).thenReturn(uuid);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("create disabled — shows disabled message")
    void createDisabledShowsMessage() {
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        when(factionService.isCustomRolesEnabled()).thenReturn(false);
        when(factionService.isRoleFactionOverridesEnabled()).thenReturn(false);

        cmd.execute(ctx("Scout", "30"));

        verify(player).sendMessage(argThat(componentContains("Role creation is disabled")));
    }
}
