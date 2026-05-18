package com.pvpindex.factions.api;

import com.pvpindex.factions.service.FactionServiceImpl;
import com.pvpindex.factions.service.InviteServiceImpl;
import com.pvpindex.factions.service.WarpServiceImpl;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.api.TeamsClaimService;
import com.skyblockexp.teamsapi.api.TeamsInviteService;
import com.skyblockexp.teamsapi.api.TeamsPowerService;
import com.skyblockexp.teamsapi.api.TeamsRelationService;
import com.skyblockexp.teamsapi.api.TeamsService;
import com.skyblockexp.teamsapi.api.TeamsWarpService;
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
    private TeamsRelationService relationAdapter;

    @Override
    public boolean register(final Plugin plugin, final FactionServiceImpl factionImpl,
            final InviteServiceImpl inviteImpl, final WarpServiceImpl warpImpl) {
        teamsAdapter = new FactionsTeamsService(factionImpl);
        inviteAdapter = new FactionsTeamsInviteService(inviteImpl);
        warpAdapter = new FactionsTeamsWarpService(warpImpl, factionImpl);
        claimAdapter = new FactionsTeamsClaimService(factionImpl);
        powerAdapter = new FactionsTeamsPowerService(factionImpl);
        relationAdapter = new FactionsTeamsRelationService(factionImpl);

        try {
            TeamsAPI.registerProvider(plugin, teamsAdapter);
            TeamsAPI.registerInviteProvider(plugin, inviteAdapter);
            TeamsAPI.registerWarpProvider(plugin, warpAdapter);
            TeamsAPI.registerClaimProvider(plugin, claimAdapter);
            TeamsAPI.registerPowerProvider(plugin, powerAdapter);
            TeamsAPI.registerRelationProvider(plugin, relationAdapter);
            return true;
        } catch (Exception e) {
            unregister();
            return false;
        }
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
                TeamsAPI.unregisterRelationProvider(relationAdapter);
            } catch (Exception ignored) { }
            relationAdapter = null;
        }
    }
}
