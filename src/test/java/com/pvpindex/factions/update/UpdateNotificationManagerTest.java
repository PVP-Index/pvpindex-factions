package com.pvpindex.factions.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.ezplugins.updater.ChainedUpdateChecker;
import com.github.ezplugins.updater.UpdateResult;
import com.github.ezplugins.updater.UpdateSource;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class UpdateNotificationManagerTest {

    @Test
    void checkAsyncStoresLatestResult() throws Exception {
        final UpdateSource source = new UpdateSource() {
            @Override
            public String getSourceName() {
                return "test";
            }

            @Override
            public UpdateResult checkNow() {
                return UpdateResult.success(true, "1.0.0", "1.1.0", "v1.1.0", "https://example.com", Instant.now());
            }

            @Override
            public java.util.concurrent.CompletableFuture<UpdateResult> checkNowAsync() {
                return java.util.concurrent.CompletableFuture.completedFuture(checkNow());
            }
        };

        final ChainedUpdateChecker checker = ChainedUpdateChecker.builder().primary(source).build();
        final UpdateNotificationManager manager = new UpdateNotificationManager(checker, Logger.getLogger("test"));
        manager.checkAsync();

        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (manager.latest().isEmpty() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }

        assertTrue(manager.latest().isPresent());
        assertTrue(manager.latest().get().getResult().isUpdateAvailable());
        assertEquals("1.1.0", manager.latest().get().getResult().getLatestVersion().orElseThrow());
    }
}
