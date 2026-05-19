package com.pvpindex.factions.bootstrap;

import com.pvpindex.factions.api.TeamsApiRegistrar;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.config.NotificationsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.service.FactionServiceImpl;
import com.pvpindex.factions.service.FlagServiceImpl;
import com.pvpindex.factions.service.InviteServiceImpl;
import com.pvpindex.factions.service.WarpServiceImpl;

/**
 * Initializes internal services and optional TeamsAPI adapters.
 *
 * <p>TeamsAPI integration is fully isolated behind {@link TeamsApiRegistrar}.
 * The concrete implementation is loaded only via {@code Class.forName()} so
 * that this class (and therefore {@code Bootstrap}) can be loaded without
 * TeamsAPI on the server classpath.
 */
public final class ServicesBootstrapComponent extends AbstractBootstrapComponent {

    static final String REGISTRAR_CLASS = "com.pvpindex.factions.api.TeamsApiRegistrarImpl";

    @Override
    public String name() {
        return "services";
    }

    @Override
    public boolean start(final BootstrapContext context) {
        final Repositories repos = context.infra().getRepositories();
        final FactionsConfig cfg = context.infra().getConfig();
        final NotificationsConfig notifCfg = context.infra().getNotificationsConfig();

        final FactionServiceImpl factionImpl =
            new FactionServiceImpl(context.plugin(), repos, cfg, notifCfg, logger(context));
        final InviteServiceImpl inviteImpl =
            new InviteServiceImpl(factionImpl, repos, cfg, logger(context));
        final WarpServiceImpl warpImpl =
            new WarpServiceImpl(repos, cfg, logger(context));

        context.services().setFactionService(factionImpl);
        context.services().setInviteService(inviteImpl);
        context.services().setWarpService(warpImpl);

        final FlagServiceImpl flagImpl =
            new FlagServiceImpl(repos, cfg, logger(context));
        context.services().setFlagService(flagImpl);

        if (!isTeamsApiAvailable(context)) {
            logger(context).info("TeamsAPI not found — running standalone.");
            context.setTeamsApiEnabled(false);
            return true;
        }

        try {
            final TeamsApiRegistrar registrar =
                (TeamsApiRegistrar) Class.forName(REGISTRAR_CLASS)
                    .getDeclaredConstructor()
                    .newInstance();
            if (registrar.register(context.plugin(), factionImpl, inviteImpl, warpImpl)) {
                context.setTeamsRegistrar(registrar);
                context.setTeamsApiEnabled(true);
                logger(context).info("TeamsAPI integration enabled.");
            } else {
                logger(context).warning("TeamsAPI provider registration failed — running standalone.");
                context.setTeamsApiEnabled(false);
            }
        } catch (Exception e) {
            logger(context).warning("Failed to initialise TeamsAPI integration: " + e.getMessage());
            context.setTeamsApiEnabled(false);
        }

        return true;
    }

    @Override
    public void stop(final BootstrapContext context) {
        final TeamsApiRegistrar registrar = context.getTeamsRegistrar();
        if (registrar != null) {
            registrar.unregister();
            context.setTeamsRegistrar(null);
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
