package com.pvpindex.factions.api;

/**
 * Holder for a runtime RoleChangeNotifier. Default is a no-op implementation
 * so core code can call it without requiring TeamsAPI on the classpath.
 */
public final class RoleChangeNotifierHolder {

    private static volatile RoleChangeNotifier notifier = new RoleChangeNotifier() { };

    private RoleChangeNotifierHolder() { }

    public static RoleChangeNotifier getNotifier() {
        return notifier;
    }

    public static void setNotifier(final RoleChangeNotifier n) {
        notifier = n != null ? n : new RoleChangeNotifier() { };
    }

    public static void clearNotifier() {
        notifier = new RoleChangeNotifier() { };
    }
}
