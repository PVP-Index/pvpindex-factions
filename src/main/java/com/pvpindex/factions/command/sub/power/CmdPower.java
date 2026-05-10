package com.pvpindex.factions.command.sub.power;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.integration.vault.VaultEconomy;
import com.pvpindex.factions.util.MsgUtil;
import org.bukkit.entity.Player;

/**
 * {@code /f power [buy]} — Power management group command.
 */
public final class CmdPower extends FactionCommand {

    public CmdPower(
            final VaultEconomy vaultEconomy,
            final FactionsConfig config,
            final Repositories repos) {
        super("power");
        setPermission("factions.cmd.power");
        setDescription("View or manage your power.");
        setOptionalArgs("[buy]");
        setRequiresPlayer(true);
        addChild(new CmdPowerBuy(vaultEconomy, config, repos));
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        MsgUtil.send(player, "<gray>Usage: <yellow>/f power buy <amount>");
    }
}
