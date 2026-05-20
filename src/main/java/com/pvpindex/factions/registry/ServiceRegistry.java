package com.pvpindex.factions.registry;

import com.pvpindex.factions.service.AuditService;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.FlagService;
import com.pvpindex.factions.service.InviteService;
import com.pvpindex.factions.service.MergeService;
import com.pvpindex.factions.service.PowerService;
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
    private FlagService flagService;
    private AuditService auditService;
    private PowerService powerService;
    private MergeService mergeService;

    public void setFactionService(final FactionService service) {
        this.factionService = service;
    }

    public void setInviteService(final InviteService service) {
        this.inviteService = service;
    }

    public void setWarpService(final WarpService service) {
        this.warpService = service;
    }

    public void setFlagService(final FlagService service) {
        this.flagService = service;
    }

    public void setAuditService(final AuditService service) {
        this.auditService = service;
    }

    public void setPowerService(final PowerService service) {
        this.powerService = service;
    }

    public void setMergeService(final MergeService service) {
        this.mergeService = service;
    }

    public FactionService getFactionService() { return factionService; }
    public InviteService getInviteService() { return inviteService; }
    public WarpService getWarpService() { return warpService; }
    public FlagService getFlagService() { return flagService; }
    public AuditService getAuditService() { return auditService; }
    public PowerService getPowerService() { return powerService; }
    public MergeService getMergeService() { return mergeService; }
}
