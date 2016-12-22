/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.client;

import com.google.common.annotations.Beta;
import javax.annotation.concurrent.NotThreadSafe;

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
 * In order to avoid excessive memore consumption, there should be a back-pressure mechanism
 * which blocks the frontend threads for appropriate durations.
 * Frontend can tolerate moderately delayed responses, but it only tolerates small block times.
 *
 * <p>An ideal back-pressure algorithm would keep the queue reasonably full,
 * while fairly delaying frontend threads. In other words, backend idle time should be low,
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
 * Input data used for tracking is tightly coupled with TransitQueue#recordCompletion arguments.
 *
 * <p>This specific class always reports zero delay, subclasses are expected to override delay computation.
 *
 * @author Vratko Polak
 */
@Beta
@NotThreadSafe
public class ProgressTracker {

    // Fields. Protected instead of private, allowing subclasses to weaken ad-hoc invariants of current implementation.

    /**
     * When no tasks has been closed yet, this will be used to estimate throughput.
     */
    protected double defaultTicksPerTask;  // Any good reason for making this final?

    /**
     * Number of tasks closed so far.
     */
    protected long tasksClosed = 0L;

    /**
     * Number of tasks so far, both open and closed.
     */
    protected long tasksEncountered = 0L;

    /**
     * The most recent tick number when the number of open tasks has become non-positive.
     */
    protected long lastIdle = Long.MIN_VALUE;

    /**
     * The most recent tick number when a task has been closed.
     */
    protected long lastClosed = Long.MIN_VALUE;

    /**
     * Tick number when the furthest known wait time is over.
     */
    protected long nearestAllowed = Long.MIN_VALUE;

    /**
     * Number of ticks elapsed before lastIdle while there was at least one open task.
     */
    protected long elapsedBeforeIdle = 0L;

    // Public shared access (read-only) accessor-like methods

    /**
     * Get the value of default ticks per task this instance was created to use.
     *
     * @return default ticks per task value
     */
    public double defaultTicksPerTask() {
        return defaultTicksPerTask;
    }

    /**
     * Get number of tasks closed so far.
     *
     * @return number of tasks known to be finished already; the value never decreases
     */
    public long tasksClosed() {
        return tasksClosed;
    }

    /**
     * Get umber of tasks so far, both open and closed.
     *
     * @return number of tasks encountered so far, open or finished; the value never decreases
     */
    public long tasksEncountered() {
        return tasksEncountered;
    }

    /**
     * Get number of tasks currently open.
     *
     * @return number of tasks started but not finished yet
     */
    public long tasksOpen() {
        return tasksEncountered - tasksClosed;
        // TODO: Should we check the return value is non-negative?
    }

    /**
     * When idle, there are no open tasks so no progress is made.
     *
     * @return {@code true} if every encountered task is already closed, {@code false} otherwise
     */
    public boolean isIdle() {
        return (tasksOpen() <= 0L);
    }

    /**
     * Number of ticks elapsed (before now) since the last closed task while there was at least one open task.
     *
     * @param now tick number corresponding to caller's present
     * @return number of ticks backend is neither idle nod responding
     */
    public long ticksStalling(final long now) {
        return isIdle() ? 0L : Math.max(now, lastClosed) - lastClosed;
    }

    /**
     * Number of ticks elapsed (before now) while there was at least one open task.
     *
     * @param now tick number corresponding to caller's present
     * @return number of ticks there was at least one task open
     */
    public long ticksWorked(final long now) {
        return isIdle() ? elapsedBeforeIdle : Math.max(now, lastIdle) - lastIdle + elapsedBeforeIdle;
    }

    /**
     * One task is roghly estimated to take this long to close.
     *
     * @param now tick number corresponding to caller's present
     * @return total ticks worked divided by closed tasks, or the default value if no closed tasks
     */
    public double ticksWorkedPerClosedTask(final long now) {
        if (tasksClosed < 1L) {
            return defaultTicksPerTask;
        }
        return ((double) ticksWorked(now)) / tasksClosed;
    }

