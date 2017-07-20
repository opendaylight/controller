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
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;
import java.util.Queue;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This queue is internally split into two queues for performance reasons, both memory efficiency and copy
 * operations.
 *
 * <p>
 * Entries are always appended to the end, but then they are transmitted to the remote end and do not necessarily
 * complete in the order in which they were sent -- hence the head of the queue does not increase linearly,
 * but can involve spurious removals of non-head entries.
 *
 * <p>
 * For memory efficiency we want to pre-allocate both queues -- which points to ArrayDeque, but that is very
 * inefficient when entries are removed from the middle. In the typical case we expect the number of in-flight
 * entries to be an order of magnitude lower than the number of enqueued entries, hence the split.
 *
 * <p>
 * Note that in transient case of reconnect, when the backend gives us a lower number of maximum in-flight entries
 * than the previous incarnation, we may end up still moving the pending queue -- but that is a very exceptional
 * scenario, hence we consciously ignore it to keep the design relatively simple.
 *
 * <p>
 * This class is not thread-safe, as it is expected to be guarded by {@link AbstractClientConnection}.
 *
 * @author Robert Varga
 */
@NotThreadSafe
abstract class TransmitQueue {
    static final class Halted extends TransmitQueue {
        // For ConnectingClientConnection.
        Halted(final int targetDepth) {
            super(targetDepth);
        }

        // For ReconnectingClientConnection.
        Halted(final TransmitQueue oldQueue, final long now) {
            super(oldQueue, now);
        }

        @Override
        int canTransmitCount(final int inflightSize) {
            return 0;
        }

        @Override
        TransmittedConnectionEntry transmit(final ConnectionEntry entry, final long now) {
            throw new UnsupportedOperationException("Attempted to transmit on a halted queue");
        }
    }

    static final class Transmitting extends TransmitQueue {
        private final BackendInfo backend;
        private long nextTxSequence;

        // For ConnectedClientConnection.
        Transmitting(final TransmitQueue oldQueue, final int targetDepth, final BackendInfo backend, final long now) {
            super(oldQueue, targetDepth, now);
            this.backend = Preconditions.checkNotNull(backend);
        }

        @Override
        int canTransmitCount(final int inflightSize) {
            return backend.getMaxMessages() - inflightSize;
        }

