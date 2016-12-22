/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.client;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * A ProgressTracker subclass which uses {@code ticksWorkedPerClosedTask} to compute delays.
 *
 * <p>This class has {@code tasksOpenLimit} used as a (weak) limit,
 * as number of open tasks aproaches that value, delays computed are increasing.
 *
 * <p>In order to keep delays from raising to unreasonably high values,
 * a maximal delay (per task) value is never exceeded.
 *
 * <p>On the other hand, there is no delay when numer of open tasks is half the limit or less,
 * in order to prevent backend from running out of tasks while there may be waiting frontend threads.
 *
 * @author Vratko Polak
 */
@NotThreadSafe
final class AveragingProgressTracker extends ProgressTracker {
    /**
     * The implementation will avoid having more that this number of tasks open.
     */
    private final long tasksOpenLimit;

    /**
     * Create an idle tracker with limit and specified ticks per task value to use as default.
     *
     * @param limit of open tasks to avoid exceeding
     * @param ticksPerTask value to use as default
     */
    private AveragingProgressTracker(final long limit, final long ticksPerTask) {
        super(ticksPerTask);
        tasksOpenLimit = limit;
    }

    /**
     * Create a default idle tracker with given limit.
     *
     * @param limit of open tasks to avoid exceeding
     */
    AveragingProgressTracker(final long limit) {
        this(limit, 1_000_000_000);
    }

    /**
     * Create a copy of an existing tracker, all future tracking is fully independent.
     *
     * @param tracker the instance to copy state from
     */
    AveragingProgressTracker(final AveragingProgressTracker tracker) {
        super(tracker);
        this.tasksOpenLimit = tracker.tasksOpenLimit;
    }

    // Public shared access (read-only) accessor-like methods

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
    public long estimateIsolatedDelay(final long now) {
        final long open = tasksOpen();
        if (open <= tasksOpenLimit / 2) {
            return 0L;
        }
        if (open >= tasksOpenLimit) {
            return defaultTicksPerTask();
        }
        final double fullness = ((double) open) / tasksOpenLimit;
        final long delay = (long) (ticksWorkedPerClosedTask(now) * (fullness - 1.0 / 2) / (1.0 - fullness));
        return Math.min(delay, defaultTicksPerTask());
    }
}
