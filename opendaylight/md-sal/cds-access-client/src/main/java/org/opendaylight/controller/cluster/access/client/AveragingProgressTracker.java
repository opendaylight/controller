/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.client;

import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A ProgressTracker subclass which uses {@code ticksWorkedPerClosedTask} to compute delays.
 *
 * <p>This class has {@code tasksOpenLimit} used as a (weak) limit,
 * as number of open tasks approaches that value, delays computed are increasing.
 *
 * <p>In order to keep {@code estimateIsolatedDelay} values from raising unreasonably high,
 * {@code defaultTicksPerTask} acts as a maximal value. {@code openTask} may return
 * higher value if there are tasks above the limit.
 *
 * <p>On the other hand, there is no delay when number of open tasks is half the limit or less,
 * in order to prevent backend from running out of tasks while there may be waiting frontend threads.
 *
 * @author Vratko Polak
 */
@NotThreadSafe
final class AveragingProgressTracker extends ProgressTracker {
    private static final long DEFAULT_TICKS_PER_TASK = TimeUnit.MILLISECONDS.toNanos(500);

    /**
     * The implementation will avoid having more that this number of tasks open.
     */
    private final long tasksOpenLimit;

    /**
     * We do not delay tasks until their count hits this threshold.
     */
    private final long noDelayThreshold;

    /**
     * Create an idle tracker with limit and specified ticks per task value to use as default.
     *
     * @param limit of open tasks to avoid exceeding
     * @param ticksPerTask value to use as default
     */
    private AveragingProgressTracker(final long limit, final long ticksPerTask) {
        super(ticksPerTask);
        tasksOpenLimit = limit;
        noDelayThreshold = limit / 2;
    }

    /**
     * Create a default idle tracker with given limit.
     *
     * @param limit of open tasks to avoid exceeding
     */
    AveragingProgressTracker(final long limit) {
        this(limit, DEFAULT_TICKS_PER_TASK);
    }

    /**
     * Construct a new tracker suitable for a new task queue related to a "reconnect".
     *
     * <p>The limit is set independently of the old tracker.
     *
     * @param oldTracker the tracker used for the previously used backend
     * @param limit of open tasks to avoid exceeding
     * @param now tick number corresponding to caller's present
     */
    AveragingProgressTracker(final ProgressTracker oldTracker, final long limit, final long now) {
        super(oldTracker, now);
        tasksOpenLimit = limit;
        noDelayThreshold = limit / 2;
    }

    /**
     * Construct a new tracker suitable for a new task queue related to a "reconnect".
     *
     * <p>The limit is copied from the old tracker.
     *
     * @param oldTracker the tracker used for the previously used backend
     * @param now tick number corresponding to caller's present
     */
    AveragingProgressTracker(final AveragingProgressTracker oldTracker, final long now) {
        this(oldTracker, oldTracker.tasksOpenLimit, now);
    }

    // Protected read-only methods

    /**
     * Give an estimate of a fair delay, assuming delays caused by other opened tasks are ignored.
     *
     * <p>This implementation returns zero delay if number of open tasks is half of limit or less.
     * Else the delay is computed, aiming to keep number of open tasks at 3/4 of limit,
     * assuming backend throughput remains constant.
     *
     * <p>As the number of open tasks approaches the limit,
     * the computed delay increases, but it never exceeds defaultTicksPerTask.
     * That means the actual number of open tasks can exceed the limit.
     *
     * @param now tick number corresponding to caller's present
     * @return delay (in ticks) after which another openTask() would be fair to be called by the same thread again
     */
    @Override
    protected long estimateIsolatedDelay(final long now) {
        final long open = tasksOpen();
        if (open <= noDelayThreshold) {
            return 0L;
        }
        if (open >= tasksOpenLimit) {
            return defaultTicksPerTask();
        }

        /*
         * Calculate the task capacity relative to the limit on open tasks. In real terms this value can be
         * in the open interval (0.0, 0.5).
         */
        final double relativeRemainingCapacity = 1.0 - (double) open / tasksOpenLimit;

        /*
         * Calculate delay coefficient. It increases in inverse proportion to relative remaining capacity, approaching
         * infinity as remaining capacity approaches 0.0.
         */
        final double delayCoefficient = (0.5 - relativeRemainingCapacity) / relativeRemainingCapacity;
        final long delay = (long) (ticksWorkedPerClosedTask(now) * delayCoefficient);

        /*
         * Cap the result to defaultTicksPerTask, since the calculated delay may overstep it.
         */
        return Math.min(delay, defaultTicksPerTask());
    }
}
