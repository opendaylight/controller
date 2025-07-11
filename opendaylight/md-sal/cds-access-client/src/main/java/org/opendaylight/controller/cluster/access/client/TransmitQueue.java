/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.SliceableMessage;
import org.opendaylight.controller.cluster.messaging.MessageSlicer;
import org.opendaylight.controller.cluster.messaging.SliceOptions;
import org.opendaylight.raft.spi.AveragingProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This queue is internally split into two queues for performance reasons, both memory efficiency and copy
 * operations.
 *
 * <p>Entries are always appended to the end, but then they are transmitted to the remote end and do not necessarily
 * complete in the order in which they were sent -- hence the head of the queue does not increase linearly, but can
 * involve spurious removals of non-head entries.
 *
 * <p>For memory efficiency we want to pre-allocate both queues -- which points to ArrayDeque, but that is very
 * inefficient when entries are removed from the middle. In the typical case we expect the number of in-flight entries
 * to be an order of magnitude lower than the number of enqueued entries, hence the split.
 *
 * <p>Note that in transient case of reconnect, when the backend gives us a lower number of maximum in-flight entries
 * than the previous incarnation, we may end up still moving the pending queue -- but that is a very exceptional
 * scenario, hence we consciously ignore it to keep the design relatively simple.
 *
 * <p>This class is not thread-safe, as it is expected to be guarded by {@link AbstractClientConnection}.
 */
abstract sealed class TransmitQueue {
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

