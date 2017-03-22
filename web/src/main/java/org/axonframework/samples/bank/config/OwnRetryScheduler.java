package org.axonframework.samples.bank.config;

import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.gateway.IntervalRetryScheduler;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by m500516 on 22.03.17.
 */
public class OwnRetryScheduler extends IntervalRetryScheduler {

    /**
     * Initializes the retry scheduler to schedule retries on the given {@code executor} using the given
     * {@code interval} and allowing {@code maxRetryCount} retries before giving up permanently.
     *
     * @param executor      The executor on which to schedule retry execution
     * @param interval      The interval in milliseconds at which to schedule a retry
     * @param maxRetryCount The maximum number of retries allowed for a single command
     */
    public OwnRetryScheduler(ScheduledExecutorService executor, int interval, int maxRetryCount) {
        super(executor, interval, maxRetryCount);
    }

    @Override
    public boolean scheduleRetry(CommandMessage commandMessage, RuntimeException lastFailure, List<Class<? extends Throwable>[]> failures, Runnable dispatchTask) {
        return super.scheduleRetry(commandMessage, lastFailure, failures, dispatchTask);
    }

    @Override
    protected boolean isExplicitlyNonTransient(Throwable failure) {
        return false;
    }
}
