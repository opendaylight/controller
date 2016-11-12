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
import com.google.common.base.Verify;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
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

    private final Queue<ConnectionEntry> pending;
    private final ClientActorContext context;
    private final Long cookie;

    private volatile ReconnectForwarder successor;
    private volatile RequestException poisoned;
    private long lastProgress;

    private AbstractClientConnection(final ClientActorContext context, final Long cookie,
            final Queue<ConnectionEntry> pending) {
        this.context = Preconditions.checkNotNull(context);
        this.cookie = Preconditions.checkNotNull(cookie);
        this.pending = Preconditions.checkNotNull(pending);
        this.lastProgress = readTime();
    }

    // Do not allow subclassing outside of this package
    AbstractClientConnection(final ClientActorContext context, final Long cookie) {
        this(context, cookie, new ArrayDeque<>(1));
    }

    // Do not allow subclassing outside of this package
    AbstractClientConnection(final AbstractClientConnection<T> oldConnection) {
        this(oldConnection.context, oldConnection.cookie, oldConnection.pending);
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

    final long readTime() {
        return context.ticker().read();
    }

    final Queue<ConnectionEntry> pending() {
        return pending;
    }

    /**
     * Send a request to the backend and invoke a specified callback when it finishes. This method is safe to invoke
     * from any thread.
     *
     * @param request Request to send
     * @param callback Callback to invoke
     */
    public final void sendRequest(final Request<?, ?> request, final Consumer<Response<?, ?>> callback) {
        Preconditions.checkState(poisoned == null, "Connection %s has been poisoned", this);

        final ReconnectForwarder beforeQueue = successor;
        final ConnectionEntry entry = new ConnectionEntry(request, callback, readTime());
        if (beforeQueue != null) {
            LOG.trace("Forwarding entry {} from {} to {}", entry, this, beforeQueue);
            beforeQueue.forwardEntry(entry);
            return;
        }

        enqueueEntry(entry);

        final ReconnectForwarder afterQueue = successor;
        if (afterQueue != null) {
            synchronized (this) {
                spliceToSuccessor(afterQueue);
            }
        }
    }

    public final synchronized void setForwarder(final ReconnectForwarder forwarder) {
        Verify.verify(successor == null, "Successor {} already set on connection {}", successor, this);
        successor = Preconditions.checkNotNull(forwarder);
        LOG.debug("Connection {} superseded by {}, splicing queue", this, successor);
        spliceToSuccessor(forwarder);
    }

    public abstract Optional<T> getBackendInfo();

    @GuardedBy("this")
    void spliceToSuccessor(final ReconnectForwarder successor) {
        ConnectionEntry entry = pending.poll();
        while (entry != null) {
            successor.forwardEntry(entry);
            entry = pending.poll();
        }
    }

    final ConnectionEntry dequeEntry() {
        lastProgress = readTime();
        return pending.poll();
    }

    void enqueueEntry(final ConnectionEntry entry) {
        pending.add(entry);
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
        final long now = readTime();

        if (!isEmpty()) {
            final long ticksSinceProgress = now - lastProgress;
            if (ticksSinceProgress >= NO_PROGRESS_TIMEOUT_NANOS) {
                LOG.error("Queue {} has not seen progress in {} seconds, failing all requests", this,
                    TimeUnit.NANOSECONDS.toSeconds(ticksSinceProgress));

                poison(new NoProgressException(ticksSinceProgress));
                current.removeConnection(this);
                return current;
            }
        }

        // Requests are always scheduled in sequence, hence checking for timeout is relatively straightforward.
        // Note we use also inquire about the delay, so we can re-schedule if needed, hence the unusual tri-state
        // return convention.
        final Optional<FiniteDuration> delay = checkTimeout(now);
        if (delay == null) {
            // We have timed out. There is no point in scheduling a timer
            return reconnectConnection(current);
        }

        if (delay.isPresent()) {
            // If there is new delay, schedule a timer
            scheduleTimer(delay.get());
        }

        return current;
    }

    boolean isEmpty() {
        return pending.isEmpty();
    }

    /*
     * We are using tri-state return here to indicate one of three conditions:
     * - if there is no timeout to schedule, return Optional.empty()
     * - if there is a timeout to schedule, return a non-empty optional
     * - if this connections has timed out, return null
     */
    @SuppressFBWarnings(value = "NP_OPTIONAL_RETURN_NULL",
            justification = "Returning null Optional is documented in the API contract.")
    final Optional<FiniteDuration> checkTimeout(final ConnectionEntry head, final long now) {
        if (head == null) {
            return Optional.empty();
        }

        final long delay = head.getEnqueuedTicks() - now + REQUEST_TIMEOUT_NANOS;
        if (delay <= 0) {
            LOG.debug("Connection {} timed out", this);
            return null;
        }

        return Optional.of(FiniteDuration.apply(delay, TimeUnit.NANOSECONDS));
    }

    /*
     * We are using tri-state return here to indicate one of three conditions:
     * - if there is no timeout to schedule, return Optional.empty()
     * - if there is a timeout to schedule, return a non-empty optional
     * - if this connections has timed out, return null
     */
    @SuppressFBWarnings(value = "NP_OPTIONAL_RETURN_NULL",
            justification = "Returning null Optional is documented in the API contract.")
    Optional<FiniteDuration> checkTimeout(final long now) {
        return checkTimeout(pending.peek(), now);
    }

    static void poisonQueue(final Queue<? extends ConnectionEntry> queue, final RequestException cause) {
        for (ConnectionEntry e : queue) {
            final Request<?, ?> request = e.getRequest();
            LOG.trace("Poisoning request {}", request, cause);
            e.complete(request.toRequestFailure(cause));
        }
        queue.clear();
    }

    void poison(final RequestException cause) {
        poisoned = cause;
        poisonQueue(pending, cause);
    }

    @VisibleForTesting
    final RequestException poisoned() {
        return poisoned;
    }

    abstract ClientActorBehavior<T> reconnectConnection(ClientActorBehavior<T> current);

    abstract void receiveResponse(final ResponseEnvelope<?> envelope);
}
