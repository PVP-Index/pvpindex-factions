package com.pvpindex.factions.predefined;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/** Manages pre-defined faction presets backed by pre-defined.yml. */
public final class PredefinedConfigManager {

    private static volatile PredefinedConfigManager instance;

    private final File file;
    private final Logger logger;

    private volatile boolean enabled;
    private volatile boolean caseSensitive;
    private volatile boolean blockDisband;
    private volatile Map<String, PredefinedFactionPreset> presetsByKey = Map.of();

    public PredefinedConfigManager(final File dataFolder, final Logger logger) {
        this.file = new File(dataFolder, "pre-defined.yml");
        this.logger = logger;
    }

    public static void setInstance(final PredefinedConfigManager manager) {
        instance = manager;
    }

    public static PredefinedConfigManager getInstance() {
        return instance;
    }

    public synchronized void initialize() {
        if (!file.exists()) {
            final YamlConfiguration cfg = new YamlConfiguration();
            cfg.set("enabled", false);
            cfg.set("case-sensitive", false);
            cfg.set("block-disband", true);
            cfg.createSection("factions");
            save(cfg);
        }
        reload();
    }

    public synchronized void reload() {
        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        enabled = cfg.getBoolean("enabled", false);
        caseSensitive = cfg.getBoolean("case-sensitive", false);
        blockDisband = cfg.getBoolean("block-disband", true);

        final Map<String, PredefinedFactionPreset> next = new LinkedHashMap<>();
        final ConfigurationSection factionsSec = cfg.getConfigurationSection("factions");
        if (factionsSec != null) {
            for (final String key : factionsSec.getKeys(false)) {
                final ConfigurationSection entry = factionsSec.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                final String name = entry.getString("name", key);
                final boolean created = entry.getBoolean("created", false);
                final HomePreset home = parseHome(entry.getConfigurationSection("home"));
                final List<ClaimPreset> claims = parseClaims(entry.getMapList("claims"));
                next.put(normalize(name), new PredefinedFactionPreset(name, created, home, claims));
            }
        }
        presetsByKey = Collections.unmodifiableMap(next);
    }

    public synchronized void savePreset(final PredefinedFactionPreset preset) {
        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        final String path = "factions." + preset.name();
        cfg.set(path + ".name", preset.name());
        cfg.set(path + ".created", preset.created());
        if (preset.home() != null) {
            cfg.set(path + ".home.world", preset.home().world());
            cfg.set(path + ".home.x", preset.home().x());
            cfg.set(path + ".home.y", preset.home().y());
            cfg.set(path + ".home.z", preset.home().z());
            cfg.set(path + ".home.yaw", preset.home().yaw());
            cfg.set(path + ".home.pitch", preset.home().pitch());
        }
        final List<Map<String, Object>> outClaims = new ArrayList<>();
        for (final ClaimPreset claim : preset.claims()) {
            final Map<String, Object> row = new LinkedHashMap<>();
            row.put("world", claim.world());
            row.put("x", claim.x());
            row.put("z", claim.z());
            outClaims.add(row);
        }
        cfg.set(path + ".claims", outClaims);
        save(cfg);

        final Map<String, PredefinedFactionPreset> mutable = new LinkedHashMap<>(presetsByKey);
        mutable.put(normalize(preset.name()), preset);
        presetsByKey = Collections.unmodifiableMap(mutable);
    }

    public synchronized void setCreated(final String factionName, final boolean created) {
        final Optional<PredefinedFactionPreset> opt = getPreset(factionName);
        if (opt.isEmpty()) {
            return;
        }
        final PredefinedFactionPreset base = opt.get();
        savePreset(new PredefinedFactionPreset(base.name(), created, base.home(), base.claims()));
    }

    public synchronized void setHome(final String factionName, final Location location) {
        final PredefinedFactionPreset base = getPreset(factionName)
            .orElse(new PredefinedFactionPreset(factionName, false, null, List.of()));
        final HomePreset home = new HomePreset(
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch());
        savePreset(new PredefinedFactionPreset(base.name(), base.created(), home, base.claims()));
    }

    public synchronized void addClaim(final String factionName, final String world, final int x, final int z) {
        final PredefinedFactionPreset base = getPreset(factionName)
            .orElse(new PredefinedFactionPreset(factionName, false, null, List.of()));
        final List<ClaimPreset> claims = new ArrayList<>(base.claims());
        final ClaimPreset candidate = new ClaimPreset(world, x, z);
        if (!claims.contains(candidate)) {
            claims.add(candidate);
        }
        savePreset(new PredefinedFactionPreset(base.name(), base.created(), base.home(), List.copyOf(claims)));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isBlockDisband() {
        return blockDisband;
    }

    public Optional<PredefinedFactionPreset> getPreset(final String factionName) {
        return Optional.ofNullable(presetsByKey.get(normalize(factionName)));
    }

    public boolean isPredefinedName(final String factionName) {
        return getPreset(factionName).isPresent();
    }

    public Set<String> presetNames() {
        final LinkedHashSet<String> names = new LinkedHashSet<>();
        for (final PredefinedFactionPreset preset : presetsByKey.values()) {
            names.add(preset.name());
        }
        return names;
    }

    public Collection<PredefinedFactionPreset> allPresets() {
        return presetsByKey.values();
    }

    public Location toLocation(final HomePreset home) {
        if (home == null) {
            return null;
        }
        final World world = Bukkit.getWorld(home.world());
        if (world == null) {
            return null;
        }
        return new Location(world, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
    }

    private HomePreset parseHome(final ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        final String world = section.getString("world", null);
        if (world == null || world.isBlank()) {
            return null;
        }
        return new HomePreset(
            world,
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"),
            (float) section.getDouble("yaw", 0.0),
            (float) section.getDouble("pitch", 0.0));
    }

    private List<ClaimPreset> parseClaims(final List<Map<?, ?>> rows) {
        final List<ClaimPreset> out = new ArrayList<>();
        for (final Map<?, ?> row : rows) {
            final Object worldObj = row.get("world");
            final Object xObj = row.get("x");
            final Object zObj = row.get("z");
            if (!(worldObj instanceof String world) || xObj == null || zObj == null) {
                continue;
            }
            out.add(new ClaimPreset(world, toInt(xObj), toInt(zObj)));
        }
        return List.copyOf(out);
    }

    private int toInt(final Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private String normalize(final String in) {
        if (in == null) {
            return "";
        }
        return caseSensitive ? in : in.toLowerCase(Locale.ROOT);
    }

    private void save(final YamlConfiguration cfg) {
        try {
            cfg.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save pre-defined.yml", e);
        }
    }

    public record PredefinedFactionPreset(String name, boolean created, HomePreset home, List<ClaimPreset> claims) {}

    public record HomePreset(String world, double x, double y, double z, float yaw, float pitch) {}

    public record ClaimPreset(String world, int x, int z) {}
}
