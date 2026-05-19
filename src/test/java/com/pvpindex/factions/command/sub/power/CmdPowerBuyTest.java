package com.pvpindex.factions.command.sub.power;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.repository.PlayerRepository;
import com.pvpindex.factions.data.repository.PowerHistoryRepository;
import com.pvpindex.factions.integration.vault.VaultEconomy;
import java.util.Optional;
import java.util.UUID;
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
@DisplayName("CmdPowerBuy — /f power buy <amount>")
class CmdPowerBuyTest extends CommandTestBase {


    @Mock private VaultEconomy vaultEconomy;
    @Mock private PlayerRepository playerRepository;
    @Mock private PowerHistoryRepository powerHistoryRepository;
    @Mock private PlayerModel playerModel;


    private CmdPowerBuy cmd;
    private final UUID uuid = UUID.randomUUID();


    @BeforeEach
    void setUp() {
        cmd = new CmdPowerBuy(vaultEconomy, config, repos);
        when(player.getUniqueId()).thenReturn(uuid);
        when(repos.players()).thenReturn(playerRepository);
        when(repos.powerHistory()).thenReturn(powerHistoryRepository);
    }


    @StorageTest
    @DisplayName("feature disabled — rejected")
    void testFeatureDisabled() {
        when(config.isPowerBuyEnabled()).thenReturn(false);


        cmd.execute(ctx("10"));


        verify(player).sendMessage(argThat(componentContains("not enabled")));
        verify(vaultEconomy, never()).withdraw(any(), any(double.class));
    }


    @StorageTest
    @DisplayName("no economy plugin — rejected")
    void testNoEconomy() {
        when(config.isPowerBuyEnabled()).thenReturn(true);
        when(vaultEconomy.isEnabled()).thenReturn(false);


        cmd.execute(ctx("10"));


        verify(player).sendMessage(argThat(componentContains("economy plugin")));
        verify(vaultEconomy, never()).withdraw(any(), any(double.class));
    }


    @StorageTest
    @DisplayName("success — power purchased")
    void testPurchaseSuccess() throws Exception {
        when(config.isPowerBuyEnabled()).thenReturn(true);
        when(vaultEconomy.isEnabled()).thenReturn(true);
        when(config.getPowerBuyMaxPerPurchase()).thenReturn(50.0);
        when(config.getMaxPower()).thenReturn(100.0);
        when(config.getPowerBuyCostPerPoint()).thenReturn(10.0);
        when(playerRepository.find(uuid.toString())).thenReturn(Optional.of(playerModel));
        when(playerModel.getPower()).thenReturn(50.0);
        when(vaultEconomy.getBalance(player)).thenReturn(1000.0);
        when(vaultEconomy.withdraw(player, 100.0)).thenReturn(true);


        cmd.execute(ctx("10"));


        verify(player).sendMessage(argThat(componentContains("purchased")));
    }


    @StorageTest
    @DisplayName("already at max power — rejected")
    void testAlreadyMaxPower() throws Exception {
        when(config.isPowerBuyEnabled()).thenReturn(true);
        when(vaultEconomy.isEnabled()).thenReturn(true);
        when(config.getPowerBuyMaxPerPurchase()).thenReturn(50.0);
        when(config.getMaxPower()).thenReturn(100.0);
        when(playerRepository.find(uuid.toString())).thenReturn(Optional.of(playerModel));
        when(playerModel.getPower()).thenReturn(100.0);


        cmd.execute(ctx("10"));


        verify(player).sendMessage(argThat(componentContains("maximum power")));
        verify(vaultEconomy, never()).withdraw(any(), any(double.class));
    }
}
