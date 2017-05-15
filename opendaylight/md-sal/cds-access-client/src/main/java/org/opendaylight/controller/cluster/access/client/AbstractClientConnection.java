/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.ActorRef;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Base class for a connection to the backend. Responsible to queueing and dispatch of requests toward the backend.
 * Can be in three conceptual states: Connecting, Connected and Reconnecting, which are represented by public final
 * classes exposed from this package.
 *
 * @author Robert Varga
 */
@NotThreadSafe
public abstract class AbstractClientConnection<T extends BackendInfo> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractClientConnection.class);

    /*
     * Timers involved in communication with the backend. There are three tiers which are spaced out to allow for
     * recovery at each tier. Keep these constants in nanoseconds, as that prevents unnecessary conversions in the fast
     * path.
     */
    /**
     * Backend aliveness timer. This is reset whenever we receive a response from the backend and kept armed whenever
     * we have an outstanding request. If when this time expires, we tear down this connection and attept to reconnect
     * it.
     */
    @VisibleForTesting
    static final long BACKEND_ALIVE_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(30);

    /**
     * Request timeout. If the request fails to complete within this time since it was originally enqueued, we time
     * the request out.
     */
    @VisibleForTesting
    static final long REQUEST_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(2);

    /**
     * No progress timeout. A client fails to make any forward progress in this time, it will terminate itself.
     */
    @VisibleForTesting
    static final long NO_PROGRESS_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(15);

    private final Lock lock = new ReentrantLock();
    private final ClientActorContext context;
    @GuardedBy("lock")
    private final TransmitQueue queue;
    private final Long cookie;

    @GuardedBy("lock")
    private boolean haveTimer;

    /**
     * Time reference when we saw any activity from the backend.
     */
    private long lastReceivedTicks;

    private volatile RequestException poisoned;

    // Do not allow subclassing outside of this package
    AbstractClientConnection(final ClientActorContext context, final Long cookie,
            final TransmitQueue queue) {
        this.context = Preconditions.checkNotNull(context);
        this.cookie = Preconditions.checkNotNull(cookie);
        this.queue = Preconditions.checkNotNull(queue);
        this.lastReceivedTicks = currentTime();
    }

    // Do not allow subclassing outside of this package
    AbstractClientConnection(final AbstractClientConnection<T> oldConnection, final int targetQueueSize) {
        this.context = oldConnection.context;
        this.cookie = oldConnection.cookie;
        this.queue = new TransmitQueue.Halted(targetQueueSize);
        this.lastReceivedTicks = oldConnection.lastReceivedTicks;
    }

    public final ClientActorContext context() {
        return context;
    }

    public final @Nonnull Long cookie() {
        return cookie;
    }

    public final ActorRef localActor() {
        return context.self();
    }

    public final long currentTime() {
        return context.ticker().read();
    }

    /**
     * Send a request to the backend and invoke a specified callback when it finishes. This method is safe to invoke
     * from any thread.
     *
     * <p>This method may put the caller thread to sleep in order to throttle the request rate.
     * The callback may be called before the sleep finishes.
     *
     * @param request Request to send
     * @param callback Callback to invoke
     */
    public final void sendRequest(final Request<?, ?> request, final Consumer<Response<?, ?>> callback) {
        final long now = currentTime();
        final long delay = enqueueEntry(new ConnectionEntry(request, callback, now), now);
        try {
            TimeUnit.NANOSECONDS.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.debug("Interrupted after sleeping {}ns", e, currentTime() - now);
        }
    }

    /**
     * Send a request to the backend and invoke a specified callback when it finishes. This method is safe to invoke
     * from any thread.
     *
     * <p>
     * Note that unlike {@link #sendRequest(Request, Consumer)}, this method does not exert backpressure, hence it
     * should never be called from an application thread.
     *
     * @param request Request to send
     * @param callback Callback to invoke
     * @param enqueuedTicks Time (according to {@link #currentTime()} of request enqueue
     */
    public final void enqueueRequest(final Request<?, ?> request, final Consumer<Response<?, ?>> callback,
            final long enqueuedTicks) {
        enqueueEntry(new ConnectionEntry(request, callback, enqueuedTicks), currentTime());
    }

    public abstract Optional<T> getBackendInfo();

    final Iterable<ConnectionEntry> startReplay() {
        lock.lock();
        return queue.asIterable();
    }

    @GuardedBy("lock")
    final void finishReplay(final ReconnectForwarder forwarder) {
        setForwarder(forwarder);
        lock.unlock();
    }

    @GuardedBy("lock")
    final void setForwarder(final ReconnectForwarder forwarder) {
        queue.setForwarder(forwarder, currentTime());
    }

    @GuardedBy("lock")
    abstract ClientActorBehavior<T> lockedReconnect(ClientActorBehavior<T> current,
            RequestException runtimeRequestException);

    final long enqueueEntry(final ConnectionEntry entry, final long now) {
        lock.lock();
        try {
            final RequestException maybePoison = poisoned;
            if (maybePoison != null) {
                throw new IllegalStateException("Connection " + this + " has been poisoned", maybePoison);
            }

            if (queue.isEmpty()) {
                // The queue is becoming non-empty, schedule a timer.
                scheduleTimer(entry.getEnqueuedTicks() + REQUEST_TIMEOUT_NANOS - now);
            }
            return queue.enqueue(entry, now);
        } finally {
            lock.unlock();
        }
    }

    final ClientActorBehavior<T> reconnect(final ClientActorBehavior<T> current, final RequestException cause) {
        lock.lock();
        try {
            return lockedReconnect(current, cause);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Schedule a timer to fire on the actor thread after a delay.
     *
     * @param delay Delay, in nanoseconds
     */
    @GuardedBy("lock")
    private void scheduleTimer(final long delay) {
        if (haveTimer) {
            LOG.debug("{}: timer already scheduled", context.persistenceId());
            return;
        }
        if (queue.hasSuccessor()) {
            LOG.debug("{}: connection has successor, not scheduling timer", context.persistenceId());
            return;
        }

        // If the delay is negative, we need to schedule an action immediately. While the caller could have checked
        // for that condition and take appropriate action, but this is more convenient and less error-prone.
        final long normalized =  delay <= 0 ? 0 : Math.min(delay, BACKEND_ALIVE_TIMEOUT_NANOS);

        final FiniteDuration dur = FiniteDuration.fromNanos(normalized);
        LOG.debug("{}: scheduling timeout in {}", context.persistenceId(), dur);
        context.executeInActor(this::runTimer, dur);
        haveTimer = true;
    }

    /**
     * Check this queue for timeout and initiate reconnection if that happened. If the queue has not made progress
     * in {@link #NO_PROGRESS_TIMEOUT_NANOS} nanoseconds, it will be aborted.
     *
     * @param current Current behavior
     * @return Next behavior to use
     */
    @VisibleForTesting
    final ClientActorBehavior<T> runTimer(final ClientActorBehavior<T> current) {
        final Optional<Long> delay;

        lock.lock();
        try {
            haveTimer = false;
            final long now = currentTime();
            // The following line is only reliable when queue is not forwarding, but such state should not last long.
            // FIXME: BUG-8422: this may not be accurate w.r.t. replayed entries
            final long ticksSinceProgress = queue.ticksStalling(now);
            if (ticksSinceProgress >= NO_PROGRESS_TIMEOUT_NANOS) {
                LOG.error("Queue {} has not seen progress in {} seconds, failing all requests", this,
                    TimeUnit.NANOSECONDS.toSeconds(ticksSinceProgress));

                lockedPoison(new NoProgressException(ticksSinceProgress));
                current.removeConnection(this);
                return current;
            }

            // Requests are always scheduled in sequence, hence checking for timeout is relatively straightforward.
            // Note we use also inquire about the delay, so we can re-schedule if needed, hence the unusual tri-state
            // return convention.
            delay = lockedCheckTimeout(now);
            if (delay == null) {
                // We have timed out. There is no point in scheduling a timer
                return lockedReconnect(current, new RuntimeRequestException("Backend connection timed out",
                    new TimeoutException()));
            }

            if (delay.isPresent()) {
                // If there is new delay, schedule a timer
                scheduleTimer(delay.get());
            }
        } finally {
            lock.unlock();
        }

        return current;
    }

    @VisibleForTesting
    final Optional<Long> checkTimeout(final long now) {
        lock.lock();
        try {
            return lockedCheckTimeout(now);
        } finally {
            lock.unlock();
        }
    }

    /*
     * We are using tri-state return here to indicate one of three conditions:
     * - if there is no timeout to schedule, return Optional.empty()
     * - if there is a timeout to schedule, return a non-empty optional
     * - if this connections has timed out, return null
     */
    @SuppressFBWarnings(value = "NP_OPTIONAL_RETURN_NULL",
            justification = "Returning null Optional is documented in the API contract.")
    @GuardedBy("lock")
    private Optional<Long> lockedCheckTimeout(final long now) {
        if (queue.isEmpty()) {
            return Optional.empty();
        }

        final long backendSilentTicks = now - lastReceivedTicks;
        if (backendSilentTicks >= BACKEND_ALIVE_TIMEOUT_NANOS) {
            LOG.debug("Connection {} has not seen activity from backend for {} nanoseconds, timing out", this,
                backendSilentTicks);
            return null;
        }

        int tasksTimedOut = 0;
        for (ConnectionEntry head = queue.peek(); head != null; head = queue.peek()) {
            final long beenOpen = now - head.getEnqueuedTicks();
            if (beenOpen < REQUEST_TIMEOUT_NANOS) {
                return Optional.of(REQUEST_TIMEOUT_NANOS - beenOpen);
            }

            tasksTimedOut++;
            queue.remove(now);
            LOG.debug("Connection {} timed out entryt {}", this, head);
            head.complete(head.getRequest().toRequestFailure(
                new RequestTimeoutException("Timed out after " + beenOpen + "ns")));
        }

        LOG.debug("Connection {} timed out {} tasks", this, tasksTimedOut);
        if (tasksTimedOut != 0) {
            queue.tryTransmit(now);
        }

        return Optional.empty();
    }

    final void poison(final RequestException cause) {
        lock.lock();
        try {
            lockedPoison(cause);
        } finally {
            lock.unlock();
        }
    }

    @GuardedBy("lock")
    private void lockedPoison(final RequestException cause) {
        poisoned = enrichPoison(cause);
        queue.poison(cause);
    }

    RequestException enrichPoison(final RequestException ex) {
        return ex;
    }

    @VisibleForTesting
    final RequestException poisoned() {
        return poisoned;
    }

    final void receiveResponse(final ResponseEnvelope<?> envelope) {
        final long now = currentTime();
        lastReceivedTicks = now;

        final Optional<TransmittedConnectionEntry> maybeEntry;
        lock.lock();
        try {
            maybeEntry = queue.complete(envelope, now);
        } finally {
            lock.unlock();
        }

        if (maybeEntry.isPresent()) {
            final TransmittedConnectionEntry entry = maybeEntry.get();
            LOG.debug("Completing {} with {}", entry, envelope);
            entry.complete(envelope.getMessage());
        }
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this).omitNullValues()).toString();
    }

    ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("client", context.getIdentifier()).add("cookie", cookie).add("poisoned", poisoned);
    }
}
