package com.pvpindex.factions.command.sub.flag;

import com.pvpindex.factions.FactionFlag;
import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.FlagService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Map;
import java.util.Optional;
import org.bukkit.entity.Player;

/** {@code /f flag list} — List all faction flags with their current values. */
public final class CmdFlagList extends FactionCommand {

    private final FactionService factionService;
    private final FlagService flagService;

    public CmdFlagList(final FactionService factionService, final FlagService flagService) {
        super("list");
        setPermission("factions.cmd.flag");
        setDescription("List all faction flags.");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.flagService = flagService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> factionOpt = CommandGuards.requireFaction(player, factionService);
        if (factionOpt.isEmpty()) {
            return;
        }
        final FactionModel faction = factionOpt.get();
        MsgUtil.sendKey(player, "flag.list-header",
                "<gold><bold>Faction Flags</bold></gold>", "faction", faction.getName());
        final Map<FactionFlag, Boolean> flags = flagService.getAllFlags(faction);
        for (final FactionFlag flag : FactionFlag.values()) {
            final boolean value = flags.getOrDefault(flag, flag.getDefaultValue());
            final String editNote = flagService.isFlagEditable(flag) ? "" : " <dark_gray>(locked)</dark_gray>";
            if (value) {
                MsgUtil.sendKey(player, "flag.entry-on",
                        "<gray>  {flag}: <green>ON</green>{edit_note}",
                        "flag", flag.getId(), "edit_note", editNote);
            } else {
                MsgUtil.sendKey(player, "flag.entry-off",
                        "<gray>  {flag}: <red>OFF</red>{edit_note}",
                        "flag", flag.getId(), "edit_note", editNote);
            }
        }
    }
}
