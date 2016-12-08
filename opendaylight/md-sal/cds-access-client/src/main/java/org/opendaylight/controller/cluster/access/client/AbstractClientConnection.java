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
import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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

    // Keep these constants in nanoseconds, as that prevents unnecessary conversions in the fast path
    @VisibleForTesting
    static final long NO_PROGRESS_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(15);
    @VisibleForTesting
    static final long REQUEST_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(30);

    private final Lock lock = new ReentrantLock();
    private final ClientActorContext context;
    @GuardedBy("lock")
    private final TransmitQueue queue;
    private final Long cookie;

    private volatile RequestException poisoned;

    // Do not allow subclassing outside of this package
    AbstractClientConnection(final ClientActorContext context, final Long cookie,
            final TransmitQueue queue) {
        this.context = Preconditions.checkNotNull(context);
        this.cookie = Preconditions.checkNotNull(cookie);
        this.queue = Preconditions.checkNotNull(queue);
    }

    // Do not allow subclassing outside of this package
    AbstractClientConnection(final AbstractClientConnection<T> oldConnection) {
        this.context = oldConnection.context;
        this.cookie = oldConnection.cookie;
        this.queue = new TransmitQueue.Halted();
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

    /**
     * Send a request to the backend and invoke a specified callback when it finishes. This method is safe to invoke
     * from any thread.
     *
     * <p>This method may put the caller thread to sleep in order to throttle the request rate.
     * The callback may be called before the sleep finishes.
     *
     * @param request Request to send
     * @param callback Callback to invoke
     * @throws InterruptedException when the thread was interrupted during its wait period
     */
    public final void sendRequest(final Request<?, ?> request, final Consumer<Response<?, ?>> callback)
                throws InterruptedException {
        final RequestException maybePoison = poisoned;
        if (maybePoison != null) {
            throw new IllegalStateException("Connection " + this + " has been poisoned", maybePoison);
        }

        final ConnectionEntry entry = new ConnectionEntry(request, callback, readTime());

        enqueueAndWait(entry, entry.getEnqueuedTicks());
    }

    public abstract Optional<T> getBackendInfo();

    final Iterable<ConnectionEntry> startReplay() {
        lock.lock();
        return queue.asIterable();
    }

    @GuardedBy("lock")
    final void finishReplay(final ReconnectForwarder forwarder) {
        // final ConnectionEntry entry = queue.peek();
        // Preconditions.checkState(entry == null, "Entry %s has not been removed", entry);
        queue.setForwarder(forwarder, readTime());
        lock.unlock();
    }

    @GuardedBy("lock")
    final void setForwarder(final ReconnectForwarder forwarder) {
        queue.setForwarder(forwarder, readTime());
    }

    @GuardedBy("lock")
    abstract ClientActorBehavior<T> reconnectConnection(ClientActorBehavior<T> current);

    private long readTime() {
        return context.ticker().read();
    }

    final long enqueueEntry(final ConnectionEntry entry, final long now) {
        lock.lock();
        try {
            return queue.enqueue(entry, now);
        } finally {
            lock.unlock();
        }
    }

    final long enqueueAndWait(final ConnectionEntry entry, final long now) throws InterruptedException {
        final long delay = enqueueEntry(entry, now);
        if (delay > 0L) {
            final int nanos = (int) delay;
            Thread.sleep(nanos / 1000, nanos % 1000);
        }
        return delay;  // the caller is informed on how much time he was supposed to spend sleeping
        // TODO: Expose this information also from sendRequest?
    }

    /**
     * Schedule a timer to fire on the actor thread after a delay.
     *
     * @param delay Delay, in nanoseconds
     */
    private void scheduleTimer(final FiniteDuration delay) {
        LOG.debug("{}: scheduling timeout in {}", context.persistenceId(), delay);
        context.executeInActor(this::runTimer, delay);
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
        final Optional<FiniteDuration> delay;

        lock.lock();
        try {
            final long now = readTime();
            // The following line is only reliable when queue is not forwarding, but such state should not last long.
            final long ticksSinceProgress = queue.ticksStalling(now);
            if (ticksSinceProgress >= NO_PROGRESS_TIMEOUT_NANOS) {
                LOG.error("Queue {} has not seen progress in {} seconds, failing all requests", this,
                    TimeUnit.NANOSECONDS.toSeconds(ticksSinceProgress));

                poison(new NoProgressException(ticksSinceProgress));
                current.removeConnection(this);
                return current;
            }

            // Requests are always scheduled in sequence, hence checking for timeout is relatively straightforward.
            // Note we use also inquire about the delay, so we can re-schedule if needed, hence the unusual tri-state
            // return convention.
            delay = checkTimeout(now);
            if (delay == null) {
                // We have timed out. There is no point in scheduling a timer
                return reconnectConnection(current);
            }
        } finally {
            lock.unlock();
        }

        if (delay.isPresent()) {
            // If there is new delay, schedule a timer
            scheduleTimer(delay.get());
        }

        return current;
    }

    /*
     * We are using tri-state return here to indicate one of three conditions:
     * - if there is no timeout to schedule, return Optional.empty()
     * - if there is a timeout to schedule, return a non-empty optional
     * - if this connections has timed out, return null
     */
    @SuppressFBWarnings(value = "NP_OPTIONAL_RETURN_NULL",
            justification = "Returning null Optional is documented in the API contract.")
    @VisibleForTesting
    final Optional<FiniteDuration> checkTimeout(final long now) {
        final ConnectionEntry head = queue.peek();
        if (head == null) {
            return Optional.empty();
        }

        final long beenOpen = now - head.getEnqueuedTicks();
        if (beenOpen >= REQUEST_TIMEOUT_NANOS ) {
            LOG.debug("Connection {} has a request not completed for {} nanos, timing out", this, beenOpen);
            return null;
        }

        return Optional.of(FiniteDuration.apply(REQUEST_TIMEOUT_NANOS - beenOpen, TimeUnit.NANOSECONDS));
    }

    final void poison(final RequestException cause) {
        poisoned = cause;
        queue.poison(cause);
    }

    @VisibleForTesting
    final RequestException poisoned() {
        return poisoned;
    }

    final void receiveResponse(final ResponseEnvelope<?> envelope) {
        final long now = readTime();

        lock.lock();
        try {
            queue.complete(envelope, now);
        } finally {
            lock.unlock();
        }
    }
}
