package com.pvpindex.factions.command.sub.admin;

import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.service.PowerService;

/** {@code /fa power ...} admin power management commands. */
public final class CmdAdminPower extends FactionCommand {

    public CmdAdminPower(final PowerService powerService) {
        super("power");
        setPermission("factions.cmd.admin.power");
        setDescription("Manage player/faction power values.");
        addChild(new CmdAdminPowerView(powerService));
        addChild(new CmdAdminPowerSet(powerService));
        addChild(new CmdAdminPowerAdd(powerService));
        addChild(new CmdAdminPowerRemove(powerService));
        addChild(new CmdAdminPowerReset(powerService));
        addChild(new CmdAdminPowerFreeze(powerService));
        addChild(new CmdAdminPowerHistory(powerService));
    }
}
