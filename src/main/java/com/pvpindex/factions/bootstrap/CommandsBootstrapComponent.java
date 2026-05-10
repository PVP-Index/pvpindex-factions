package com.pvpindex.factions.bootstrap;

import com.pvpindex.factions.command.AdminCommandExecutor;
import com.pvpindex.factions.command.AdminTabCompleter;
import com.pvpindex.factions.command.FactionCommandExecutor;
import com.pvpindex.factions.command.FactionTabCompleter;
import com.pvpindex.factions.command.sub.CmdClaim;
import com.pvpindex.factions.command.sub.CmdCreate;
import com.pvpindex.factions.command.sub.CmdDemote;
import com.pvpindex.factions.command.sub.CmdDesc;
import com.pvpindex.factions.command.sub.CmdDisband;
import com.pvpindex.factions.command.sub.CmdFly;
import com.pvpindex.factions.command.sub.CmdGui;
import com.pvpindex.factions.command.sub.CmdHelp;
import com.pvpindex.factions.command.sub.CmdHome;
import com.pvpindex.factions.command.sub.CmdInfo;
import com.pvpindex.factions.command.sub.CmdInvite;
import com.pvpindex.factions.command.sub.CmdJoin;
import com.pvpindex.factions.command.sub.CmdKick;
import com.pvpindex.factions.command.sub.CmdLeader;
import com.pvpindex.factions.command.sub.CmdLeave;
import com.pvpindex.factions.command.sub.CmdList;
import com.pvpindex.factions.command.sub.CmdMap;
import com.pvpindex.factions.command.sub.CmdNotify;
import com.pvpindex.factions.command.sub.CmdPromote;
import com.pvpindex.factions.command.sub.CmdRelation;
import com.pvpindex.factions.command.sub.CmdRename;
import com.pvpindex.factions.command.sub.CmdSetHome;
import com.pvpindex.factions.command.sub.CmdTop;
import com.pvpindex.factions.command.sub.CmdUnclaim;
import com.pvpindex.factions.command.sub.CmdUnsetHome;
import com.pvpindex.factions.command.sub.admin.CmdAdminBypass;
import com.pvpindex.factions.command.sub.admin.CmdAdminClaim;
import com.pvpindex.factions.command.sub.admin.CmdAdminDisband;
import com.pvpindex.factions.command.sub.admin.CmdAdminHelp;
import com.pvpindex.factions.command.sub.admin.CmdAdminReload;
import com.pvpindex.factions.command.sub.admin.CmdAdminUnclaim;
import com.pvpindex.factions.command.sub.bank.CmdBank;
import com.pvpindex.factions.command.sub.power.CmdPower;
import com.pvpindex.factions.command.sub.warp.CmdWarp;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.engine.EngineChunkChange;
import com.pvpindex.factions.engine.EngineEconomy;
import com.pvpindex.factions.registry.CommandRegistry;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.InviteService;
import com.pvpindex.factions.service.WarpService;
import org.bukkit.command.PluginCommand;

/**
 * Registers player/admin command trees and tab completion.
 */
public final class CommandsBootstrapComponent extends AbstractBootstrapComponent {

    @Override
    public String name() {
        return "commands";
    }

    @Override
    public boolean start(final BootstrapContext context) {
        final Repositories repos = context.infra().getRepositories();
        final FactionsConfig cfg = context.infra().getConfig();
        final FactionService factionSvc = context.services().getFactionService();
        final InviteService inviteSvc = context.services().getInviteService();
        final WarpService warpSvc = context.services().getWarpService();
        final EngineChunkChange chunkChange = context.engines().getChunkChange();
        final EngineEconomy economy = context.engines().getEconomy();
        final com.pvpindex.factions.engine.AutoTerritoryModeCache autoModeCache =
            context.engines().getAutoTerritoryModeCache();

        final CommandRegistry commandRegistry = new CommandRegistry();
        commandRegistry.register(new CmdCreate(factionSvc));
        commandRegistry.register(new CmdDisband(factionSvc));
        commandRegistry.register(new CmdInfo(factionSvc));
        commandRegistry.register(new CmdInvite(factionSvc, inviteSvc, repos));
        commandRegistry.register(new CmdJoin(factionSvc, inviteSvc));
        commandRegistry.register(new CmdLeave(factionSvc));
        commandRegistry.register(new CmdKick(factionSvc));
        commandRegistry.register(new CmdClaim(chunkChange, context.infra().getTerritoryGuard(), autoModeCache));
        commandRegistry.register(new CmdUnclaim(chunkChange, factionSvc, autoModeCache));
        commandRegistry.register(new CmdHome(factionSvc, context.infra().getEssentialsInterop()));
        commandRegistry.register(new CmdSetHome(factionSvc, context.infra().getTerritoryGuard()));
        commandRegistry.register(new CmdUnsetHome(factionSvc));
        commandRegistry.register(new CmdFly(factionSvc));
        commandRegistry.register(new CmdRelation(factionSvc));
        commandRegistry.register(new CmdWarp(factionSvc, warpSvc, context.infra().getTerritoryGuard()));
        commandRegistry.register(new CmdBank(factionSvc, economy));
        commandRegistry.register(new CmdPower(context.infra().getVaultEconomy(), cfg, repos));
        commandRegistry.register(new CmdList(factionSvc));
        commandRegistry.register(new CmdMap());
        commandRegistry.register(new CmdNotify());
        commandRegistry.register(new CmdGui(context.engines().getFactionsGuiManager(), context.infra().getGuiConfig()));
        commandRegistry.register(new CmdLeader(factionSvc));
        commandRegistry.register(new CmdPromote(factionSvc));
        commandRegistry.register(new CmdDemote(factionSvc));
        commandRegistry.register(new CmdRename(factionSvc));
        commandRegistry.register(new CmdDesc(factionSvc));
        commandRegistry.register(new CmdTop(factionSvc));
        commandRegistry.register(new CmdHelp(commandRegistry));

        final FactionCommandExecutor executor = new FactionCommandExecutor(
            context.plugin(), commandRegistry, repos, cfg, context.engines().getFactionsGuiManager(), logger(context));
        final FactionTabCompleter tabCompleter = new FactionTabCompleter(
            context.plugin(), commandRegistry, repos, cfg, logger(context));

        for (final String alias : new String[]{"f", "faction", "factions"}) {
            final PluginCommand cmd = context.plugin().getCommand(alias);
            if (cmd != null) {
                cmd.setExecutor(executor);
                cmd.setTabCompleter(tabCompleter);
            }
        }

        final CommandRegistry adminRegistry = new CommandRegistry();
        adminRegistry.register(new CmdAdminBypass());
        adminRegistry.register(new CmdAdminClaim());
        adminRegistry.register(new CmdAdminUnclaim());
        adminRegistry.register(new CmdAdminDisband(factionSvc));
        adminRegistry.register(new CmdAdminReload());
        adminRegistry.register(new CmdAdminHelp(adminRegistry));

        final AdminCommandExecutor adminExecutor = new AdminCommandExecutor(
            context.plugin(), adminRegistry, repos, cfg, logger(context));
        final AdminTabCompleter adminTabCompleter = new AdminTabCompleter(
            context.plugin(), adminRegistry, repos, cfg, logger(context));

        for (final String alias : new String[]{"fa", "factionadmin"}) {
            final PluginCommand cmd = context.plugin().getCommand(alias);
            if (cmd != null) {
                cmd.setExecutor(adminExecutor);
                cmd.setTabCompleter(adminTabCompleter);
            }
        }
        return true;
    }
}
