/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for a connection to the backend. Responsible to queueing and dispatch of requests toward the backend.
 * Can be in three conceptual states: Connecting, Connected and Reconnecting, which are represented by public final
 * classes exposed from this package. This class NOT thread-safe, not are its subclasses expected to be thread-safe.
 *
 * @param <T> the type of associated {@link BackendInfo}
 */
public abstract sealed class AbstractClientConnection<T extends BackendInfo>
        permits AbstractReceivingClientConnection, ConnectingClientConnection {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractClientConnection.class);

    /*
     * Timers involved in communication with the backend. There are three tiers which are spaced out to allow for
     * recovery at each tier. Keep these constants in nanoseconds, as that prevents unnecessary conversions in the fast
     * path.
     */
    /**
     * Backend aliveness timer. This is reset whenever we receive a response from the backend and kept armed whenever
     * we have an outstanding request. If when this time expires, we tear down this connection and attempt to reconnect
     * it.
     */
    public static final long DEFAULT_BACKEND_ALIVE_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(30);

    /**
     * Request timeout. If the request fails to complete within this time since it was originally enqueued, we time
     * the request out.
     */
    public static final long DEFAULT_REQUEST_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(2);

    /**
     * No progress timeout. A client fails to make any forward progress in this time, it will terminate itself.
     */
    public static final long DEFAULT_NO_PROGRESS_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(15);

    // Emit a debug entry if we sleep for more that this amount
    private static final long DEBUG_DELAY_NANOS = TimeUnit.MILLISECONDS.toNanos(100);

    // Upper bound on the time a thread is forced to sleep to keep queue size under control
    private static final long MAX_DELAY_SECONDS = 5;
    private static final long MAX_DELAY_NANOS = TimeUnit.SECONDS.toNanos(MAX_DELAY_SECONDS);

    private final Lock lock = new ReentrantLock();
    private final @NonNull ClientActorContext context;
    private final @NonNull Long cookie;
    private final String backendName;
    @GuardedBy("lock")
    private final TransmitQueue queue;

    @GuardedBy("lock")
    private boolean haveTimer;

    /**
     * Time reference when we saw any activity from the backend.
     */
    private long lastReceivedTicks;

    private volatile RequestException poisoned;

    // Private constructor to avoid code duplication.
    private AbstractClientConnection(final AbstractClientConnection<T> oldConn, final TransmitQueue newQueue,
            final String backendName) {
        context = oldConn.context;
        cookie = oldConn.cookie;
        this.backendName = requireNonNull(backendName);
        queue = requireNonNull(newQueue);
        // Will be updated in finishReplay if needed.
        lastReceivedTicks = oldConn.lastReceivedTicks;
    }

    // This constructor is only to be called by ConnectingClientConnection constructor.
    // Do not allow subclassing outside of this package
    AbstractClientConnection(final ClientActorContext context, final Long cookie, final String backendName,
            final int queueDepth) {
        this.context = requireNonNull(context);
        this.cookie = requireNonNull(cookie);
        this.backendName = requireNonNull(backendName);
        queue = new TransmitQueue.Halted(queueDepth);
        lastReceivedTicks = currentTime();
    }

    // This constructor is only to be called (indirectly) by ReconnectingClientConnection constructor.
    // Do not allow subclassing outside of this package
    AbstractClientConnection(final AbstractClientConnection<T> oldConn) {
        this(oldConn, new TransmitQueue.Halted(oldConn.queue, oldConn.currentTime()), oldConn.backendName);
    }

    // This constructor is only to be called (indirectly) by ConnectedClientConnection constructor.
    // Do not allow subclassing outside of this package
    AbstractClientConnection(final AbstractClientConnection<T> oldConn, final T newBackend,
            final int queueDepth) {
        this(oldConn, new TransmitQueue.Transmitting(oldConn.queue, queueDepth, newBackend, oldConn.currentTime(),
            requireNonNull(oldConn.context).messageSlicer()), newBackend.getName());
    }

    /**
     * {@return the {@link ClientActorContext}}
     */
    public final @NonNull ClientActorContext context() {
        return context;
    }

    /**
     * {@return the cookie}
     */
    public final @NonNull Long cookie() {
        return cookie;
    }

    /**
     * {@return the local actor}
     */
    public final @NonNull ActorRef localActor() {
        return context.self();
    }

    /**
     * {@return current logical time, in nanoseconds}
     */
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
        sendEntry(new ConnectionEntry(request, callback, now), now);
    }

    /**
     * Send a request to the backend and invoke a specified callback when it finishes. This method is safe to invoke
     * from any thread.
     *
     * <p>Note that unlike {@link #sendRequest(Request, Consumer)}, this method does not exert backpressure, hence it
     * should never be called from an application thread and serves mostly for moving requests between queues.
     *
     * @param request Request to send
     * @param callback Callback to invoke
     * @param enqueuedTicks Time (according to {@link #currentTime()} of request enqueue
     */
    public final void enqueueRequest(final Request<?, ?> request, final Consumer<Response<?, ?>> callback,
            final long enqueuedTicks) {
        enqueueEntry(new ConnectionEntry(request, callback, enqueuedTicks), currentTime());
    }

    private long enqueueOrForward(final ConnectionEntry entry, final long now) {
        lock.lock();
        try {
            commonEnqueue(entry, now);
            return queue.enqueueOrForward(entry, now);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Enqueue an entry, possibly also transmitting it.
     */
    public final void enqueueEntry(final ConnectionEntry entry, final long now) {
        lock.lock();
        try {
            commonEnqueue(entry, now);
            queue.enqueueOrReplay(entry, now);
        } finally {
            lock.unlock();
        }
    }

    @Holding("lock")
    private void commonEnqueue(final ConnectionEntry entry, final long now) {
        final RequestException maybePoison = poisoned;
        if (maybePoison != null) {
            throw new IllegalStateException("Connection " + this + " has been poisoned", maybePoison);
        }

        if (queue.isEmpty()) {
            // The queue is becoming non-empty, schedule a timer.
            scheduleTimer(entry.getEnqueuedTicks() + context.config().getRequestTimeout() - now);
        }
    }

    // To be called from ClientActorBehavior on ConnectedClientConnection after entries are replayed.
    final void cancelDebt() {
        queue.cancelDebt(currentTime());
    }

    public abstract Optional<T> getBackendInfo();

    final Collection<ConnectionEntry> startReplay() {
        lock.lock();
        return queue.drain();
    }

    @Holding("lock")
    final void finishReplay(final ReconnectForwarder forwarder) {
        setForwarder(forwarder);

        /*
         * The process of replaying all messages may have taken a significant chunk of time, depending on type
         * of messages, queue depth and available processing power. In extreme situations this may have already
         * exceeded BACKEND_ALIVE_TIMEOUT_NANOS, in which case we are running the risk of not making reasonable forward
         * progress before we start a reconnect cycle.
         *
         * Note that the timer is armed after we have sent the first message, hence we should be seeing a response
         * from the backend before we see a timeout, simply due to how the mailbox operates.
         *
         * At any rate, reset the timestamp once we complete reconnection (which an atomic transition from the
         * perspective of outside world), as that makes it a bit easier to reason about timing of events.
         */
        lastReceivedTicks = currentTime();
        lock.unlock();
    }

    @Holding("lock")
    final void setForwarder(final ReconnectForwarder forwarder) {
        queue.setForwarder(forwarder, currentTime());
    }

    @Holding("lock")
    abstract ClientActorBehavior<T> lockedReconnect(ClientActorBehavior<T> current,
            RequestException runtimeRequestException);

    final void sendEntry(final ConnectionEntry entry, final long now) {
        long delay = enqueueOrForward(entry, now);
        try {
            if (delay >= DEBUG_DELAY_NANOS) {
                if (delay > MAX_DELAY_NANOS) {
                    LOG.info("Capping {} throttle delay from {} to {} seconds", this,
                        TimeUnit.NANOSECONDS.toSeconds(delay), MAX_DELAY_SECONDS, new Throwable());
                    delay = MAX_DELAY_NANOS;
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("{}: Sleeping for {}ms on connection {}", context.persistenceId(),
                        TimeUnit.NANOSECONDS.toMillis(delay), this);
                }
            }
            TimeUnit.NANOSECONDS.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.debug("Interrupted after sleeping {}ns", currentTime() - now, e);
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
    @Holding("lock")
    private void scheduleTimer(final long delay) {
        if (haveTimer) {
            LOG.debug("{}: timer already scheduled on {}", context.persistenceId(), this);
            return;
        }
        if (queue.hasSuccessor()) {
            LOG.debug("{}: connection {} has a successor, not scheduling timer", context.persistenceId(), this);
            return;
        }

        // If the delay is negative, we need to schedule an action immediately. While the caller could have checked
        // for that condition and take appropriate action, but this is more convenient and less error-prone.
        final long normalized =  delay <= 0 ? 0 : Math.min(delay, context.config().getBackendAlivenessTimerInterval());

        final var dur = Duration.ofNanos(normalized);
        LOG.debug("{}: connection {} scheduling timeout in {}", context.persistenceId(), this, dur);
        context.executeInActor(this::runTimer, dur);
        haveTimer = true;
    }

    /**
     * Check this queue for timeout and initiate reconnection if that happened. If the queue has not made progress
     * in {@link #DEFAULT_NO_PROGRESS_TIMEOUT_NANOS} nanoseconds, it will be aborted.
     *
     * @param current Current behavior
     * @return Next behavior to use
     */
    @VisibleForTesting
    final ClientActorBehavior<T> runTimer(final ClientActorBehavior<T> current) {
        lock.lock();

        final List<ConnectionEntry> poisonEntries;
        final NoProgressException poisonCause;
        try {
            haveTimer = false;
            final long now = currentTime();

            LOG.debug("{}: running timer on {}", context.persistenceId(), this);

            // The following line is only reliable when queue is not forwarding, but such state should not last long.
            // FIXME: BUG-8422: this may not be accurate w.r.t. replayed entries
            final long ticksSinceProgress = queue.ticksStalling(now);
            if (ticksSinceProgress < context.config().getNoProgressTimeout()) {
                // Requests are always scheduled in sequence, hence checking for timeout is relatively straightforward.
                // Note we use also inquire about the delay, so we can re-schedule if needed, hence the unusual
                // tri-state return convention.
                final OptionalLong delay = lockedCheckTimeout(now);
                if (delay == null) {
                    // We have timed out. There is no point in scheduling a timer
                    LOG.debug("{}: connection {} timed out", context.persistenceId(), this);
                    return lockedReconnect(current, new RuntimeRequestException("Backend connection timed out",
                        new TimeoutException()));
                }

                if (delay.isPresent()) {
                    // If there is new delay, schedule a timer
                    scheduleTimer(delay.orElseThrow());
                } else {
                    LOG.debug("{}: not scheduling timeout on {}", context.persistenceId(), this);
                }

                return current;
            }

            LOG.error("Queue {} has not seen progress in {} seconds, failing all requests", this,
                TimeUnit.NANOSECONDS.toSeconds(ticksSinceProgress));
            poisonCause = new NoProgressException(ticksSinceProgress);
            poisonEntries = lockedPoison(poisonCause);
            current.removeConnection(this);
        } finally {
            lock.unlock();
        }

        poison(poisonEntries, poisonCause);
        return current;
    }

    @VisibleForTesting
    final OptionalLong checkTimeout(final long now) {
        lock.lock();
        try {
            return lockedCheckTimeout(now);
        } finally {
            lock.unlock();
        }
    }

    long backendSilentTicks(final long now) {
        return now - lastReceivedTicks;
    }

    /*
     * We are using tri-state return here to indicate one of three conditions:
     * - if there is no timeout to schedule, return Optional.empty()
     * - if there is a timeout to schedule, return a non-empty optional
     * - if this connections has timed out, return null
     */
    @Holding("lock")
    private OptionalLong lockedCheckTimeout(final long now) {
        if (queue.isEmpty()) {
            LOG.debug("{}: connection {} is empty", context.persistenceId(), this);
            return OptionalLong.empty();
        }

        final long backendSilentTicks = backendSilentTicks(now);
        if (backendSilentTicks >= context.config().getBackendAlivenessTimerInterval()) {
            LOG.debug("{}: Connection {} has not seen activity from backend for {} nanoseconds, timing out",
                context.persistenceId(), this, backendSilentTicks);
            return null;
        }

        int tasksTimedOut = 0;
        for (ConnectionEntry head = queue.peek(); head != null; head = queue.peek()) {
            final long beenOpen = now - head.getEnqueuedTicks();
            final long requestTimeout = context.config().getRequestTimeout();
            if (beenOpen < requestTimeout) {
                return OptionalLong.of(requestTimeout - beenOpen);
            }

            tasksTimedOut++;
            queue.remove(now);
            LOG.debug("{}: Connection {} timed out entry {}", context.persistenceId(), this, head);

            timeoutEntry(head, beenOpen);
        }

        LOG.debug("Connection {} timed out {} tasks", this, tasksTimedOut);
        if (tasksTimedOut != 0) {
            queue.tryTransmit(now);
        }

        return OptionalLong.empty();
    }

    private void timeoutEntry(final ConnectionEntry entry, final long beenOpen) {
        // Timeouts needs to be re-scheduled on actor thread because we are holding the lock on the current queue,
        // which may be the tail of a successor chain. This is a problem if the callback attempts to send a request
        // because that will attempt to lock the chain from the start, potentially causing a deadlock if there is
        // a concurrent attempt to transmit.
        context.executeInActor(current -> {
            final double time = beenOpen * 1.0 / 1_000_000_000;
            entry.complete(entry.getRequest().toRequestFailure(
                new RequestTimeoutException(entry.getRequest() + " timed out after " + time
                        + " seconds. The backend for " + backendName + " is not available.")));
            return current;
        });
    }

    final void poison(final RequestException cause) {
        final List<ConnectionEntry> entries;

        lock.lock();
        try {
            entries = lockedPoison(cause);
        } finally {
            lock.unlock();
        }

        poison(entries, cause);
    }

    // Do not hold any locks while calling this
    private static void poison(final Collection<? extends ConnectionEntry> entries, final RequestException cause) {
        for (ConnectionEntry e : entries) {
            final Request<?, ?> request = e.getRequest();
            LOG.trace("Poisoning request {}", request, cause);
            e.complete(request.toRequestFailure(cause));
        }
    }

    @Holding("lock")
    private List<ConnectionEntry> lockedPoison(final RequestException cause) {
        poisoned = enrichPoison(cause);
        return queue.poison();
    }

    RequestException enrichPoison(final RequestException ex) {
        return ex;
    }

    @VisibleForTesting
    final RequestException poisoned() {
        return poisoned;
    }

    void receiveResponse(final ResponseEnvelope<?> envelope) {
        final long now = currentTime();
        lastReceivedTicks = now;

        final @Nullable TransmittedConnectionEntry entry;
        lock.lock();
        try {
            entry = queue.complete(envelope, now);
        } finally {
            lock.unlock();
        }

        if (entry != null) {
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
