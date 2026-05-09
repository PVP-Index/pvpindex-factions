package com.gyvex.pvpindex.factions.command.sub.bank;

import com.gyvex.pvpindex.factions.command.CommandContext;
import com.gyvex.pvpindex.factions.command.FactionCommand;
import com.gyvex.pvpindex.factions.command.CommandGuards;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.engine.EngineEconomy;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.util.MoneyParser;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import java.util.OptionalDouble;
import org.bukkit.entity.Player;

/** {@code /f bank withdraw <amount>} — Withdraw money from the faction bank. */
public final class CmdBankWithdraw extends FactionCommand {

    private final FactionService factionService;
    private final EngineEconomy engineEconomy;

    public CmdBankWithdraw(
            final FactionService factionService, final EngineEconomy engineEconomy) {
        super("withdraw");
        setPermission("factions.cmd.bank");
        setDescription("Withdraw money from the faction bank.");
        setRequiredArgs("<amount>");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.engineEconomy = engineEconomy;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> factionOpt = CommandGuards.requireFaction(player, factionService);
        if (factionOpt.isEmpty()) {
            return;
        }
        final OptionalDouble parsed = MoneyParser.parse(ctx.arg(0));
        if (parsed.isEmpty()) {
            MsgUtil.send(player, "<red>Invalid amount.");
            return;
        }
        final double amount = parsed.getAsDouble();
        if (amount <= 0) {
            MsgUtil.send(player, "<red>Amount must be positive.");
            return;
        }
        engineEconomy.withdraw(player, factionOpt.get().getId(), amount);
    }
}
