package com.pvpindex.factions.bootstrap;

import java.util.logging.Logger;

/**
 * Base class for bootstrap components with common logging helpers.
 */
public abstract class AbstractBootstrapComponent implements BootstrapComponent {

    protected Logger logger(final BootstrapContext context) {
        return context.plugin().getLogger();
    }
}

