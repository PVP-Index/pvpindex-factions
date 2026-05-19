package com.pvpindex.factions.api;

import com.pvpindex.factions.service.FactionServiceImpl;
import com.pvpindex.factions.service.InviteServiceImpl;
import com.pvpindex.factions.service.WarpServiceImpl;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.api.TeamsClaimService;
import com.skyblockexp.teamsapi.api.TeamsInviteService;
import com.skyblockexp.teamsapi.api.TeamsPowerService;
import com.skyblockexp.teamsapi.api.TeamsService;
import com.skyblockexp.teamsapi.api.TeamsWarpService;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

/**
 * Concrete TeamsAPI integration registrar.
 *
 * <p><strong>Never reference this class directly from bootstrap or any other code
 * that is loaded unconditionally.</strong> It must only be instantiated via
 * {@code Class.forName("com.pvpindex.factions.api.TeamsApiRegistrarImpl")}
 * after TeamsAPI has been confirmed present on the server. Loading this class
 * when TeamsAPI is absent will throw {@link NoClassDefFoundError}.
 */
public final class TeamsApiRegistrarImpl implements TeamsApiRegistrar {

    private TeamsService teamsAdapter;
    private TeamsInviteService inviteAdapter;
    private TeamsWarpService warpAdapter;
    private TeamsClaimService claimAdapter;
    private TeamsPowerService powerAdapter;
    /** Stored as Object to avoid a bytecode-level reference to TeamsRelationService (TeamsAPI 1.6+). */
    private Object relationAdapter;
    /** Stored as Object to avoid a bytecode-level reference to TeamsNotificationService (TeamsAPI 1.7+). */
    private Object notificationAdapter;
    /** Stored as Object to avoid a bytecode-level reference to TeamsPowerHistoryService (TeamsAPI 1.8+). */
    private Object powerHistoryAdapter;

    @Override
    public boolean register(final Plugin plugin, final FactionServiceImpl factionImpl,
            final InviteServiceImpl inviteImpl, final WarpServiceImpl warpImpl) {
        teamsAdapter = new FactionsTeamsService(factionImpl);
        inviteAdapter = new FactionsTeamsInviteService(inviteImpl);
        warpAdapter = new FactionsTeamsWarpService(warpImpl, factionImpl);
        claimAdapter = new FactionsTeamsClaimService(factionImpl);
        powerAdapter = new FactionsTeamsPowerService(factionImpl);

        try {
            TeamsAPI.registerProvider(plugin, teamsAdapter);
            TeamsAPI.registerInviteProvider(plugin, inviteAdapter);
            TeamsAPI.registerWarpProvider(plugin, warpAdapter);
            TeamsAPI.registerClaimProvider(plugin, claimAdapter);
            TeamsAPI.registerPowerProvider(plugin, powerAdapter);
        } catch (Exception e) {
            unregister();
            return false;
        }

        // TeamsRelationService was introduced in TeamsAPI 1.6. Load it via reflection so
        // the bytecode verifier never resolves it when TeamsAPI 1.5.x is installed.
        try {
            final Class<?> relSvcClass =
                    Class.forName("com.skyblockexp.teamsapi.api.TeamsRelationService");
            final Object relAdapter =
                    Class.forName("com.pvpindex.factions.api.FactionsTeamsRelationService")
                            .getDeclaredConstructor(FactionServiceImpl.class)
                            .newInstance(factionImpl);
            TeamsAPI.class.getMethod("registerRelationProvider", Plugin.class, relSvcClass)
                    .invoke(null, plugin, relAdapter);
            relationAdapter = relAdapter;
        } catch (ClassNotFoundException ignored) {
            // TeamsAPI < 1.6 installed — relation provider not available
        } catch (ReflectiveOperationException e) {
            Logger.getLogger("PvPIndexFactions")
                    .warning("Could not register TeamsAPI relation provider: " + e.getMessage());
        }

        // TeamsNotificationService was introduced in TeamsAPI 1.7. Load it via reflection so
        // the bytecode verifier never resolves it when TeamsAPI 1.6.x is installed.
        try {
            final Class<?> notifSvcClass =
                    Class.forName("com.skyblockexp.teamsapi.api.TeamsNotificationService");
            final Object notifAdapter =
                    Class.forName("com.pvpindex.factions.api.FactionsTeamsNotificationService")
                            .getDeclaredConstructor(
                                    Class.forName("com.pvpindex.factions.data.Repositories"),
                                    java.util.logging.Logger.class)
                            .newInstance(factionImpl.getRepos(), factionImpl.getLogger());
            TeamsAPI.class.getMethod("registerNotificationProvider", Plugin.class, notifSvcClass)
                    .invoke(null, plugin, notifAdapter);
            notificationAdapter = notifAdapter;
        } catch (ClassNotFoundException ignored) {
            // TeamsAPI < 1.7 installed — notification provider not available
        } catch (ReflectiveOperationException e) {
            Logger.getLogger("PvPIndexFactions")
                    .warning("Could not register TeamsAPI notification provider: " + e.getMessage());
        }

        // TeamsPowerHistoryService was introduced in TeamsAPI 1.8. Load it via reflection so
        // the bytecode verifier never resolves it when TeamsAPI 1.7.x is installed.
        try {
            final Class<?> phSvcClass =
                    Class.forName("com.skyblockexp.teamsapi.api.TeamsPowerHistoryService");
            final Object phAdapter =
                    Class.forName("com.pvpindex.factions.api.FactionsTeamsPowerHistoryService")
                            .getDeclaredConstructor(
                                    Class.forName("com.pvpindex.factions.service.FactionServiceImpl"))
                            .newInstance(factionImpl);
            TeamsAPI.class.getMethod("registerPowerHistoryProvider", Plugin.class, phSvcClass)
                    .invoke(null, plugin, phAdapter);
            powerHistoryAdapter = phAdapter;
        } catch (ClassNotFoundException ignored) {
            // TeamsAPI < 1.8 installed — power history provider not available
        } catch (ReflectiveOperationException e) {
            Logger.getLogger("PvPIndexFactions")
                    .warning("Could not register TeamsAPI power history provider: " + e.getMessage());
        }

        return true;
    }

