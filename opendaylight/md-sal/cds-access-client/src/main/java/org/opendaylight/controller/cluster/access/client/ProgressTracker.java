/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.client;

import com.google.common.base.Preconditions;
import javax.annotation.concurrent.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for tracking throughput and computing delays when processing stream of tasks.
 *
 * <p>The idea is to improve throughput in a typical request-response scenario.
 * A "frontend" is sending requests towards "backend", backend is sending responses back to fronted.
 * Both frontend and backend may be realized by multiple Java threads,
 * so there may be multiple requests not yet responded to.
 * In terms of taks processing, frontend is "opening" tasks and backend is "closing" them.
 * Latency of the backend may fluctuate wildly. To avoid backend running out of open tasks,
 * there should be a queue of requests frontend can add to.
 * In order to avoid excessive memory consumption, there should be a back-pressure mechanism
 * which blocks the frontend threads for appropriate durations.
 * Frontend can tolerate moderately delayed responses, but it only tolerates small block times.
 *
 * <p>An ideal back-pressure algorithm would keep the queue reasonably full,
 * while fairly delaying the frontend threads. In other words, backend idle time should be low,
 * as well as frontend block time dispersion
 * (as opposed to block time average, which is dictated by overall performance).
 *
 * <p>In order for an algorithm to compute reasonable wait times,
 * various inputs can be useful, mostly related to timing of various stages of task processing.
 * Methods of this class assume "enqueue and wait" usage.
 * The delay computation is pessimistic, it expects each participating thread to enqueue another task
 * as soon as its delay time allows.
 *
 * <p>This class is not thread safe, the callers are responsible for guarding against conflicting access.
 * Time is measured in ticks (nanos), methods never look at current time, relying on {@code now} argument instead.
 * This means the sequence of {$code now} argument values is expected to be non-decreasing.
 *
 * <p>Input data used for tracking is tightly coupled with TransitQueue#recordCompletion arguments.
 *
 * @author Vratko Polak
 */
// TODO: Would bulk methods be less taxing than a loop of single task calls?
@NotThreadSafe
abstract class ProgressTracker {
    private static final Logger LOG = LoggerFactory.getLogger(ProgressTracker.class);

    /**
     * When no tasks has been closed yet, this will be used to estimate throughput.
     */
    private final long defaultTicksPerTask;

    /**
     * Number of tasks closed so far.
     */
    private long tasksClosed = 0;

    /**
     * Number of tasks so far, both open and closed.
     */
    private long tasksEncountered = 0;

    /**
     * The most recent tick number when the number of open tasks has become non-positive.
     */
    private long lastIdle = Long.MIN_VALUE;

    /**
     * The most recent tick number when a task has been closed.
     */
    private long lastClosed = Long.MIN_VALUE;

    /**
     * Tick number when the farthest known wait time is over.
     */
    private long nearestAllowed = Long.MIN_VALUE;

    /**
     * Number of ticks elapsed before lastIdle while there was at least one open task.
     */
    private long elapsedBeforeIdle = 0L;

    // Constructors

    /**
     * Construct an idle tracker with specified ticks per task value to use as default.
     *
     * @param ticksPerTask value to use as default
     */
    ProgressTracker(final long ticksPerTask) {
        Preconditions.checkArgument(ticksPerTask >= 0);
        defaultTicksPerTask = ticksPerTask;
    }

    /**
     * Construct a new tracker suitable for a new task queue related to a "reconnect".
     *
     * <p>When reconnecting to a new backend, tasks may need to be re-processed by the frontend,
     * possibly resulting in a different number of tasks.
     * Also, performance of the new backend can be different, but the perforance of the previous backend
     * is generally still better estimate than defaults of a brand new tracker.
     *
     * <p>This "inheritance constructor" creates a new tracker with no open tasks (thus initially idle),
     * but other internal values should lead to a balanced performance
     * after tasks opened in the source tracker are "replayed" into the new tracker.
     *
     * <p>In particular, this impementation keeps the number of closed tasks the same,
     * and makes it so ticksWorkedPerClosedTask is initially the same as in the old tracker.
     *
     * @param oldTracker the tracker used for the previously used backend
     * @param now tick number corresponding to caller's present
     */
    ProgressTracker(final ProgressTracker oldTracker, final long now) {
        this.defaultTicksPerTask = oldTracker.defaultTicksPerTask;
        this.tasksEncountered = this.tasksClosed = oldTracker.tasksClosed;
        this.lastClosed = oldTracker.lastClosed;
        this.nearestAllowed = oldTracker.nearestAllowed;  // Call cancelDebt explicitly if needed.
        this.lastIdle = oldTracker.lastIdle;
        this.elapsedBeforeIdle = oldTracker.elapsedBeforeIdle;
        if (!oldTracker.isIdle()) {
            transitToIdle(now);
        }
    }

    // "Public" shared access (read-only) accessor-like methods

    /**
     * Get number of tasks closed so far.
     *
     * @return number of tasks known to be finished already; the value never decreases
     */
    final long tasksClosed() {
        return tasksClosed;
    }

    /**
     * Get umber of tasks so far, both open and closed.
     *
     * @return number of tasks encountered so far, open or finished; the value never decreases
     */
    final long tasksEncountered() {
        return tasksEncountered;
    }

    /**
     * Get number of tasks currently open.
     *
     * @return number of tasks started but not finished yet
     */
    final long tasksOpen() {  // TODO: Should we return int?
        // TODO: Should we check the return value is non-negative?
        return tasksEncountered - tasksClosed;
    }

