package com.gyvex.pvpindex.factions.bootstrap;

import com.gyvex.pvpindex.factions.api.FactionsTeamsInviteService;
import com.gyvex.pvpindex.factions.api.FactionsTeamsService;
import com.gyvex.pvpindex.factions.api.FactionsTeamsWarpService;
import com.gyvex.pvpindex.factions.config.FactionsConfig;
import com.gyvex.pvpindex.factions.data.Repositories;
import com.gyvex.pvpindex.factions.service.FactionServiceImpl;
import com.gyvex.pvpindex.factions.service.InviteServiceImpl;
import com.gyvex.pvpindex.factions.service.WarpServiceImpl;

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

        try {
            com.skyblockexp.teamsapi.api.TeamsAPI.registerProvider(context.plugin(), teamsAdapter);
            com.skyblockexp.teamsapi.api.TeamsAPI.registerInviteProvider(context.plugin(), inviteAdapter);
            com.skyblockexp.teamsapi.api.TeamsAPI.registerWarpProvider(context.plugin(), warpAdapter);
            context.setTeamsAdapter(teamsAdapter);
            context.setInviteAdapter(inviteAdapter);
            context.setWarpAdapter(warpAdapter);
            context.setTeamsApiEnabled(true);
            logger(context).info("TeamsAPI integration enabled.");
        } catch (Exception e) {
            logger(context).warning("Failed to register TeamsAPI providers: " + e.getMessage());
            context.setTeamsAdapter(null);
            context.setInviteAdapter(null);
            context.setWarpAdapter(null);
            context.setTeamsApiEnabled(false);
        }

        return true;
    }

    @Override
    public void stop(final BootstrapContext context) {
        if (context.getTeamsAdapter() != null) {
            try {
                com.skyblockexp.teamsapi.api.TeamsAPI.unregisterProvider(context.getTeamsAdapter());
            } catch (Exception ignored) { }
        }
        if (context.getInviteAdapter() != null) {
            try {
                com.skyblockexp.teamsapi.api.TeamsAPI.unregisterInviteProvider(context.getInviteAdapter());
            } catch (Exception ignored) { }
        }
        if (context.getWarpAdapter() != null) {
            try {
                com.skyblockexp.teamsapi.api.TeamsAPI.unregisterWarpProvider(context.getWarpAdapter());
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
