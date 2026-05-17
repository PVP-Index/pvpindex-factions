package com.pvpindex.factions.scheduler;

/**
 * Handle returned by timer-based scheduling methods to allow cancellation.
 */
public interface CancelableTask {

    /** Cancel the scheduled or repeating task. */
    void cancel();
}
