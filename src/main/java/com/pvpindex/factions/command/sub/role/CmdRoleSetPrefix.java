package com.pvpindex.factions.command.sub.role;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import org.bukkit.entity.Player;

/** {@code /f role setprefix <role> <prefix|none>}. */
public final class CmdRoleSetPrefix extends FactionCommand {

    private final FactionService factionService;

    public CmdRoleSetPrefix(final FactionService factionService) {
        super("setprefix");
        setPermission("factions.cmd.role.edit");
        setDescription("Set role chat prefix.");
        setRequiredArgs("<role>", "<prefix|none>");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player actor = (Player) ctx.getSender();
        if (!CommandGuards.requireOfficerOrAbove(actor, factionService)) {
            return;
        }
        if (!factionService.isRolePrefixesEnabled() || !factionService.isRoleFactionOverridesEnabled()) {
            MsgUtil.sendKey(actor, "custom.role.prefix-disabled",
                "<red>Role prefixes are disabled. Enable roles.prefix.enabled and roles.overrides.enabled.");
            return;
        }
        final String prefixArg = ctx.arg(1);
        final String prefix = "none".equalsIgnoreCase(prefixArg) ? null : prefixArg;
        if (factionService.setRolePrefix(actor.getUniqueId(), ctx.arg(0), prefix)) {
            MsgUtil.sendKey(actor, "custom.role.prefix-success",
                "<green>Updated role <white>{name}<green> prefix.",
                "name", ctx.arg(0));
            return;
        }
        MsgUtil.sendKey(actor, "custom.role.prefix-failed", "<red>Could not update that role prefix.");
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (!ctx.isPlayer()) {
            return List.of();
        }
        if (argIndex == 0) {
            return factionService.listRoles(((Player) ctx.getSender()).getUniqueId()).stream().map(RankModel::getName).toList();
        }
        if (argIndex == 1) {
            return List.of("none");
        }
        return List.of();
    }
}
