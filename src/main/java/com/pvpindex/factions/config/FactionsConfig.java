package com.pvpindex.factions.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed wrapper around the Bukkit {@link FileConfiguration} for {@code config.yml}.
 *
 * <p>Instantiated in {@code PvPIndexFactions#onEnable()} and re-created on
 * {@code /fa reload}.
 */
public class FactionsConfig {

    private final FileConfiguration cfg;

    public FactionsConfig(final FileConfiguration cfg) {
        this.cfg = cfg;
    }

    // -------------------------------------------------------------------------
    // Faction limits
    // -------------------------------------------------------------------------

    public int getMaxMembers() {
        return cfg.getInt("factions.max-members", 50);
    }

    public int getMaxWarps() {
        return cfg.getInt("factions.max-warps", 10);
    }

    public int getMaxAllies() {
        return cfg.getInt("factions.max-allies", 5);
    }

    public int getMaxTruces() {
        return cfg.getInt("factions.max-truces", 5);
    }

    public int getInviteTtlHours() {
        return cfg.getInt("factions.invites.ttl-hours", 72);
    }

    // -------------------------------------------------------------------------
    // Power
    // -------------------------------------------------------------------------

    public double getPowerPerPlayerMax() {
        return cfg.getDouble("factions.power.per-player-max", 10.0);
    }

    /** Alias for {@link #getPowerPerPlayerMax()} — max power per player. */
    public double getMaxPower() {
        return getPowerPerPlayerMax();
    }

    public double getPowerRegenPerSecond() {
        return cfg.getDouble("factions.power.regen-per-second", 0.1);
    }

    /** Power regenerated per tick cycle (every 60 s) for an online player. */
    public double getPowerRegenOnline() {
        return cfg.getDouble("factions.power.regen-online", getPowerRegenPerSecond() * 60);
    }

    /** Power regenerated per tick cycle (every 60 s) for an offline player. */
    public double getPowerRegenOffline() {
        return cfg.getDouble("factions.power.regen-offline", getPowerRegenPerSecond() * 30);
    }

    public double getPowerLossOnDeath() {
        return cfg.getDouble("factions.power.loss-on-death", 4.0);
    }

    public long getPowerGracePeriodSeconds() {
        return cfg.getLong("factions.power.grace-period-seconds", 3600);
    }

    public int getPowerTickIntervalSeconds() {
        return cfg.getInt("factions.power.tick-interval-seconds", 60);
    }

    public boolean isPowerGainOnKillEnabled() {
        return cfg.getBoolean("factions.power.gain-on-kill.enabled", true);
    }

    public double getPowerGainOnKill() {
        return cfg.getDouble("factions.power.gain-on-kill.amount", 2.0);
    }

    // -------------------------------------------------------------------------
    // Kill scaling (F3)
    // -------------------------------------------------------------------------

    public boolean isKillScaleEnabled() {
        return cfg.getBoolean("factions.power.gain-on-kill.scale.enabled", false);
    }

    public double getKillScaleMinFactor() {
        return cfg.getDouble("factions.power.gain-on-kill.scale.min-factor", 0.25);
    }

    public double getKillScaleMaxFactor() {
        return cfg.getDouble("factions.power.gain-on-kill.scale.max-factor", 2.0);
    }

    // -------------------------------------------------------------------------
    // Inactive member exclusion (F1)
    // -------------------------------------------------------------------------

    public boolean isPowerInactiveExclusionEnabled() {
        return cfg.getBoolean("factions.power.inactive-exclusion.enabled", false);
    }

    public int getPowerInactiveDays() {
        return cfg.getInt("factions.power.inactive-exclusion.days", 7);
    }

    // -------------------------------------------------------------------------
    // Death streak multiplier (F2)
    // -------------------------------------------------------------------------

    public boolean isDeathStreakEnabled() {
        return cfg.getBoolean("factions.power.death-streak.enabled", false);
    }

    public int getDeathStreakWindowSeconds() {
        return cfg.getInt("factions.power.death-streak.window-seconds", 600);
    }

    public double getDeathStreakMultiplier() {
        return cfg.getDouble("factions.power.death-streak.multiplier", 1.5);
    }

    public boolean isPowerBuyEnabled() {
        return cfg.getBoolean("factions.power.buy.enabled", false);
    }

    public double getPowerBuyCostPerPoint() {
        return cfg.getDouble("factions.power.buy.cost-per-point", 100.0);
    }

    public double getPowerBuyMaxPerPurchase() {
        return cfg.getDouble("factions.power.buy.max-per-purchase", 5.0);
    }

    // -------------------------------------------------------------------------
    // Land
    // -------------------------------------------------------------------------

    public int getLandBufferZone() {
        return cfg.getInt("factions.land.buffer-zone", 0);
    }

    public int getLandMaxPerCommand() {
        return cfg.getInt("factions.land.max-per-command", 200);
    }

    /** Chunks of land granted per unit of power. */
    public double getLandPerPower() {
        return cfg.getDouble("factions.land.per-power", 1.0);
    }

    /** Hard ceiling on how many chunks a single faction may ever claim. */
    public int getMaxLand() {
        return cfg.getInt("factions.land.max", 500);
    }

    public int getMapOnceRadius() {
        return cfg.getInt("factions.map.once-radius", 3);
    }

    public int getListPageSize() {
        return cfg.getInt("factions.list.page-size", 8);
    }

    public int getTopPageSize() {
        return cfg.getInt("factions.top.page-size", 8);
    }

    public boolean isInfoShowAllies() {
        return cfg.getBoolean("factions.info.relations.show-allies", true);
    }

    public boolean isInfoShowTruces() {
        return cfg.getBoolean("factions.info.relations.show-truces", false);
    }

    public boolean isInfoShowNeutrals() {
        return cfg.getBoolean("factions.info.relations.show-neutrals", false);
    }

    public boolean isInfoShowEnemies() {
        return cfg.getBoolean("factions.info.relations.show-enemies", false);
    }

    public int getBankHistoryPageSize() {
        return cfg.getInt("factions.bank.history.page-size", 8);
    }

    public int getWarpListPageSize() {
        return cfg.getInt("factions.warp.list.page-size", 8);
    }

    // -------------------------------------------------------------------------
    // Economy
    // -------------------------------------------------------------------------

    public boolean isEconomyEnabled() {
        return cfg.getBoolean("factions.economy.enabled", true);
    }

    public double getCostCreate() {
        return cfg.getDouble("factions.economy.cost-create", 50.0);
    }

    public double getCostClaim() {
        return cfg.getDouble("factions.economy.cost-claim", 100.0);
    }

    public boolean isTaxEnabled() {
        return cfg.getBoolean("factions.economy.tax.enabled", false);
    }

    public double getTaxRate() {
        return cfg.getDouble("factions.economy.tax.rate", 0.05);
    }

    public int getTaxIntervalHours() {
        return cfg.getInt("factions.economy.tax.interval-hours", 24);
    }

    public double getTaxMinBankBalance() {
        return cfg.getDouble("factions.economy.tax.min-bank-balance", 0.0);
    }

    public double getTaxMinChargeAmount() {
        return cfg.getDouble("factions.economy.tax.min-charge-amount", 0.01);
    }

    // -------------------------------------------------------------------------
    // Fly
    // -------------------------------------------------------------------------

    public boolean isFlyEnabled() {
        return cfg.getBoolean("factions.fly.enabled", true);
    }

    public boolean isFlyDisableOnThreat() {
        return cfg.getBoolean("factions.fly.disable-on-threat", true);
    }

    public boolean isFlyRequireOwnTerritory() {
        return cfg.getBoolean("factions.fly.require-own-territory", true);
    }

    // -------------------------------------------------------------------------
    // Chat
    // -------------------------------------------------------------------------

    public boolean isChatTagEnabled() {
        return cfg.getBoolean("factions.chat.show-tag", true);
    }

    /** Alias for {@link #isChatTagEnabled()} used by chat engines. */
    public boolean isChatFormatEnabled() {
        return isChatTagEnabled();
    }

    public String getChatTagFormat() {
        return cfg.getString("factions.chat.tag-format", "<gray>[<gold>{faction_name}</gold>]</gray> ");
    }

    // -------------------------------------------------------------------------
    // Integrations
    // -------------------------------------------------------------------------

    public boolean isVaultEnabled() {
        return cfg.getBoolean("integrations.vault", true);
    }

    /** Alias for {@link #isEconomyEnabled()} — whether faction banks are enabled. */
    public boolean isBankEnabled() {
        return isEconomyEnabled();
    }

    public boolean isWorldGuardEnabled() {
        return cfg.getBoolean("integrations.worldguard", true);
    }

    public boolean isDynmapEnabled() {
        return cfg.getBoolean("integrations.dynmap", true);
    }

    public boolean isPlaceholderApiEnabled() {
        return cfg.getBoolean("integrations.placeholderapi", true);
    }

    public boolean isEssentialsXEnabled() {
        return cfg.getBoolean("integrations.essentialsx.enabled", false);
    }

    public boolean isLwcEnabled() {
        return cfg.getBoolean("integrations.lwc.enabled", true);
    }

    public boolean isLwcRequireBuildRightsToCreate() {
        return cfg.getBoolean("integrations.lwc.require-build-rights-to-create", true);
    }

    public boolean isLwcRemoveIfNoBuildRights() {
        return cfg.getBoolean("integrations.lwc.remove-if-no-build-rights", true);
    }

    public boolean isLwcRemoveOnClaimChange() {
        return cfg.getBoolean("integrations.lwc.remove-on-claim-change", true);
    }

    public boolean isDiscordSrvEnabled() {
        return cfg.getBoolean("integrations.discordsrv.enabled", false);
    }

    public String getDiscordSrvChannelId() {
        return cfg.getString("integrations.discordsrv.channel-id", "");
    }

    public boolean isDiscordSrvFactionCreatedEnabled() {
        return cfg.getBoolean("integrations.discordsrv.events.faction-created.enabled", true);
    }

    public String getDiscordSrvFactionCreatedMessage() {
        return cfg.getString("integrations.discordsrv.events.faction-created.message",
            "**{faction}** was founded!");
    }

    public boolean isDiscordSrvFactionDisbandedEnabled() {
        return cfg.getBoolean("integrations.discordsrv.events.faction-disbanded.enabled", true);
    }

    public String getDiscordSrvFactionDisbandedMessage() {
        return cfg.getString("integrations.discordsrv.events.faction-disbanded.message",
            "**{faction}** was disbanded.");
    }

    public boolean isDiscordSrvRelationAllyEnabled() {
        return cfg.getBoolean("integrations.discordsrv.events.relation-ally.enabled", true);
    }

    public String getDiscordSrvRelationAllyMessage() {
        return cfg.getString("integrations.discordsrv.events.relation-ally.message",
            ":handshake: **{source}** and **{target}** are now allies!");
    }

    public boolean isDiscordSrvRelationTruceEnabled() {
        return cfg.getBoolean("integrations.discordsrv.events.relation-truce.enabled", true);
    }

    public String getDiscordSrvRelationTruceMessage() {
        return cfg.getString("integrations.discordsrv.events.relation-truce.message",
            ":white_flag: **{source}** and **{target}** agreed to a truce.");
    }

    public boolean isDiscordSrvRelationEnemyEnabled() {
        return cfg.getBoolean("integrations.discordsrv.events.relation-enemy.enabled", true);
    }

    public String getDiscordSrvRelationEnemyMessage() {
        return cfg.getString("integrations.discordsrv.events.relation-enemy.message",
            ":crossed_swords: **{source}** declared war on **{target}**!");
    }

    // -------------------------------------------------------------------------
    // Metrics
    // -------------------------------------------------------------------------

    public boolean isBstatsEnabled() {
        return cfg.getBoolean("factions.metrics.bstats.enabled", true);
    }

    public int getBstatsPluginId() {
        return cfg.getInt("factions.metrics.bstats.plugin-id", 31240);
    }

    // -------------------------------------------------------------------------
    // Zones
    // -------------------------------------------------------------------------

    public boolean isSafeZoneEnabled() {
        return cfg.getBoolean("factions.zones.safe-zone.enabled", true);
    }

    public boolean isWarZoneEnabled() {
        return cfg.getBoolean("factions.zones.war-zone.enabled", true);
    }

    // -------------------------------------------------------------------------
    // Overclaiming
    // -------------------------------------------------------------------------

    public boolean isOverclaimingEnabled() {
        return cfg.getBoolean("factions.overclaiming.enabled", false);
    }

    public boolean isOverclaimRequireEnemyRelation() {
        return cfg.getBoolean("factions.overclaiming.require-enemy-relation", true);
    }

    /** When {@code true}, factions with zero members online cannot have their land overclaimed. */
    public boolean isOfflineProtectionEnabled() {
        return cfg.getBoolean("factions.overclaiming.offline-protection.enabled", false);
    }

    // -------------------------------------------------------------------------
    // Raidable broadcast (F4)
    // -------------------------------------------------------------------------

    public boolean isRaidableBroadcastEnabled() {
        return cfg.getBoolean("factions.raidable.broadcast.enabled", true);
    }

    public boolean isRaidableBroadcastServerWide() {
        return cfg.getBoolean("factions.raidable.broadcast.server-wide", false);
    }

    // -------------------------------------------------------------------------
    // War shield (F6)
    // -------------------------------------------------------------------------

    public boolean isWarShieldEnabled() {
        return cfg.getBoolean("factions.war.shield.enabled", false);
    }

    public int getWarShieldMaxDurationHours() {
        return cfg.getInt("factions.war.shield.max-duration-hours", 8);
    }

    // -------------------------------------------------------------------------
    // Updates
    // -------------------------------------------------------------------------

    public boolean isUpdateCheckEnabled() {
        return cfg.getBoolean("factions.updates.enabled", true);
    }

    public boolean isUpdateNotifyOpsOnJoin() {
        return cfg.getBoolean("factions.updates.notify-ops-on-join", true);
    }

    public String getUpdateModrinthSlug() {
        return cfg.getString("factions.updates.modrinth-slug", "pvpindex-factions");
    }

    public String getUpdateGithubOwner() {
        return cfg.getString("factions.updates.github-owner", "PVP-Index");
    }

    public String getUpdateGithubRepo() {
        return cfg.getString("factions.updates.github-repo", "pvpindex-factions");
    }
}
