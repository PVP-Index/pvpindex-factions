package com.pvpindex.factions.service;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.service.PowerService.Request;
import com.pvpindex.factions.service.PowerService.Result;
import com.pvpindex.factions.service.PowerService.Source;
import com.pvpindex.factions.service.PowerService.ZoneContext;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Default {@link PowerService} implementation. */
public final class PowerServiceImpl implements PowerService {

    private final Repositories repos;
    private final FactionsConfig config;

    public PowerServiceImpl(final Repositories repos, final FactionsConfig config) {
        this.repos = repos;
        this.config = config;
    }

    @Override
    public Optional<PlayerModel> findPlayerByNameOrUuid(final String input) throws StorageException {
        try {
            return repos.players().find(UUID.fromString(input).toString());
        } catch (IllegalArgumentException ignored) {
            @SuppressWarnings("deprecation")
            final OfflinePlayer op = Bukkit.getOfflinePlayer(input);
            if (!op.hasPlayedBefore() && !op.isOnline()) {
                return Optional.empty();
            }
            return repos.players().find(op.getUniqueId().toString());
        }
    }

    @Override
    public Result apply(final Request request) throws StorageException {
        final Optional<PlayerModel> playerOpt = repos.players().find(request.playerId());
        if (playerOpt.isEmpty()) {
            return new Result(false, false, 0.0, request.baseDelta(), 0.0, 0.0, "PLAYER_NOT_FOUND");
        }
        final PlayerModel model = playerOpt.get();
        final double before = model.getPower();
        final double requestedDelta = request.baseDelta();

        if (!request.bypassFreeze() && model.isPowerFrozen() && sourceAffectedByFreeze(request.source())) {
            notifyBlockedByFreeze(model.getId(), request.source(), request.actorName());
            return new Result(false, true, before, requestedDelta, 0.0, before, "FROZEN");
        }

        final double sourceAmount = sourceAmount(request.source(), request.baseDelta());
        if (!sourceEnabled(request.source())) {
            return new Result(false, false, before, requestedDelta, 0.0, before, "SOURCE_DISABLED");
        }

        double delta = sourceAmount;
        if (request.source() == Source.DEATH || request.source() == Source.KILL) {
            delta = applyMultipliers(delta, request.world(), request.zone());
        }
        delta = applyEventClamp(delta);

        final double minPower = config.getPowerMin();
        final double maxPower = config.getPowerMax();
        final double after = Math.max(minPower, Math.min(maxPower, before + delta));
        final double effectiveDelta = after - before;

        if (Math.abs(effectiveDelta) < 0.00001D) {
            return new Result(false, false, before, requestedDelta, 0.0, before, "NO_CHANGE");
        }

        model.setPower(after);
        repos.players().save(model);
        repos.powerHistory().record(model.getId(), effectiveDelta, reasonCode(request), after);
        notifyPowerChange(model.getId(), request, before, effectiveDelta, after);
        return new Result(true, false, before, requestedDelta, effectiveDelta, after, reasonCode(request));
    }

    @Override
    public double getFactionPower(final String factionId) throws StorageException {
        final Optional<FactionModel> faction = repos.factions().find(factionId);
        double total = faction.map(FactionModel::getPowerBoost).orElse(0.0);
        for (final PlayerModel pm : repos.players().findByFactionId(factionId)) {
            if (isExcludedByInactivity(pm)) {
                continue;
            }
            total += pm.getPower();
        }
        return total;
    }

    private boolean sourceEnabled(final Source source) {
        return switch (source) {
            case REGEN_ONLINE -> config.isPowerSourceRegenOnlineEnabled();
            case REGEN_OFFLINE -> config.isPowerSourceRegenOfflineEnabled();
            case DEATH -> config.isPowerSourceDeathLossEnabled();
            case KILL -> config.isPowerSourceKillGainEnabled();
            case BUY -> config.isPowerBuyEnabled() && config.isPowerSourceBuyEnabled();
            case ADMIN_SET, ADMIN_ADD, ADMIN_REMOVE, ADMIN_RESET -> true;
        };
    }

    private double sourceAmount(final Source source, final double requested) {
        return switch (source) {
            case REGEN_ONLINE -> config.getPowerSourceRegenOnlineAmount();
            case REGEN_OFFLINE -> config.getPowerSourceRegenOfflineAmount();
            case DEATH -> -Math.abs(config.getPowerSourceDeathLossAmount());
            case KILL -> Math.abs(config.getPowerSourceKillGainAmount());
            case BUY -> requested;
            case ADMIN_SET, ADMIN_ADD, ADMIN_REMOVE, ADMIN_RESET -> requested;
        };
    }

    private String reasonCode(final Request request) {
        if (request.reason() != null && !request.reason().isBlank()) {
            return request.reason().trim();
        }
        return request.source().name();
    }

    private double applyMultipliers(final double delta, final String world, final ZoneContext zone) {
        double result = delta;
        result *= config.getPowerWorldMultiplier(world);
        result *= config.getPowerZoneMultiplier(zone.name().toLowerCase(Locale.ROOT));
        return result;
    }