    /**
     * When idle, there are no open tasks so no progress is made.
     *
     * @return {@code true} if every encountered task is already closed, {@code false} otherwise
     */
    final boolean isIdle() {
        return tasksClosed >= tasksEncountered;
    }

    /**
     * Number of ticks elapsed (before now) since the last closed task while there was at least one open task.
     *
     * @param now tick number corresponding to caller's present
     * @return number of ticks backend is neither idle nor responding
     */
    final long ticksStalling(final long now) {
        return isIdle() ? 0 : Math.max(now, lastClosed) - lastClosed;
    }

    // Read only protected methods.

    /**
     * Get the value of default ticks per task this instance was created to use.
     *
     * @return default ticks per task value
     */
    protected final long defaultTicksPerTask() {
        return defaultTicksPerTask;
    }

    /**
     * One task is roughly estimated to take this long to close.
     *
     * @param now tick number corresponding to caller's present
     * @return total ticks worked divided by closed tasks, or the default value if no closed tasks
     */
    protected final double ticksWorkedPerClosedTask(final long now) {
        if (tasksClosed < 1) {
            return defaultTicksPerTask;
        }
        return (double) ticksWorked(now) / tasksClosed;
    }

    // Read only private methods.

    /**
     * Number of ticks elapsed (before now) while there was at least one open task.
     *
     * @param now tick number corresponding to caller's present
     * @return number of ticks there was at least one task open
     */
    private long ticksWorked(final long now) {
        return isIdle() ? elapsedBeforeIdle : Math.max(now, lastIdle) - lastIdle + elapsedBeforeIdle;
    }

    /**
     * Give an estimate of a tick number when there will be no accumulated delays.
     *
     * <p>The delays accumulated include one more open task.
     * Basically, the return value corresponds to openTask() return value,
     * but this gives an absolute time, instead of delay relative to now.
     *
     * @param now tick number corresponding to caller's present
     * @return estimated tick number when all threads with opened tasks are done waiting
     */
    private long estimateAllowed(final long now) {
        return Math.max(now, nearestAllowed + estimateIsolatedDelay(now));
    }

    // State-altering "public" methods.

    /**
     * Track a task is being closed.
     *
     * @param now tick number corresponding to caller's present
     * @param enqueuedTicks see TransitQueue#recordCompletion
     * @param transmitTicks see TransitQueue#recordCompletion
     * @param execNanos see TransitQueue#recordCompletion
     */
    final void closeTask(final long now, final long enqueuedTicks, final long transmitTicks, final long execNanos) {
        if (isIdle()) {
            LOG.info("Attempted to close a task while no tasks are open");
        } else {
            unsafeCloseTask(now, enqueuedTicks, transmitTicks, execNanos);
        }
    }

    /**
     * Track a task that is being opened.
     *
     * @param now tick number corresponding to caller's present
     * @return number of ticks (nanos) the caller thread should wait before opening another task
     */
    final long openTask(final long now) {
        openTaskWithoutThrottle(now);
        return reserveDelay(now);
    }

    /**
     * Set nearestAllowed value to now.
     *
     * <p>This is useful when new a backend has just connected,
     * after a period of no working backend present.
     * The accumulated delays should not limit future tasks.
     * The queue fullness and the averaged backend performance are kept,
     * as they should result in good enough estimations for new tasks.
     *
     * @param now tick number corresponding to caller's present
     */
    final void cancelDebt(final long now) {
        nearestAllowed = now;
    }

    // Private state-altering methods.

    /**
     * Compute the next delay and update nearestAllowed value accordingly.
     *
     * @param now tick number corresponding to caller's present
     * @return number of ticks (nanos) the caller thread should wait before opening another task
     */
    private long reserveDelay(final long now) {
        nearestAllowed = estimateAllowed(now);
        return nearestAllowed - now;
    }

    /**
     * Track a task is being closed.
     *
     * <p>This method does not verify there was any task open.
     * This call can empty the collection of open tasks, that special case should be handled.
     *
     * @param now tick number corresponding to caller's present
     * @param enqueuedTicks see TransmitQueue#recordCompletion
     * @param transmitTicks see TransmitQueue#recordCompletion
     * @param execNanos see TransmitQueue#recordCompletion
     */
    private void unsafeCloseTask(final long now, final long enqueuedTicks, final long transmitTicks,
                final long execNanos) {
        tasksClosed++;
        lastClosed = now;
        if (isIdle()) {
            transitToIdle(now);
        }
    }

    /**
     * Track a task is being opened.
     *
     * <p>This method does not aggregate delays, allowing the caller to sidestep the throttling.
     * This call can make the collection of open tasks non-empty, that special case should be handled.
     *
     * @param now tick number corresponding to caller's present
     */
    private void openTaskWithoutThrottle(final long now) {
        if (isIdle()) {
            transitFromIdle(now);
        }
        tasksEncountered++;
    }

    /**
     * Update lastIdle as a new "last" just hapened.
     */
    private void transitFromIdle(final long now) {
        lastIdle = Math.max(now, lastIdle);
    }

    /**
     * Update elapsedBeforeIdle as the "before" has jast moved.
     */
    private void transitToIdle(final long now) {
        elapsedBeforeIdle += Math.max(0, now - lastIdle);
    }

    // Protected abstract read-only methods.

    /**
     * Give an estimate of a fair delay, assuming delays caused by other opened tasks are ignored.
     *
     * @param now tick number corresponding to caller's present
     * @return delay (in ticks) after which another openTask() would be fair to be called by the same thread again
     */
    protected abstract long estimateIsolatedDelay(long now);
}
