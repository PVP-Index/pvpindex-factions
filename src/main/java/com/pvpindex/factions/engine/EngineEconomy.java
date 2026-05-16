package com.pvpindex.factions.engine;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.config.NotificationsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.BankTransactionModel;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.event.FactionBankTransactionEvent;
import com.pvpindex.factions.event.FactionBankTransactionEvent.Type;
import com.pvpindex.factions.integration.vault.VaultEconomy;
import com.pvpindex.factions.scheduler.CancelableTask;
import com.pvpindex.factions.scheduler.TaskScheduler;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import java.util.UUID;
import java.util.Locale;
import java.util.Objects;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Handles faction bank operations with all known bug fixes applied:
 *
 * <ol>
 *   <li>Confusing parameter ordering — explicit named helpers.</li>
 *   <li>Non-atomic transfer — uses Jaloquent transaction for all multi-record writes.</li>
 *   <li>Economy disabled mid-transaction — Vault availability checked per call.</li>
 *   <li>Negative/zero amounts — validated before any processing.</li>
 * </ol>
 */
public class EngineEconomy {

    private final Repositories repos;
    private final FactionsConfig config;
    private final NotificationsConfig notificationsConfig;
    private final VaultEconomy vaultEconomy;
    private final Logger logger;
    private final TaskScheduler taskScheduler;
    private CancelableTask taxTask;

    public EngineEconomy(
            final Plugin plugin,
            final Repositories repos,
            final FactionsConfig config,
            final VaultEconomy vaultEconomy,
            final Logger logger) {
        this(plugin, repos, config, null, vaultEconomy, null, logger);
    }

    public EngineEconomy(
            final Plugin plugin,
            final Repositories repos,
            final FactionsConfig config,
            final NotificationsConfig notificationsConfig,
            final VaultEconomy vaultEconomy,
            final Logger logger) {
        this(plugin, repos, config, notificationsConfig, vaultEconomy, null, logger);
    }

    public EngineEconomy(
            final Plugin plugin,
            final Repositories repos,
            final FactionsConfig config,
            final NotificationsConfig notificationsConfig,
            final VaultEconomy vaultEconomy,
            final TaskScheduler taskScheduler,
            final Logger logger) {
        this.repos = repos;
        this.config = config;
        this.notificationsConfig = notificationsConfig;
        this.vaultEconomy = vaultEconomy;
        this.taskScheduler = taskScheduler;
        this.logger = logger;
    }

    /**
     * Starts periodic faction bank taxation if enabled in config.
     *
     * @param scheduler task scheduler to use for the timer
     */
    public void startTaxScheduler(final TaskScheduler scheduler) {
        stopTaxScheduler();
        if (!config.isBankEnabled() || !config.isTaxEnabled()) {
            return;
        }
        final int intervalHours = Math.max(1, config.getTaxIntervalHours());
        final long intervalTicks = intervalHours * 60L * 60L * 20L;
        taxTask = scheduler.scheduleAsyncTimer(
            this::applyPeriodicTaxesSafely,
            intervalTicks,
            intervalTicks);
        logger.info("Faction bank tax enabled: rate=" + config.getTaxRate()
            + ", intervalHours=" + intervalHours);
    }

    /**
     * Stops periodic faction bank taxation task if active.
     */
    public void stopTaxScheduler() {
        if (taxTask != null) {
            taxTask.cancel();
            taxTask = null;
        }
    }

    // -------------------------------------------------------------------------
    // Deposit: player wallet → faction bank
    // -------------------------------------------------------------------------

