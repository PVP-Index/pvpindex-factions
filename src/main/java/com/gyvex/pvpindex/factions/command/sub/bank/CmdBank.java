package com.gyvex.pvpindex.factions.command.sub.bank;

import com.gyvex.pvpindex.factions.command.CommandContext;
import com.gyvex.pvpindex.factions.command.CommandGuards;
import com.gyvex.pvpindex.factions.command.FactionCommand;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.engine.EngineEconomy;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import org.bukkit.entity.Player;

/**
 * {@code /f bank <deposit|withdraw> <amount>} — Faction bank group.
 *
 * <p>This command acts as a dispatcher; the actual logic lives in
 * {@link CmdBankDeposit} and {@link CmdBankWithdraw}.
 */
public final class CmdBank extends FactionCommand {

    private final FactionService factionService;

    public CmdBank(final FactionService factionService, final EngineEconomy engineEconomy) {
        super("bank");
        setPermission("factions.cmd.bank");
        setDescription("Manage the faction bank.");
        setOptionalArgs("[deposit|withdraw|transfer|history]");
        setRequiresPlayer(true);
        this.factionService = factionService;
        addChild(new CmdBankDeposit(factionService, engineEconomy));
        addChild(new CmdBankWithdraw(factionService, engineEconomy));
        addChild(new CmdBankTransfer(factionService, engineEconomy));
        addChild(new CmdBankHistory(factionService));
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> factionOpt = CommandGuards.requireFaction(player, factionService);
        if (factionOpt.isEmpty()) {
            return;
        }
        MsgUtil.send(player, "<gold>Faction bank balance: <white>"
            + String.format(java.util.Locale.ROOT, "%.2f", factionOpt.get().getBank()) + "</white>");
        MsgUtil.send(player, "<gray>Use <yellow>/f bank history</yellow> to view transactions.");
    }
}
