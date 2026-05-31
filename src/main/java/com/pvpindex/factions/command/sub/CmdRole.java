package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.command.sub.role.CmdRoleAssign;
import com.pvpindex.factions.command.sub.role.CmdRoleCreate;
import com.pvpindex.factions.command.sub.role.CmdRoleDelete;
import com.pvpindex.factions.command.sub.role.CmdRoleList;
import com.pvpindex.factions.command.sub.role.CmdRoleRename;
import com.pvpindex.factions.command.sub.role.CmdRoleSetPrefix;
import com.pvpindex.factions.command.sub.role.CmdRoleSetPriority;
import com.pvpindex.factions.service.FactionService;

/** {@code /f role ...} role management commands. */
public final class CmdRole extends FactionCommand {

    public CmdRole(final FactionService factionService) {
        super("role");
        setPermission("factions.cmd.role");
        setDescription("Manage faction roles.");
        setRequiresPlayer(true);
        addChild(new CmdRoleList(factionService));
        addChild(new CmdRoleCreate(factionService));
        addChild(new CmdRoleRename(factionService));
        addChild(new CmdRoleSetPriority(factionService));
        addChild(new CmdRoleSetPrefix(factionService));
        addChild(new CmdRoleDelete(factionService));
        addChild(new CmdRoleAssign(factionService));
    }
}
