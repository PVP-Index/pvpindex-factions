package com.pvpindex.factions.bootstrap;

/**
 * A startup/shutdown phase for plugin bootstrapping.
 */
public interface BootstrapComponent {

    /**
     * Human-readable component name for logs.
     */
    String name();

    /**
     * Executes startup logic for this component.
     *
     * @return true when startup succeeded.
     */
    boolean start(BootstrapContext context);

    /**
     * Executes shutdown logic for this component.
     */
    default void stop(final BootstrapContext context) {
        // no-op by default
    }
}