    @Override
    public void unregister() {
        if (teamsAdapter != null) {
            try {
                TeamsAPI.unregisterProvider(teamsAdapter);
            } catch (Exception ignored) { }
            teamsAdapter = null;
        }
        if (inviteAdapter != null) {
            try {
                TeamsAPI.unregisterInviteProvider(inviteAdapter);
            } catch (Exception ignored) { }
            inviteAdapter = null;
        }
        if (warpAdapter != null) {
            try {
                TeamsAPI.unregisterWarpProvider(warpAdapter);
            } catch (Exception ignored) { }
            warpAdapter = null;
        }
        if (claimAdapter != null) {
            try {
                TeamsAPI.unregisterClaimProvider(claimAdapter);
            } catch (Exception ignored) { }
            claimAdapter = null;
        }
        if (powerAdapter != null) {
            try {
                TeamsAPI.unregisterPowerProvider(powerAdapter);
            } catch (Exception ignored) { }
            powerAdapter = null;
        }
        if (relationAdapter != null) {
            try {
                final Class<?> relSvcClass =
                        Class.forName("com.skyblockexp.teamsapi.api.TeamsRelationService");
                TeamsAPI.class.getMethod("unregisterRelationProvider", relSvcClass)
                        .invoke(null, relationAdapter);
            } catch (ReflectiveOperationException ignored) { }
            relationAdapter = null;
        }
        if (notificationAdapter != null) {
            try {
                final Class<?> notifSvcClass =
                        Class.forName("com.skyblockexp.teamsapi.api.TeamsNotificationService");
                TeamsAPI.class.getMethod("unregisterNotificationProvider", notifSvcClass)
                        .invoke(null, notificationAdapter);
            } catch (ReflectiveOperationException ignored) { }
            notificationAdapter = null;
        }
        if (powerHistoryAdapter != null) {
            try {
                final Class<?> phSvcClass =
                        Class.forName("com.skyblockexp.teamsapi.api.TeamsPowerHistoryService");
                TeamsAPI.class.getMethod("unregisterPowerHistoryProvider", phSvcClass)
                        .invoke(null, powerHistoryAdapter);
            } catch (ReflectiveOperationException ignored) { }
            powerHistoryAdapter = null;
        }
    }
}
