package com.pvpindex.factions.command.sub.admin;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.util.MsgUtil;
import org.bukkit.entity.Player;

/** {@code /fa bypass} */
public final class CmdAdminBypass extends FactionCommand {

    public CmdAdminBypass() {
        super("bypass");
        setPermission("factions.admin");
        setDescription("Toggle admin protection bypass mode.");
        setRequiresPlayer(true);
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        try {
            final PlayerModel model = ctx.getRepos().players().findOrCreate(player.getUniqueId().toString());
            model.setOverriding(!model.isOverriding());
            ctx.getRepos().players().save(model);
            MsgUtil.send(player, model.isOverriding()
                ? "<gold>[Admin] Bypass enabled."
                : "<gold>[Admin] Bypass disabled.");
        } catch (StorageException e) {
            MsgUtil.send(player, "<red>Failed to toggle bypass.");
        }
    }
}