        @Override
        TransmittedConnectionEntry transmit(final ConnectionEntry entry, final long now) {
            final RequestEnvelope env = new RequestEnvelope(entry.getRequest().toVersion(backend.getVersion()),
                backend.getSessionId(), nextTxSequence++);

            final TransmittedConnectionEntry ret = new TransmittedConnectionEntry(entry, env.getSessionId(),
                env.getTxSequence(), now);
            backend.getActor().tell(env, ActorRef.noSender());
            return ret;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(TransmitQueue.class);

    private final Deque<TransmittedConnectionEntry> inflight = new ArrayDeque<>();
    private final Deque<ConnectionEntry> pending = new ArrayDeque<>();
    private final AveragingProgressTracker tracker;  // Cannot be just ProgressTracker as we are inheriting limits.
    private ReconnectForwarder successor;

    /**
     * Construct initial transmitting queue.
     */
    TransmitQueue(final int targetDepth) {
        tracker = new AveragingProgressTracker(targetDepth);
    }

    /**
     * Construct new transmitting queue while inheriting timing data from the previous transmit queue instance.
     */
    TransmitQueue(final TransmitQueue oldQueue, final int targetDepth, final long now) {
        tracker = new AveragingProgressTracker(oldQueue.tracker, targetDepth, now);
    }

    /**
     * Construct new transmitting queue while inheriting timing and size data from the previous transmit queue instance.
     */
    TransmitQueue(final TransmitQueue oldQueue, final long now) {
        tracker = new AveragingProgressTracker(oldQueue.tracker, now);
    }

    /**
     * Cancel the accumulated sum of delays as we expect the new backend to work now.
     */
    void cancelDebt(final long now) {
        tracker.cancelDebt(now);
    }

    /**
     * Drain the contents of the connection into a list. This will leave the queue empty and allow further entries
     * to be added to it during replay. When we set the successor all entries enqueued between when this methods
     * returns and the successor is set will be replayed to the successor.
     *
     * @return Collection of entries present in the queue.
     */
    final Collection<ConnectionEntry> drain() {
        final Collection<ConnectionEntry> ret = new ArrayDeque<>(inflight.size() + pending.size());
        ret.addAll(inflight);
        ret.addAll(pending);
        inflight.clear();
        pending.clear();
        return ret;
    }

    final long ticksStalling(final long now) {
        return tracker.ticksStalling(now);
    }

    final boolean hasSuccessor() {
        return successor != null;
    }

    // If a matching request was found, this will track a task was closed.
    final Optional<TransmittedConnectionEntry> complete(final ResponseEnvelope<?> envelope, final long now) {
        Optional<TransmittedConnectionEntry> maybeEntry = findMatchingEntry(inflight, envelope);
        if (maybeEntry == null) {
            LOG.debug("Request for {} not found in inflight queue, checking pending queue", envelope);
            maybeEntry = findMatchingEntry(pending, envelope);
        }

        if (maybeEntry == null || !maybeEntry.isPresent()) {
            LOG.warn("No request matching {} found, ignoring response", envelope);
            return Optional.empty();
        }

        final TransmittedConnectionEntry entry = maybeEntry.get();
        tracker.closeTask(now, entry.getEnqueuedTicks(), entry.getTxTicks(), envelope.getExecutionTimeNanos());

        // We have freed up a slot, try to transmit something
        tryTransmit(now);

        return Optional.of(entry);
    }

    final void tryTransmit(final long now) {
        final int toSend = canTransmitCount(inflight.size());
        if (toSend > 0 && !pending.isEmpty()) {
            transmitEntries(toSend, now);
        }
    }

    private void transmitEntries(final int maxTransmit, final long now) {
        for (int i = 0; i < maxTransmit; ++i) {
            final ConnectionEntry e = pending.poll();
            if (e == null) {
                LOG.debug("Queue {} transmitted {} requests", this, i);
                return;
            }

            transmitEntry(e, now);
        }

        LOG.debug("Queue {} transmitted {} requests", this, maxTransmit);
    }

    private void transmitEntry(final ConnectionEntry entry, final long now) {
        LOG.debug("Queue {} transmitting entry {}", this, entry);
        // We are not thread-safe and are supposed to be externally-guarded,
        // hence send-before-record should be fine.
        // This needs to be revisited if the external guards are lowered.
        inflight.addLast(transmit(entry, now));
    }

    final long enqueueOrForward(final ConnectionEntry entry, final long now) {
        if (successor != null) {
            // This call will pay the enqueuing price, hence the caller does not have to
            successor.forwardEntry(entry, now);
            return 0;
        }

        return enqueue(entry, now);
    }

    final void enqueueOrReplay(final ConnectionEntry entry, final long now) {
        if (successor != null) {
            successor.replayEntry(entry, now);
        } else {
            enqueue(entry, now);
        }
    }

    /**
     * Enqueue an entry, possibly also transmitting it.
     *
     * @return Delay to be forced on the calling thread, in nanoseconds.
     */
    private long enqueue(final ConnectionEntry entry, final long now) {

        // XXX: we should place a guard against incorrect entry sequences:
        // entry.getEnqueueTicks() should have non-negative difference from the last entry present in the queues

        // Reserve an entry before we do anything that can fail
        final long delay = tracker.openTask(now);

        /*
         * This is defensive to make sure we do not do the wrong thing here and reorder messages if we ever happen
         * to have available send slots and non-empty pending queue.
         */
        final int toSend = canTransmitCount(inflight.size());
        if (toSend <= 0) {
            LOG.trace("Queue is at capacity, delayed sending of request {}", entry.getRequest());
            pending.addLast(entry);
            return delay;
        }

        if (pending.isEmpty()) {
            transmitEntry(entry, now);
            return delay;
        }

        pending.addLast(entry);
        transmitEntries(toSend, now);
        return delay;
    }

    /**
     * Return the number of entries which can be transmitted assuming the supplied in-flight queue size.
     */
    abstract int canTransmitCount(int inflightSize);

    abstract TransmittedConnectionEntry transmit(ConnectionEntry entry, long now);

    final boolean isEmpty() {
        return inflight.isEmpty() && pending.isEmpty();
    }

    final ConnectionEntry peek() {
        final ConnectionEntry ret = inflight.peek();
        if (ret != null) {
            return ret;
        }

        return pending.peek();
    }

    final void poison(final RequestException cause) {
        poisonQueue(inflight, cause);
        poisonQueue(pending, cause);
    }

    final void setForwarder(final ReconnectForwarder forwarder, final long now) {
        Verify.verify(successor == null, "Successor %s already set on connection %s", successor, this);
        successor = Preconditions.checkNotNull(forwarder);
        LOG.debug("Connection {} superseded by {}, splicing queue", this, successor);

        /*
         * We need to account for entries which have been added between the time drain() was called and this method
         * is invoked. Since the old connection is visible during replay and some entries may have completed on the
         * replay thread, there was an avenue for this to happen.
         */
        int count = 0;
        ConnectionEntry entry = inflight.poll();
        while (entry != null) {
            successor.replayEntry(entry, now);
            entry = inflight.poll();
            count++;
        }

        entry = pending.poll();
        while (entry != null) {
            successor.replayEntry(entry, now);
            entry = pending.poll();
            count++;
        }

        LOG.debug("Connection {} queue spliced {} messages", this, count);
    }

    final void remove(final long now) {
        final TransmittedConnectionEntry txe = inflight.poll();
        if (txe == null) {
            final ConnectionEntry entry = pending.pop();
            tracker.closeTask(now, entry.getEnqueuedTicks(), 0, 0);
        } else {
            tracker.closeTask(now, txe.getEnqueuedTicks(), txe.getTxTicks(), 0);
        }
    }

    @VisibleForTesting
    Deque<TransmittedConnectionEntry> getInflight() {
        return inflight;
    }

    @VisibleForTesting
    Deque<ConnectionEntry> getPending() {
        return pending;
    }

    /*
     * We are using tri-state return here to indicate one of three conditions:
     * - if a matching entry is found, return an Optional containing it
     * - if a matching entry is not found, but it makes sense to keep looking at other queues, return null
     * - if a conflicting entry is encountered, indicating we should ignore this request, return an empty Optional
     */
    @SuppressFBWarnings(value = "NP_OPTIONAL_RETURN_NULL",
            justification = "Returning null Optional is documented in the API contract.")
    private static Optional<TransmittedConnectionEntry> findMatchingEntry(final Queue<? extends ConnectionEntry> queue,
            final ResponseEnvelope<?> envelope) {
        // Try to find the request in a queue. Responses may legally come back in a different order, hence we need
        // to use an iterator
        final Iterator<? extends ConnectionEntry> it = queue.iterator();
        while (it.hasNext()) {
            final ConnectionEntry e = it.next();
            final Request<?, ?> request = e.getRequest();
            final Response<?, ?> response = envelope.getMessage();

            // First check for matching target, or move to next entry
            if (!request.getTarget().equals(response.getTarget())) {
                continue;
            }

            // Sanity-check logical sequence, ignore any out-of-order messages
            if (request.getSequence() != response.getSequence()) {
                LOG.debug("Expecting sequence {}, ignoring response {}", request.getSequence(), envelope);
                return Optional.empty();
            }

            // Check if the entry has (ever) been transmitted
            if (!(e instanceof TransmittedConnectionEntry)) {
                return Optional.empty();
            }

            final TransmittedConnectionEntry te = (TransmittedConnectionEntry) e;

            // Now check session match
            if (envelope.getSessionId() != te.getSessionId()) {
                LOG.debug("Expecting session {}, ignoring response {}", te.getSessionId(), envelope);
                return Optional.empty();
            }
            if (envelope.getTxSequence() != te.getTxSequence()) {
                LOG.warn("Expecting txSequence {}, ignoring response {}", te.getTxSequence(), envelope);
                return Optional.empty();
            }

            LOG.debug("Completing request {} with {}", request, envelope);
            it.remove();
            return Optional.of(te);
        }

        return null;
    }

    private static void poisonQueue(final Queue<? extends ConnectionEntry> queue, final RequestException cause) {
        for (ConnectionEntry e : queue) {
            final Request<?, ?> request = e.getRequest();
            LOG.trace("Poisoning request {}", request, cause);
            e.complete(request.toRequestFailure(cause));
        }
        queue.clear();
    }
}
