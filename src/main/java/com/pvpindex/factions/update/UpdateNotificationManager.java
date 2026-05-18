package com.pvpindex.factions.update;

import com.github.ezplugins.updater.ChainedUpdateChecker;
import com.github.ezplugins.updater.ChainedUpdateResult;
import com.github.ezplugins.updater.UpdateResult;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Runs update checks and stores the latest result for join-time notifications.
 */
public final class UpdateNotificationManager {

    private final ChainedUpdateChecker checker;
    private final Logger logger;
    private volatile ChainedUpdateResult latest;

    public UpdateNotificationManager(final ChainedUpdateChecker checker, final Logger logger) {
        this.checker = checker;
        this.logger = logger;
    }

    public void checkAsync() {
        checker.checkNowAsync().thenAccept(this::handleResult).exceptionally(error -> {
            logger.warning("Update check failed unexpectedly: " + error.getMessage());
            return null;
        });
    }

    public Optional<ChainedUpdateResult> latest() {
        return Optional.ofNullable(latest);
    }

    private void handleResult(final ChainedUpdateResult result) {
        latest = result;
        final UpdateResult update = result.getResult();
        if (update.hasError()) {
            logger.warning("All update sources failed: " + result.getFailuresBySource().keySet());
            return;
        }
        if (update.isUpdateAvailable()) {
            logger.warning("Update available (" + result.getSourceUsed().orElse("unknown") + "): "
                + update.getCurrentVersion() + " -> " + update.getLatestVersion().orElse("unknown"));
            update.getReleaseUrl().ifPresent(url -> logger.warning("Download: " + url));
            return;
        }
        logger.info("Plugin is up to date (" + result.getSourceUsed().orElse("unknown") + ").");
    }
}