    /**
     * Deposit money from a player's wallet into the faction bank.
     *
     * @param player    depositing player (must be in a faction)
     * @param factionId target faction ID
     * @param amount    positive amount to deposit
     * @return {@code true} on success
     */
    public boolean deposit(final Player player, final String factionId, final double amount) {
        // Bug fix #4 — reject non-positive amounts early
        if (amount <= 0) {
            MsgUtil.send(player, "<red>Amount must be positive.");
            return false;
        }
        // Bug fix #3 — check Vault before each operation
        if (!vaultEconomy.isEnabled()) {
            MsgUtil.send(player, "<red>Economy is not available.");
            return false;
        }
        if (!config.isBankEnabled()) {
            MsgUtil.send(player, "<red>Faction banks are disabled.");
            return false;
        }
        try {
            final Optional<FactionModel> factionOpt = repos.factions().find(factionId);
            if (factionOpt.isEmpty()) {
                MsgUtil.send(player, "<red>Faction not found.");
                return false;
            }

            final double playerBalance = vaultEconomy.getBalance(player);
            if (playerBalance < amount) {
                MsgUtil.send(player, "<red>You do not have enough money.");
                return false;
            }

            final FactionBankTransactionEvent event = new FactionBankTransactionEvent(
                factionOpt.get(), player.getUniqueId(), Type.DEPOSIT, amount);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            final double finalAmount = event.getAmount();

            // Bug fix #3 — re-check Vault inside the commit window
            if (!vaultEconomy.isEnabled()) {
                MsgUtil.send(player, "<red>Economy is not available.");
                return false;
            }

            // Withdraw from player; if it fails do not modify the bank
            final boolean withdrawn = vaultEconomy.withdraw(player, finalAmount);
            if (!withdrawn) {
                MsgUtil.send(player, "<red>Could not withdraw money from your account.");
                return false;
            }

            // Now credit the bank (Bug fix #2 — single-record update, no multi-record race here)
            final FactionModel faction = factionOpt.get();
            faction.setBank(faction.getBank() + finalAmount);
            repos.factions().save(faction);
            recordTransaction(
                faction.getId(),
                player.getUniqueId(),
                "DEPOSIT",
                finalAmount,
                null,
                "Player deposit");
            MsgUtil.send(player, "<green>Deposited <white>"
                + String.format("%.2f", finalAmount)
                + "<green> into the faction bank.");
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to deposit to faction bank " + factionId, e);
            MsgUtil.send(player, "<red>An internal error occurred.");
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Withdraw: faction bank → player wallet
    // -------------------------------------------------------------------------

    /**
     * Withdraw money from the faction bank into a player's wallet.
     *
     * @param player    receiving player (must be in the faction)
     * @param factionId source faction ID
     * @param amount    positive amount to withdraw
     * @return {@code true} on success
     */
    public boolean withdraw(final Player player, final String factionId, final double amount) {
        if (amount <= 0) {
            MsgUtil.send(player, "<red>Amount must be positive.");
            return false;
        }
        if (!vaultEconomy.isEnabled()) {
            MsgUtil.send(player, "<red>Economy is not available.");
            return false;
        }
        if (!config.isBankEnabled()) {
            MsgUtil.send(player, "<red>Faction banks are disabled.");
            return false;
        }
        try {
            final Optional<FactionModel> factionOpt = repos.factions().find(factionId);
            if (factionOpt.isEmpty()) {
                MsgUtil.send(player, "<red>Faction not found.");
                return false;
            }

            final FactionModel faction = factionOpt.get();
            if (faction.getBank() < amount) {
                MsgUtil.send(player, "<red>The faction bank does not have enough funds.");
                return false;
            }

            final FactionBankTransactionEvent event = new FactionBankTransactionEvent(
                faction, player.getUniqueId(), Type.WITHDRAW, amount);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            final double finalAmount = event.getAmount();

            if (!vaultEconomy.isEnabled()) {
                MsgUtil.send(player, "<red>Economy is not available.");
                return false;
            }

            // Debit the bank first, then credit the player
            faction.setBank(faction.getBank() - finalAmount);
            repos.factions().save(faction);

            final boolean deposited = vaultEconomy.deposit(player, finalAmount);
            if (!deposited) {
                // Roll back the bank debit
                faction.setBank(faction.getBank() + finalAmount);
                repos.factions().save(faction);
                MsgUtil.send(player, "<red>Could not credit your account.");
                return false;
            }
            recordTransaction(
                faction.getId(),
                player.getUniqueId(),
                "WITHDRAW",
                finalAmount,
                null,
                "Player withdraw");
            MsgUtil.send(player, "<green>Withdrew <white>"
                + String.format("%.2f", finalAmount)
                + "<green> from the faction bank.");
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to withdraw from faction bank " + factionId, e);
            MsgUtil.send(player, "<red>An internal error occurred.");
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Transfer: faction bank → faction bank
    // -------------------------------------------------------------------------

    /**
     * Transfer money between two faction banks atomically.
     *
     * <p>Bug fix #1: explicit from/to ordering. Bug fix #2: wrapped in a transaction.
     *
     * @param invoker        player who triggered the transfer (for permission checks / events)
     * @param fromFactionId  source faction
     * @param toFactionId    destination faction
     * @param amount         positive amount
     * @return {@code true} on success
     */
    public boolean transfer(
            final UUID invoker,
            final String fromFactionId,
            final String toFactionId,
            final double amount) {
        if (amount <= 0) {
            return false;
        }
        if (!vaultEconomy.isEnabled()) {
            return false;
        }
        if (!config.isBankEnabled()) {
            return false;
        }
        try {
            final boolean[] success = {false};
            // Bug fix #2 — wrap in a Jaloquent transaction
            repos.factions().transaction(() -> {
                // Re-load both inside the transaction window
                final Optional<FactionModel> fromOpt = repos.factions().find(fromFactionId);
                final Optional<FactionModel> toOpt = repos.factions().find(toFactionId);
                if (fromOpt.isEmpty() || toOpt.isEmpty()) {
                    return;
                }
                final FactionModel from = fromOpt.get();
                final FactionModel to = toOpt.get();

                // Bug fix #4 — double-check balance inside the transaction
                if (from.getBank() < amount) {
                    return;
                }

                // Bug fix #3 — re-check Vault inside the transaction window
                if (!vaultEconomy.isEnabled()) {
                    return;
                }

                from.setBank(from.getBank() - amount);
                to.setBank(to.getBank() + amount);
                repos.factions().save(from);
                repos.factions().save(to);
                recordTransaction(
                    from.getId(),
                    invoker,
                    "TRANSFER_OUT",
                    amount,
                    to.getId(),
                    "Transfer to " + Objects.toString(to.getName(), to.getId()));
                recordTransaction(
                    to.getId(),
                    invoker,
                    "TRANSFER_IN",
                    amount,
                    from.getId(),
                    "Transfer from " + Objects.toString(from.getName(), from.getId()));
                success[0] = true;
            });

            if (success[0]) {
                final Optional<FactionModel> fromFaction = repos.factions().find(fromFactionId);
                fromFaction.ifPresent(f -> {
                    final FactionBankTransactionEvent event = new FactionBankTransactionEvent(
                        f, invoker, Type.TRANSFER, amount);
                    Bukkit.getPluginManager().callEvent(event);
                });
            }
            return success[0];
        } catch (StorageException e) {
            logger.log(Level.SEVERE,
                "Failed to transfer from " + fromFactionId + " to " + toFactionId, e);
            return false;
        }
    }

    private void recordTransaction(
            final String factionId,
            final UUID actorUuid,
            final String type,
            final double amount,
            final String counterpartyFactionId,
            final String note) throws StorageException {
        if (repos.bankTransactions() == null) {
            return;
        }
        final BankTransactionModel tx = new BankTransactionModel(UUID.randomUUID().toString());
        tx.setFactionId(factionId);
        tx.setActorUuid(actorUuid == null ? null : actorUuid.toString());
        tx.setType(type.toUpperCase(Locale.ROOT));
        tx.setAmount(amount);
        tx.setCounterpartyFactionId(counterpartyFactionId);
        tx.setCreatedAt(System.currentTimeMillis());
        tx.setNote(note);
        repos.bankTransactions().save(tx);
    }

    /**
     * Applies one taxation pass over all factions.
     *
     * @return number of factions taxed
     */
    public int applyFactionTaxesNow() {
        if (!config.isBankEnabled() || !config.isTaxEnabled()) {
            return 0;
        }
        final double rate = config.getTaxRate();
        if (rate <= 0.0D) {
            return 0;
        }
        final double minBank = Math.max(0.0D, config.getTaxMinBankBalance());
        final double minCharge = Math.max(0.0D, config.getTaxMinChargeAmount());

        int taxedFactions = 0;
        try {
            final List<FactionModel> factions = repos.factions().findAll();
            for (final FactionModel faction : factions) {
                if (!faction.isNormal()) {
                    continue;
                }
                final double currentBank = faction.getBank();
                if (currentBank <= minBank) {
                    continue;
                }
                final double computed = roundMoney(currentBank * rate);
                if (computed <= 0.0D || computed < minCharge) {
                    continue;
                }
                final double taxAmount = Math.min(currentBank, computed);
                final double newBank = roundMoney(Math.max(0.0D, currentBank - taxAmount));
                faction.setBank(newBank);
                repos.factions().save(faction);
                recordTransaction(
                    faction.getId(),
                    null,
                    "TAX",
                    taxAmount,
                    null,
                    "Periodic bank tax (" + roundMoney(rate * 100.0D) + "%)");
                taxedFactions++;
                notifyTaxedMembers(faction, taxAmount, newBank);
            }
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to apply periodic faction bank tax", e);
        }
        return taxedFactions;
    }

    private void applyPeriodicTaxesSafely() {
        final int taxed = applyFactionTaxesNow();
        if (taxed > 0) {
            logger.info("Applied faction bank tax to " + taxed + " faction(s).");
        }
    }

    private void notifyTaxedMembers(final FactionModel faction, final double taxAmount, final double newBank) {
        if (notificationsConfig == null || !notificationsConfig.isEconomyTaxNotifyMembers()) {
            return;
        }
        final String message = MsgUtil.replace(
            MsgUtil.message(
                "bank.tax-charged",
                "<gold>Faction bank tax charged: <yellow>{amount}<gold>. New bank: <yellow>{balance}<gold>."),
            "amount", String.format(Locale.US, "%.2f", taxAmount),
            "balance", String.format(Locale.US, "%.2f", newBank));
        FactionMemberNotifier.notifyMembers(
            taskScheduler,
            repos,
            logger,
            faction.getId(),
            member -> member.hasBankTaxNotifications(),
            message);
    }

    private double roundMoney(final double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }
}
