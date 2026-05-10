package com.pvpindex.factions.bootstrap;

import com.pvpindex.factions.api.FactionsTeamsClaimService;
import com.pvpindex.factions.api.FactionsTeamsInviteService;
import com.pvpindex.factions.api.FactionsTeamsPowerService;
import com.pvpindex.factions.api.FactionsTeamsService;
import com.pvpindex.factions.api.FactionsTeamsWarpService;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.service.FactionServiceImpl;
import com.pvpindex.factions.service.InviteServiceImpl;
import com.pvpindex.factions.service.WarpServiceImpl;

/**
 * Initializes internal services and optional TeamsAPI adapters.
 */
public final class ServicesBootstrapComponent extends AbstractBootstrapComponent {

    @Override
    public String name() {
        return "services";
    }

    @Override
    public boolean start(final BootstrapContext context) {
        final Repositories repos = context.infra().getRepositories();
        final FactionsConfig cfg = context.infra().getConfig();

        final FactionServiceImpl factionImpl =
            new FactionServiceImpl(context.plugin(), repos, cfg, logger(context));
        final InviteServiceImpl inviteImpl =
            new InviteServiceImpl(factionImpl, repos, cfg, logger(context));
        final WarpServiceImpl warpImpl =
            new WarpServiceImpl(repos, cfg, logger(context));

        context.services().setFactionService(factionImpl);
        context.services().setInviteService(inviteImpl);
        context.services().setWarpService(warpImpl);

        if (!isTeamsApiAvailable(context)) {
            logger(context).info("TeamsAPI not found — running standalone.");
            context.setTeamsApiEnabled(false);
            return true;
        }

        final FactionsTeamsService teamsAdapter = new FactionsTeamsService(factionImpl);
        final FactionsTeamsInviteService inviteAdapter = new FactionsTeamsInviteService(inviteImpl);
        final FactionsTeamsWarpService warpAdapter = new FactionsTeamsWarpService(warpImpl, factionImpl);
        final FactionsTeamsClaimService claimAdapter = new FactionsTeamsClaimService(factionImpl);
        final FactionsTeamsPowerService powerAdapter = new FactionsTeamsPowerService(factionImpl);

        try {
            com.skyblockexp.teamsapi.api.TeamsAPI.registerProvider(context.plugin(), teamsAdapter);
            com.skyblockexp.teamsapi.api.TeamsAPI.registerInviteProvider(context.plugin(), inviteAdapter);
            com.skyblockexp.teamsapi.api.TeamsAPI.registerWarpProvider(context.plugin(), warpAdapter);
            com.skyblockexp.teamsapi.api.TeamsAPI.registerClaimProvider(context.plugin(), claimAdapter);
            com.skyblockexp.teamsapi.api.TeamsAPI.registerPowerProvider(context.plugin(), powerAdapter);
            context.setTeamsAdapter(teamsAdapter);
            context.setInviteAdapter(inviteAdapter);
            context.setWarpAdapter(warpAdapter);
            context.setClaimAdapter(claimAdapter);
            context.setPowerAdapter(powerAdapter);
            context.setTeamsApiEnabled(true);
            logger(context).info("TeamsAPI integration enabled.");
        } catch (Exception e) {
            logger(context).warning("Failed to register TeamsAPI providers: " + e.getMessage());
            context.setTeamsAdapter(null);
            context.setInviteAdapter(null);
            context.setWarpAdapter(null);
            context.setClaimAdapter(null);
            context.setPowerAdapter(null);
            context.setTeamsApiEnabled(false);
        }

        return true;
    }

    @Override
    public void stop(final BootstrapContext context) {
        if (context.getTeamsAdapter() != null) {
            try {
                com.skyblockexp.teamsapi.api.TeamsAPI.unregisterProvider(
                    (com.skyblockexp.teamsapi.api.TeamsService) context.getTeamsAdapter());
            } catch (Exception ignored) { }
        }
        if (context.getInviteAdapter() != null) {
            try {
                com.skyblockexp.teamsapi.api.TeamsAPI.unregisterInviteProvider(
                    (com.skyblockexp.teamsapi.api.TeamsInviteService) context.getInviteAdapter());
            } catch (Exception ignored) { }
        }
        if (context.getWarpAdapter() != null) {
            try {
                com.skyblockexp.teamsapi.api.TeamsAPI.unregisterWarpProvider(
                    (com.skyblockexp.teamsapi.api.TeamsWarpService) context.getWarpAdapter());
            } catch (Exception ignored) { }
        }
        if (context.getClaimAdapter() != null) {
            try {
                com.skyblockexp.teamsapi.api.TeamsAPI.unregisterClaimProvider(
                    (com.skyblockexp.teamsapi.api.TeamsClaimService) context.getClaimAdapter());
            } catch (Exception ignored) { }
        }
        if (context.getPowerAdapter() != null) {
            try {
                com.skyblockexp.teamsapi.api.TeamsAPI.unregisterPowerProvider(
                    (com.skyblockexp.teamsapi.api.TeamsPowerService) context.getPowerAdapter());
            } catch (Exception ignored) { }
        }
    }

    private boolean isTeamsApiAvailable(final BootstrapContext context) {
        try {
            Class.forName("com.skyblockexp.teamsapi.api.TeamsAPI");
            return context.plugin().getServer().getPluginManager().getPlugin("TeamsAPI") != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