    /**
     * Give an estimate of a fair delay, assuming delays caused by other opened tasks are ignored.
     *
     * <p>This implementation always returns zero delay.
     *
     * @param now tick number corresponding to caller's present
     * @return delay (in ticks) after which another openTask() would be fair to be called by the same thread again
     */
    public long estimateIsolatedDelay(final long now) {
        return 0L;
    }

    /**
     * Give an estimate of openTask() return value.
     *
     * <p>When the returned delay is positive, the caller thread should wait that time before opening additional task.
     *
     * <p>This method in general takes into account previously assigned delays to avoid overlaps.
     *
     * @param now tick number corresponding to caller's present
     * @return delay (in ticks) after which another openTask() is fair to be called by the same thread again
     */
    public long estimateDelay(final long now) {
        return estimateAllowed(now) - now;
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
    public long estimateAllowed(final long now) {
        return Math.max(now, nearestAllowed) + estimateIsolatedDelay(now);
    }

    // Constructors

    /**
     * Construct a default idle tracker.
     */
    public ProgressTracker() {
        this(1_000_000_000.0);
    }

    /**
     * Construct an idle tracker with specified ticks per task value to use as default.
     *
     * @param ticksPerTask value to use as default
     */
    public ProgressTracker(final double ticksPerTask) {
        defaultTicksPerTask = ticksPerTask;
    }

    /**
     * Construct a copy of an existing tracker, all future tracking is fully independent.
     *
     * @param tracker the instance to copy state from
     */
    public ProgressTracker(final ProgressTracker tracker) {
        this.defaultTicksPerTask = tracker.defaultTicksPerTask;
        this.tasksClosed = tracker.tasksClosed;
        this.tasksEncountered = tracker.tasksEncountered;
        this.lastClosed = tracker.lastClosed;
        this.lastIdle = tracker.lastIdle;
        this.nearestAllowed = tracker.nearestAllowed;
        this.elapsedBeforeIdle = tracker.elapsedBeforeIdle;
    }

    // State-altering public methods.

    /**
     * Track a task is being closed.
     *
     * @param now tick number corresponding to caller's present
     * @param enqueuedTicks see TransitQueue#recordCompletion
     * @param transmitTicks see TransitQueue#recordCompletion
     * @param execNanos see TransitQueue#recordCompletion
     * @return {@code true} if task was closed, {@code false} if there was no open task
     */
    public boolean closeTask(final long now, final long enqueuedTicks, final long transmitTicks, final long execNanos) {
        if (tasksOpen() <= 0L) {
            return false;
        }
        protectedCloseTask(now, enqueuedTicks, transmitTicks, execNanos);
        return true;
    }

    /**
     * Track a task that is being opened.
     *
     * @param now tick number corresponding to caller's present
     * @return number of ticks (nanos) the caller thread should wait before opening another task
     */
    public long openTask(final long now) {
        protectedOpenTask(now);
        return reserveDelay(now);
    }

    // Internal state-altering methods. Protected instead of private,
    // allowing subclasses to weaken ad-hoc invariants of current implementation.

    /**
     * Compute the next delay and update nearestAllowed value accordingly.
     *
     * @param now tick number corresponding to caller's present
     * @return number of ticks (nanos) the caller thread should wait before opening another task
     */
    protected long reserveDelay(final long now) {
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
     * @param enqueuedTicks see TransitQueue#recordCompletion
     * @param transmitTicks see TransitQueue#recordCompletion
     * @param execNanos see TransitQueue#recordCompletion
     */
    protected void protectedCloseTask(final long now, final long enqueuedTicks, final long transmitTicks,
                final long execNanos) {
        tasksClosed++;
        lastClosed = now;
        if (isIdle()) {
            elapsedBeforeIdle += now - lastIdle;
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
    protected void protectedOpenTask(final long now) {
        if (isIdle()) {
            lastIdle = Math.max(now, lastIdle);
        }
        tasksEncountered++;
    }

    // TODO: Would bulk methods be less taxing than a loop of single task calls?

}
