package com.pvpindex.factions.command.sub.predefined;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.util.MsgUtil;

final class PredefinedCommandSupport {

    private PredefinedCommandSupport() {
    }

    static PredefinedConfigManager requireEnabled(final CommandContext ctx) {
        final PredefinedConfigManager manager = PredefinedConfigManager.getInstance();
        if (manager == null || !manager.isEnabled()) {
            MsgUtil.sendKey(
                ctx.getSender(),
                "predefined.disabled",
                "<red>Predefined factions are disabled.");
            return null;
        }
        return manager;
    }
}
