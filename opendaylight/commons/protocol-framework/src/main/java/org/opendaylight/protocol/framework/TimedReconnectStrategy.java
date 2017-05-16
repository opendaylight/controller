/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Swiss army knife equivalent for reconnect strategies.
 *
 * This strategy continues to schedule reconnect attempts, each having to complete in a fixed time (connectTime).
 *
 * Initial sleep time is specified as minSleep. Each subsequent unsuccessful attempt multiplies this time by a constant
 * factor (sleepFactor) -- this allows for either constant reconnect times (sleepFactor = 1), or various degrees of
 * exponential back-off (sleepFactor &gt; 1). Maximum sleep time between attempts can be capped to a specific value
 * (maxSleep).
 *
 * The strategy can optionally give up based on two criteria:
 *
 * A preset number of connection retries (maxAttempts) has been reached, or
 *
 * A preset absolute deadline is reached (deadline nanoseconds, as reported by System.nanoTime(). In this specific case,
 * both connectTime and maxSleep will be controlled such that the connection attempt is resolved as closely to the
 * deadline as possible.
 *
 * Both these caps can be combined, with the strategy giving up as soon as the first one is reached.
 */
@Deprecated
@ThreadSafe
public final class TimedReconnectStrategy implements ReconnectStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(TimedReconnectStrategy.class);
    private final EventExecutor executor;
    private final Long deadline, maxAttempts, maxSleep;
    private final double sleepFactor;
    private final int connectTime;
    private final long minSleep;

    @GuardedBy("this")
    private long attempts;

    @GuardedBy("this")
    private long lastSleep;

    @GuardedBy("this")
    private boolean scheduled;

    public TimedReconnectStrategy(final EventExecutor executor, final int connectTime, final long minSleep, final double sleepFactor,
            final Long maxSleep, final Long maxAttempts, final Long deadline) {
        Preconditions.checkArgument(maxSleep == null || minSleep <= maxSleep);
        Preconditions.checkArgument(sleepFactor >= 1);
        Preconditions.checkArgument(connectTime >= 0);
        this.executor = Preconditions.checkNotNull(executor);
        this.deadline = deadline;
        this.maxAttempts = maxAttempts;
        this.minSleep = minSleep;
        this.maxSleep = maxSleep;
        this.sleepFactor = sleepFactor;
        this.connectTime = connectTime;
    }

    @Override
    public synchronized Future<Void> scheduleReconnect(final Throwable cause) {
        LOG.debug("Connection attempt failed", cause);

        // Check if a reconnect attempt is scheduled
        Preconditions.checkState(!this.scheduled);

        // Get a stable 'now' time for deadline calculations
        final long now = System.nanoTime();

        // Obvious stop conditions
        if (this.maxAttempts != null && this.attempts >= this.maxAttempts) {
            return this.executor.newFailedFuture(new Throwable("Maximum reconnection attempts reached"));
        }
        if (this.deadline != null && this.deadline <= now) {
            return this.executor.newFailedFuture(new TimeoutException("Reconnect deadline reached"));
        }

        /*
         * First connection attempt gets initialized to minimum sleep,
         * each subsequent is exponentially backed off by sleepFactor.
         */
        if (this.attempts != 0) {
            this.lastSleep *= this.sleepFactor;
        } else {
            this.lastSleep = this.minSleep;
        }

        // Cap the sleep time to maxSleep
        if (this.maxSleep != null && this.lastSleep > this.maxSleep) {
            LOG.debug("Capped sleep time from {} to {}", this.lastSleep, this.maxSleep);
            this.lastSleep = this.maxSleep;
        }

        this.attempts++;

        // Check if the reconnect attempt is within the deadline
        if (this.deadline != null && this.deadline <= now + TimeUnit.MILLISECONDS.toNanos(this.lastSleep)) {
            return this.executor.newFailedFuture(new TimeoutException("Next reconnect would happen after deadline"));
        }

        LOG.debug("Connection attempt {} sleeping for {} milliseconds", this.attempts, this.lastSleep);

        // If we are not sleeping at all, return an already-succeeded future
        if (this.lastSleep == 0) {
            return this.executor.newSucceededFuture(null);
        }

        // Need to retain a final reference to this for locking purposes,
        // also set the scheduled flag.
        final Object lock = this;
        this.scheduled = true;

        // Schedule a task for the right time. It will also clear the flag.
        return this.executor.schedule(() -> {
            synchronized (lock) {
                Preconditions.checkState(TimedReconnectStrategy.this.scheduled);
                TimedReconnectStrategy.this.scheduled = false;
            }

            return null;
        }, this.lastSleep, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void reconnectSuccessful() {
        Preconditions.checkState(!this.scheduled);
        this.attempts = 0;
    }

    @Override
    public int getConnectTimeout() throws TimeoutException {
        int timeout = this.connectTime;

        if (this.deadline != null) {

            // If there is a deadline, we may need to cap the connect
            // timeout to meet the deadline.
            final long now = System.nanoTime();
            if (now >= this.deadline) {
                throw new TimeoutException("Reconnect deadline already passed");
            }

            final long left = TimeUnit.NANOSECONDS.toMillis(this.deadline - now);
            if (left < 1) {
                throw new TimeoutException("Connect timeout too close to deadline");
            }

            /*
             * A bit of magic:
             * - if time left is less than the timeout, set it directly
             * - if there is no timeout, and time left is:
             *      - less than maximum integer, set timeout to time left
             *      - more than maximum integer, set timeout Integer.MAX_VALUE
             */
            if (timeout > left) {
                timeout = (int) left;
            } else if (timeout == 0) {
                timeout = left <= Integer.MAX_VALUE ? (int) left : Integer.MAX_VALUE;
            }
        }
        return timeout;
    }
}
