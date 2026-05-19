package com.pvpindex.factions.command.sub.power;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.integration.vault.VaultEconomy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import com.pvpindex.factions.command.StorageTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdPower — /f power")
class CmdPowerTest extends CommandTestBase {


    @Mock private VaultEconomy vaultEconomy;


    private CmdPower cmd;


    @BeforeEach
    void setUp() {
        cmd = new CmdPower(vaultEconomy, config, repos);
    }


    @StorageTest
    @DisplayName("perform — shows usage message with buy sub-command")
    void testShowsUsage() {
        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("buy")));
    }
}