        @Override
        void preComplete(final ResponseEnvelope<?> envelope) {
        }
    }

    static final class Transmitting extends TransmitQueue {
        private static final long NOT_SLICING = -1;

        private final BackendInfo backend;
        private final MessageSlicer messageSlicer;
        private long nextTxSequence;
        private long currentSlicedEnvSequenceId = NOT_SLICING;

        // For ConnectedClientConnection.
        Transmitting(final TransmitQueue oldQueue, final int targetDepth, final BackendInfo backend, final long now,
                final MessageSlicer messageSlicer) {
            super(oldQueue, targetDepth, now);
            this.backend = requireNonNull(backend);
            this.messageSlicer = requireNonNull(messageSlicer);
        }

        @Override
        int canTransmitCount(final int inflightSize) {
            return backend.getMaxMessages() - inflightSize;
        }

        @Override
        TransmittedConnectionEntry transmit(final ConnectionEntry entry, final long now) {
            // If we're currently slicing a message we can't send any subsequent requests until slicing completes to
            // avoid an out-of-sequence request envelope failure on the backend. In this case we return null to indicate
            // the request was not transmitted.
            if (currentSlicedEnvSequenceId >= 0) {
                return null;
            }

            final var request = entry.getRequest();
            final var env = new RequestEnvelope(request.toVersion(backend.getVersion()), backend.getSessionId(),
                nextTxSequence++);

            if (request instanceof SliceableMessage) {
                if (messageSlicer.slice(SliceOptions.builder().identifier(request.getTarget())
                        .message(env).replyTo(request.getReplyTo()).sendTo(backend.getActor())
                        .onFailureCallback(t -> env.sendFailure(new RuntimeRequestException(
                                "Failed to slice request " + request, t), 0L)).build())) {
                    // The request was sliced so record the envelope sequence id to prevent transmitting
                    // subsequent requests until slicing completes.
                    currentSlicedEnvSequenceId = env.getTxSequence();
                }
            } else {
                backend.getActor().tell(env, ActorRef.noSender());
            }

            return new TransmittedConnectionEntry(entry, env.getSessionId(), env.getTxSequence(), now);
        }

        @Override
        void preComplete(final ResponseEnvelope<?> envelope) {
            if (envelope.getTxSequence() == currentSlicedEnvSequenceId) {
                // Slicing completed for the prior request - clear the cached sequence id field to enable subsequent
                // requests to be transmitted.
                currentSlicedEnvSequenceId = NOT_SLICING;
            }
        }
    }

    @NonNullByDefault
    private sealed interface MatchingEntry {
        // Nothing else
    }

    private record FoundEntry(TransmittedConnectionEntry entry) implements MatchingEntry {
        public FoundEntry {
            requireNonNull(entry);
        }
    }

    private static final class NotPresentEntry implements MatchingEntry {
        static final NotPresentEntry INSTANCE = new NotPresentEntry();
    }

    private static final Logger LOG = LoggerFactory.getLogger(TransmitQueue.class);

    private final ArrayDeque<TransmittedConnectionEntry> inflight = new ArrayDeque<>();
    private final ArrayDeque<ConnectionEntry> pending = new ArrayDeque<>();
    // Cannot be just ProgressTracker as we are inheriting limits.
    private final AveragingProgressTracker tracker;
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
        final var ret = new ArrayDeque<ConnectionEntry>(inflight.size() + pending.size());
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
    final @Nullable TransmittedConnectionEntry complete(final ResponseEnvelope<?> envelope, final long now) {
        preComplete(envelope);

        var matchingEntry = findMatchingEntry(inflight, envelope);
        if (matchingEntry == null) {
            LOG.debug("Request for {} not found in inflight queue, checking pending queue", envelope);
            matchingEntry = findMatchingEntry(pending, envelope);
        }

        if (matchingEntry instanceof FoundEntry(var entry)) {
            tracker.closeTask(now, entry.getEnqueuedTicks(), entry.getTxTicks(), envelope.getExecutionTimeNanos());

            // We have freed up a slot, try to transmit something
            tryTransmit(now);

            return entry;
        }

        LOG.warn("No request matching {} found, ignoring response", envelope);
        return null;
    }

    final void tryTransmit(final long now) {
        final int toSend = canTransmitCount(inflight.size());
        if (toSend > 0 && !pending.isEmpty()) {
            transmitEntries(toSend, now);
        }
    }

    private void transmitEntries(final int maxTransmit, final long now) {
        for (int i = 0; i < maxTransmit; ++i) {
            final var entry = pending.poll();
            if (entry == null || !transmitEntry(entry, now)) {
                LOG.debug("Queue {} transmitted {} requests", this, i);
                return;
            }
        }

        LOG.debug("Queue {} transmitted {} requests", this, maxTransmit);
    }

    private boolean transmitEntry(final ConnectionEntry entry, final long now) {
        LOG.debug("Queue {} transmitting entry {}", this, entry);
        // We are not thread-safe and are supposed to be externally-guarded,
        // hence send-before-record should be fine.
        // This needs to be revisited if the external guards are lowered.
        final var maybeTransmitted = transmit(entry, now);
        if (maybeTransmitted == null) {
            return false;
        }

        inflight.addLast(maybeTransmitted);
        return true;
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
            if (!transmitEntry(entry, now)) {
                LOG.debug("Queue {} cannot transmit request {} - delaying it", this, entry.getRequest());
                pending.addLast(entry);
            }

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

    abstract @Nullable TransmittedConnectionEntry transmit(ConnectionEntry entry, long now);

    abstract void preComplete(ResponseEnvelope<?> envelope);

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

    final List<ConnectionEntry> poison() {
        final List<ConnectionEntry> entries = new ArrayList<>(inflight.size() + pending.size());
        entries.addAll(inflight);
        inflight.clear();
        entries.addAll(pending);
        pending.clear();
        return entries;
    }

    final void setForwarder(final ReconnectForwarder forwarder, final long now) {
        verify(successor == null, "Successor %s already set on connection %s", successor, this);
        successor = requireNonNull(forwarder);
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
        final var txe = inflight.poll();
        if (txe == null) {
            final var entry = pending.pop();
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
     * - if a matching entry is found, return an MatchingEntry.Found containing it
     * - if a matching entry is not found, but it makes sense to keep looking at other queues, return null
     * - if a conflicting entry is encountered, indicating we should ignore this request,
     *   return MatchingEntry.NotPresent
     */
    private static @Nullable MatchingEntry findMatchingEntry(final Queue<? extends ConnectionEntry> queue,
            final ResponseEnvelope<?> envelope) {
        // Try to find the request in a queue. Responses may legally come back in a different order, hence we need
        // to use an iterator
        final var it = queue.iterator();
        while (it.hasNext()) {
            final var ce = it.next();
            final var request = ce.getRequest();
            final var response = envelope.getMessage();

            // First check for matching target, or move to next entry
            if (!request.getTarget().equals(response.getTarget())) {
                continue;
            }

            // Sanity-check logical sequence, ignore any out-of-order messages
            if (request.getSequence() != response.getSequence()) {
                LOG.debug("Expecting sequence {}, ignoring response {}", request.getSequence(), envelope);
                return NotPresentEntry.INSTANCE;
            }

            // Check if the entry has (ever) been transmitted
            if (!(ce instanceof TransmittedConnectionEntry tce)) {
                return NotPresentEntry.INSTANCE;
            }

            // Now check session match
            if (envelope.getSessionId() != tce.getSessionId()) {
                LOG.debug("Expecting session {}, ignoring response {}", tce.getSessionId(), envelope);
                return NotPresentEntry.INSTANCE;
            }
            if (envelope.getTxSequence() != tce.getTxSequence()) {
                LOG.warn("Expecting txSequence {}, ignoring response {}", tce.getTxSequence(), envelope);
                return NotPresentEntry.INSTANCE;
            }

            LOG.debug("Completing request {} with {}", request, envelope);
            it.remove();
            return new FoundEntry(tce);
        }

        return null;
    }
}
