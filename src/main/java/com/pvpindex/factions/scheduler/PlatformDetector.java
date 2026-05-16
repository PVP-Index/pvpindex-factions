package com.pvpindex.factions.scheduler;

/**
 * Detects which server platform is running at runtime.
 */
public final class PlatformDetector {

    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
            folia = false;
        }
        FOLIA = folia;
    }

    private PlatformDetector() {
    }

    /**
     * Returns {@code true} when the server is Folia (threaded region scheduler).
     *
     * @return {@code true} on Folia, {@code false} on Paper or Spigot
     */
    public static boolean isFolia() {
        return FOLIA;
    }
}
