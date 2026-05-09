package com.pvpindex.factions.integration.vault;

import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Thin wrapper around Vault's {@link Economy} service.
 *
 * <p>Vault is a soft-dependency. When Vault is absent {@link #isEnabled()} returns
 * {@code false} and all mutation methods return {@code false} without throwing.
 */
public class VaultEconomy {

    private final Logger logger;
    /** {@code false} once we've confirmed Vault itself is absent — skip future lookups. */
    private boolean vaultPresent = true;

    public VaultEconomy(final Logger logger) {
        this.logger = logger;
    }

    /**
     * Check whether the Vault plugin is present on the server.
     * The economy provider is resolved lazily on first use so that plugins
     * that register later (e.g. EzEconomy) are found correctly.
     *
     * @return {@code true} if the Vault plugin jar is loaded
     */
    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            vaultPresent = false;
            return false;
        }
        return true;
    }

    /**
     * Resolve the economy provider from the ServicesManager on demand.
     * Returns {@code null} when Vault is absent or no provider is registered.
     */
    private Economy economy() {
        if (!vaultPresent) return null;
        final RegisteredServiceProvider<Economy> rsp =
            Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp != null ? rsp.getProvider() : null;
    }

    /** @return {@code true} if Vault is present and an economy provider is registered. */
    public boolean isEnabled() {
        return economy() != null;
    }

    /**
     * Get the balance of a player.
     *
     * @param player the player
     * @return balance, or 0 if economy is not available
     */
    public double getBalance(final Player player) {
        final Economy eco = economy();
        if (eco == null) return 0;
        return eco.getBalance(player);
    }

    /**
     * Withdraw an amount from a player.
     *
     * @param player the player
     * @param amount positive amount
     * @return {@code true} if the transaction succeeded
     */
    public boolean withdraw(final Player player, final double amount) {
        final Economy eco = economy();
        if (eco == null || amount <= 0) return false;
        return eco.withdrawPlayer(player, amount).transactionSuccess();
    }

    /**
     * Deposit an amount to a player.
     *
     * @param player the player
     * @param amount positive amount
     * @return {@code true} if the transaction succeeded
     */
    public boolean deposit(final Player player, final double amount) {
        final Economy eco = economy();
        if (eco == null || amount <= 0) return false;
        return eco.depositPlayer(player, amount).transactionSuccess();
    }
}
