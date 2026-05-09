package com.pvpindex.factions.registry;

import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.InviteService;
import com.pvpindex.factions.service.WarpService;

/**
 * Holds the internal service implementations used by commands and engines.
 *
 * <p>Populated by {@link com.pvpindex.factions.Bootstrap}.
 * TeamsAPI adapter instances are tracked directly in Bootstrap for shutdown purposes.
 */
public class ServiceRegistry {

    private FactionService factionService;
    private InviteService inviteService;
    private WarpService warpService;

    public void setFactionService(final FactionService service) {
        this.factionService = service;
    }

    public void setInviteService(final InviteService service) {
        this.inviteService = service;
    }

    public void setWarpService(final WarpService service) {
        this.warpService = service;
    }

    public FactionService getFactionService() { return factionService; }
    public InviteService getInviteService() { return inviteService; }
    public WarpService getWarpService() { return warpService; }
}
