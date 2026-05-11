package com.pvpindex.factions.command.sub.predefined;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.util.MsgUtil;

/** {@code /f predefined list}. */
public final class CmdPredefinedList extends FactionCommand {

    public CmdPredefinedList() {
        super("list");
        setPermission("factions.cmd.predefined.list");
        setDescription("List predefined factions.");
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final PredefinedConfigManager manager = PredefinedCommandSupport.requireEnabled(ctx);
        if (manager == null) {
            return;
        }
        if (manager.presetNames().isEmpty()) {
            MsgUtil.sendKey(
                ctx.getSender(),
                "predefined.none",
                "<yellow>No predefined factions configured.");
            return;
        }
        MsgUtil.send(ctx.getSender(), "<gold>Predefined factions:</gold> <white>"
            + String.join(", ", manager.presetNames()) + "</white>");
    }
}
