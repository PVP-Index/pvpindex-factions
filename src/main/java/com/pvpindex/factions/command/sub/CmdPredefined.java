package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.command.sub.predefined.CmdPredefinedClaim;
import com.pvpindex.factions.command.sub.predefined.CmdPredefinedCreate;
import com.pvpindex.factions.command.sub.predefined.CmdPredefinedList;
import com.pvpindex.factions.command.sub.predefined.CmdPredefinedReload;
import com.pvpindex.factions.command.sub.predefined.CmdPredefinedSetHome;
import com.pvpindex.factions.service.FactionService;

/** {@code /f predefined <create|claim|sethome|reload|list>}. */
public final class CmdPredefined extends FactionCommand {

    public CmdPredefined(final FactionService factionService) {
        super("predefined");
        setAliases("prefined");
        setPermission("factions.cmd.predefined");
        setDescription("Manage predefined factions.");
        addChild(new CmdPredefinedCreate(factionService));
        addChild(new CmdPredefinedClaim());
        addChild(new CmdPredefinedSetHome());
        addChild(new CmdPredefinedReload());
        addChild(new CmdPredefinedList());
    }
}
