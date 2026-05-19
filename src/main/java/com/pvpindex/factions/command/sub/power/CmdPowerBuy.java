package com.pvpindex.factions.command.sub.power;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.integration.vault.VaultEconomy;
import com.pvpindex.factions.util.MoneyParser;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import org.bukkit.entity.Player;

/** {@code /f power buy <amount>} — Purchase personal power using money (opt-in). */
public final class CmdPowerBuy extends FactionCommand {

    private final VaultEconomy vaultEconomy;
    private final FactionsConfig factionsConfig;
    private final Repositories repos;

    public CmdPowerBuy(
            final VaultEconomy vaultEconomy,
            final FactionsConfig factionsConfig,
            final Repositories repos) {
        super("buy");
        setPermission("factions.cmd.power.buy");
        setDescription("Purchase personal power with money.");
        setRequiredArgs("<amount>");
        setRequiresPlayer(true);
        this.vaultEconomy = vaultEconomy;
        this.factionsConfig = factionsConfig;
        this.repos = repos;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();

        if (!factionsConfig.isPowerBuyEnabled()) {
            MsgUtil.sendKey(player, "power.buy-disabled", "<red>Buying power is not enabled on this server.");
            return;
        }

        if (!vaultEconomy.isEnabled()) {
            MsgUtil.sendKey(player, "power.buy-no-vault", "<red>An economy plugin is required to buy power.");
            return;
        }

        final double maxPerPurchase = factionsConfig.getPowerBuyMaxPerPurchase();
        final OptionalDouble parsed = MoneyParser.parse(ctx.arg(0));
        if (parsed.isEmpty() || parsed.getAsDouble() <= 0 || parsed.getAsDouble() > maxPerPurchase) {
            MsgUtil.sendKey(player, "power.buy-invalid-amount",
                "<red>Enter a positive amount (max <yellow>{max}<red> per purchase).",
                "max", String.format(java.util.Locale.ROOT, "%.1f", maxPerPurchase));
            return;
        }

        final double amount = parsed.getAsDouble();
        final String playerId = player.getUniqueId().toString();

        try {
            final Optional<PlayerModel> pmOpt = repos.players().find(playerId);
            if (pmOpt.isEmpty()) {
                MsgUtil.send(player, "<red>Could not find your player data.");
                return;
            }
            final PlayerModel pm = pmOpt.get();
            final double maxPower = factionsConfig.getMaxPower();
            if (pm.getPower() >= maxPower) {
                MsgUtil.sendKey(player, "power.buy-already-max", "<red>You already have maximum power.");
                return;
            }

            final double actualAmount = Math.min(amount, maxPower - pm.getPower());
            final double cost = actualAmount * factionsConfig.getPowerBuyCostPerPoint();
            final String costFormatted = String.format(java.util.Locale.ROOT, "%.2f", cost);

            if (vaultEconomy.getBalance(player) < cost) {
                MsgUtil.sendKey(player, "power.buy-insufficient-funds",
                    "<red>You need <yellow>{cost}<red> to buy that much power.",
                    "cost", costFormatted);
                return;
            }

            if (!vaultEconomy.withdraw(player, cost)) {
                MsgUtil.send(player, "<red>Transaction failed — please try again.");
                return;
            }

            pm.setPower(pm.getPower() + actualAmount);
            repos.players().save(pm);
            repos.powerHistory().record(playerId, actualAmount, "BUY", pm.getPower());

            MsgUtil.sendKey(player, "power.buy-success",
                "<green>You purchased <yellow>{amount}<green> power for <yellow>{cost}<green>.",
                "amount", String.format(java.util.Locale.ROOT, "%.1f", actualAmount),
                "cost", costFormatted);
        } catch (com.github.ezframework.jaloquent.exception.StorageException e) {
            MsgUtil.send(player, "<red>A storage error occurred. Please try again.");
        }
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return List.of("1", "2", "5");
        }
        return List.of();
    }
}