    private double applyEventClamp(final double delta) {
        final double maxAbs = config.getPowerMaxChangePerEvent();
        if (maxAbs <= 0) {
            return delta;
        }
        return Math.max(-maxAbs, Math.min(maxAbs, delta));
    }

    private boolean sourceAffectedByFreeze(final Source source) {
        return switch (source) {
            case REGEN_ONLINE, REGEN_OFFLINE -> config.isPowerFreezeBlocksRegen();
            case DEATH, KILL, BUY -> config.isPowerFreezeBlocksAutomatic();
            case ADMIN_SET, ADMIN_ADD, ADMIN_REMOVE, ADMIN_RESET -> false;
        };
    }

    private boolean isExcludedByInactivity(final PlayerModel pm) {
        if (!config.isPowerInactiveExclusionEnabled()) {
            return false;
        }
        final long inactiveMs = config.getPowerInactiveDays() * 24L * 3600L * 1000L;
        final long last = pm.getLastActivity();
        return last > 0 && System.currentTimeMillis() - last > inactiveMs;
    }

    private void notifyBlockedByFreeze(final String playerId, final Source source, final String actorName) {
        final Player player = Bukkit.getPlayer(UUID.fromString(playerId));
        if (player != null && config.isPowerNotifyActor()) {
            MsgUtil.sendKey(player, "power.blocked-frozen",
                "<yellow>Power change blocked while frozen. Source: <white>{source}</white>.",
                "source", source.name());
        }
        if (config.isPowerNotifyStaff()) {
            final String msg = MsgUtil.replace(MsgUtil.message("power.staff-blocked-frozen",
                    "<gray>[Power] <white>{player}</white> blocked by freeze. Source: <white>{source}</white>."),
                "player", player != null ? player.getName() : playerId,
                "source", source.name(),
                "actor", actorName == null ? "system" : actorName);
            broadcastStaff(msg);
        }
    }

    private void notifyPowerChange(
        final String playerId,
        final Request request,
        final double before,
        final double delta,
        final double after
    ) throws StorageException {
        final String deltaText = String.format(Locale.ROOT, "%.2f", delta);
        final String beforeText = String.format(Locale.ROOT, "%.2f", before);
        final String afterText = String.format(Locale.ROOT, "%.2f", after);
        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerId));
        final String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : playerId;
        final String reason = request.reason() == null ? "" : request.reason();
        final String source = request.source().name();
        final String zone = request.zone() == null ? "unknown" : request.zone().name().toLowerCase(Locale.ROOT);
        final String world = request.world() == null ? "unknown" : request.world();

        if (config.isPowerNotifyActor()) {
            final Player targetPlayer = Bukkit.getPlayer(UUID.fromString(playerId));
            if (targetPlayer != null) {
                MsgUtil.sendKey(targetPlayer, "power.change-actor",
                    "<gray>Power <white>{before}</white> -> <white>{after}</white> (<yellow>{delta}</yellow>)",
                    "before", beforeText, "after", afterText, "delta", deltaText);
            }
        }

        if (config.isPowerNotifyFaction()) {
            final Optional<PlayerModel> pm = repos.players().find(playerId);
            if (pm.isPresent() && pm.get().isInFaction()) {
                final List<PlayerModel> members = repos.players().findByFactionId(pm.get().getFactionId());
                final String msg = MsgUtil.replace(MsgUtil.message("power.change-faction",
                        "<gray>[Faction] <white>{player}</white>: <yellow>{delta}</yellow> (<white>{after}</white>)"),
                    "player", playerName, "delta", deltaText, "after", afterText);
                for (final PlayerModel member : members) {
                    final Player online = Bukkit.getPlayer(UUID.fromString(member.getId()));
                    if (online != null) {
                        MsgUtil.send(online, msg);
                    }
                }
            }
        }

        if (config.isPowerNotifyStaff()) {
            final String msg = MsgUtil.replace(MsgUtil.message("power.change-staff",
                    "<gray>[Power] <white>{player}</white> {delta} ({before} -> {after}) "
                        + "<dark_gray>{source} {reason} {world} {zone}"),
                "player", playerName,
                "delta", deltaText,
                "before", beforeText,
                "after", afterText,
                "source", source,
                "reason", reason.isBlank() ? "-" : reason,
                "world", world,
                "zone", zone);
            broadcastStaff(msg);
        }
    }

    private void broadcastStaff(final String msg) {
        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("factions.admin")) {
                MsgUtil.send(online, msg);
            }
        }
        final CommandSender console = Bukkit.getConsoleSender();
        MsgUtil.send(console, MsgUtil.stripTags(msg));
    }

    public static ZoneContext zoneFromClaim(final Optional<BoardEntry> claim, final String factionId) {
        if (claim.isEmpty()) {
            return ZoneContext.WILDERNESS;
        }
        final String fid = claim.get().getFactionId();
        if (FactionModel.SAFEZONE_ID.equals(fid)) {
            return ZoneContext.SAFEZONE;
        }
        if (FactionModel.WARZONE_ID.equals(fid)) {
            return ZoneContext.WARZONE;
        }
        if (factionId != null && factionId.equals(fid)) {
            return ZoneContext.OWN_CLAIMED;
        }
        return ZoneContext.ENEMY_CLAIMED;
    }
}

