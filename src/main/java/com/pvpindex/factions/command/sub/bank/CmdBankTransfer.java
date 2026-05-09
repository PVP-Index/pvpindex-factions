package com.pvpindex.factions.command.sub.bank;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.engine.EngineEconomy;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MoneyParser;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import org.bukkit.entity.Player;

/** {@code /f bank transfer <faction> <amount>} */
public final class CmdBankTransfer extends FactionCommand {

    private final FactionService factionService;
    private final EngineEconomy engineEconomy;

    public CmdBankTransfer(final FactionService factionService, final EngineEconomy engineEconomy) {
        super("transfer");
        setPermission("factions.cmd.bank.transfer");
        setDescription("Transfer money from your faction bank to another faction bank.");
        setRequiredArgs("<faction>", "<amount>");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.engineEconomy = engineEconomy;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> source = CommandGuards.requireFaction(player, factionService);
        if (source.isEmpty()) {
            return;
        }
        if (!CommandGuards.requireOfficerOrAbove(player, factionService)) {
            return;
        }
        final Optional<FactionModel> target = factionService.getFactionByName(ctx.arg(0));
        if (target.isEmpty()) {
            MsgUtil.send(player, "<red>Faction not found.");
            return;
        }
        if (source.get().getId().equals(target.get().getId())) {
            MsgUtil.send(player, "<red>You cannot transfer to your own faction.");
            return;
        }
        final OptionalDouble parsed = MoneyParser.parse(ctx.arg(1));
        if (parsed.isEmpty()) {
            MsgUtil.send(player, "<red>Invalid amount.");
            return;
        }
        final double amount = parsed.getAsDouble();
        if (amount <= 0) {
            MsgUtil.send(player, "<red>Amount must be positive.");
            return;
        }
        if (engineEconomy.transfer(player.getUniqueId(), source.get().getId(), target.get().getId(), amount)) {
            MsgUtil.send(player, "<green>Transferred <white>"
                + String.format(Locale.ROOT, "%.2f", amount)
                + "<green> to faction <white>" + target.get().getName() + "<green>.");
            return;
        }
        MsgUtil.send(player, "<red>Transfer failed.");
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return factionService.getAllFactions().stream().map(FactionModel::getName).toList();
        }
        return List.of();
    }
}
