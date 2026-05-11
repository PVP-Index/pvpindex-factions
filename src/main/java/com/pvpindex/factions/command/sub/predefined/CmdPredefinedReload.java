package com.pvpindex.factions.command.sub.predefined;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.util.MsgUtil;

/** {@code /f predefined reload}. */
public final class CmdPredefinedReload extends FactionCommand {

    public CmdPredefinedReload() {
        super("reload");
        setPermission("factions.cmd.predefined.reload");
        setDescription("Reload pre-defined.yml.");
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final PredefinedConfigManager manager = PredefinedConfigManager.getInstance();
        if (manager == null) {
            MsgUtil.sendKey(
                ctx.getSender(),
                "predefined.reload-failed",
                "<red>Predefined manager is not available.");
            return;
        }
        manager.reload();
        MsgUtil.sendKey(
            ctx.getSender(),
            "predefined.reload-success",
            "<green>Predefined factions reloaded.");
    }
}
