/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import com.google.common.base.Verify;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/*
 * TODO: make this class and its users thread-safe. This will require some atomic state-keeping so that timeouts,
 *       retries and enqueues work as expected.
 */
@NotThreadSafe
final class SequencedQueue {
    private static final Logger LOG = LoggerFactory.getLogger(SequencedQueue.class);

    // Keep these constant in nanoseconds, as that prevents unnecessary conversions in the fast path
    @VisibleForTesting
    static final long NO_PROGRESS_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(15);
    @VisibleForTesting
    static final long REQUEST_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(30);
    private static final FiniteDuration INITIAL_REQUEST_TIMEOUT = FiniteDuration.apply(REQUEST_TIMEOUT_NANOS,
        TimeUnit.NANOSECONDS);

    /**
     * Default number of permits we start with. This value is used when we start up only, once we resolve a backend
     * we will use its advertized {@link BackendInfo#getMaxMessages()} forever, refreshing the value on each successful
     * resolution.
     */
    private static final int DEFAULT_TX_LIMIT = 1000;

    private final Ticker ticker;
    private final Long cookie;

    /*
     * We need to keep the sequence of operations towards the backend and rate-limit what we send out, possibly dealing
     * with the limit changing between reconnects (which imply retransmission).
     *
     * We keep three queues: one for requests that have been sent to the last known backend (until we have a new one),
     * one for requests that have been sent to the previous backend (and have not been transmitted to the current one),
     * and one for requests which have not been transmitted at all.
     *
     * When transmitting we first try to drain the second queue and service the third one only when that becomes empty.
     * When receiving, we look at the first two -- as the response has to match a transmitted request. Since responses
     * can get re-ordered, we may end up receiving responses to previously-sent requests before we have a chance
     * to retransmit -- hence the second queue.
     */
    private Queue<SequencedQueueEntry> currentInflight = new ArrayDeque<>();
    private Queue<SequencedQueueEntry> lastInflight = new ArrayDeque<>();
    private final Queue<SequencedQueueEntry> pending = new ArrayDeque<>();

    /**
     * Last scheduled resolution request. We do not use this object aside from requiring it as a proof that when
     * resolution occurs via {@link #setBackendInfo(CompletionStage, BackendInfo)}, we only update the last requested
     * result.
     */
    private CompletionStage<? extends BackendInfo> backendProof;
    private BackendInfo backend;

    // This is not final because we need to be able to replace it.
    private long txSequence;

    private int lastTxLimit = DEFAULT_TX_LIMIT;

    /**
     * Last scheduled timer. We use this to prevent multiple timers from being scheduled for this queue.
     */
    private Object expectingTimer;

    private long lastProgress;

    // Updated from application thread
    private volatile boolean notClosed = true;

    SequencedQueue(final Long cookie, final Ticker ticker) {
        this.cookie = Preconditions.checkNotNull(cookie);
        this.ticker = Preconditions.checkNotNull(ticker);
        lastProgress = ticker.read();
    }

    Long getCookie() {
        return cookie;
    }

    private void checkNotClosed() {
        Preconditions.checkState(notClosed, "Queue %s is closed", this);
    }

    private long nextTxSequence() {
        return txSequence++;
    }

    /**
     * Enqueue, and possibly transmit a request. Results of this method are tri-state, indicating to the caller
     * the following scenarios:
     * 1) The request has been enqueued and transmitted. No further actions are necessary
     * 2) The request has been enqueued and transmitted, but the caller needs to schedule a new timer
     * 3) The request has been enqueued, but the caller needs to request resolution of backend information and that
     *    process needs to complete before transmission occurs
     *
     * These options are covered via returning an {@link Optional}. The caller needs to examine it and decode
     * the scenarios above according to the following rules:
     * - if is null, the first case applies
     * - if {@link Optional#isPresent()} returns false, the third case applies and the caller should initiate backend
     *      resolution and eventually call {@link #setBackendInfo(CompletionStage, BackendInfo)}
     * - if {@link Optional#isPresent()} returns true, the second case applies and the caller MUST schedule a timer
     *
     * @param request Request to be sent
     * @param callback Callback to be invoked
     * @return Optional duration with semantics described above.
     */
    @Nullable Optional<FiniteDuration> enqueueRequest(final Request<?, ?> request, final RequestCallback callback) {
        checkNotClosed();

        final long now = ticker.read();
        final SequencedQueueEntry e = new SequencedQueueEntry(request, callback, now);
        if (backend == null) {
            LOG.debug("No backend available, request resolution");
            pending.add(e);
            return Optional.empty();
        }
        if (!lastInflight.isEmpty()) {
            LOG.debug("Retransmit not yet complete, delaying request {}", request);
            pending.add(e);
            return null;
        }
        if (currentInflight.size() >= lastTxLimit) {
            LOG.debug("Queue is at capacity, delayed sending of request {}", request);
            pending.add(e);
            return null;
        }

        // Ready to transmit
        currentInflight.offer(e);
        LOG.debug("Enqueued request {} to queue {}", request, this);

        e.retransmit(backend, nextTxSequence(), now);
        if (expectingTimer == null) {
            expectingTimer = now + REQUEST_TIMEOUT_NANOS;
            return Optional.of(INITIAL_REQUEST_TIMEOUT);
        } else {
            return null;
        }
    }

    /*
     * We are using tri-state return here to indicate one of three conditions:
     * - if a matching entry is found, return an Optional containing it
     * - if a matching entry is not found, but it makes sense to keep looking at other queus, return null
     * - if a conflicting entry is encountered, indicating we should ignore this request, return an empty Optional
     */
    private static Optional<SequencedQueueEntry> findMatchingEntry(final Queue<SequencedQueueEntry> queue,
            final ResponseEnvelope<?> envelope) {
        // Try to find the request in a queue. Responses may legally come back in a different order, hence we need
        // to use an iterator
        final Iterator<SequencedQueueEntry> it = queue.iterator();
        while (it.hasNext()) {
            final SequencedQueueEntry e = it.next();
            final TxDetails txDetails = Verify.verifyNotNull(e.getTxDetails());

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

            // Now check session match
            if (envelope.getSessionId() != txDetails.getSessionId()) {
                LOG.debug("Expecting session {}, ignoring response {}", txDetails.getSessionId(), envelope);
                return Optional.empty();
            }
            if (envelope.getTxSequence() != txDetails.getTxSequence()) {
                LOG.warn("Expecting txSequence {}, ignoring response", txDetails.getTxSequence(), envelope);
                return Optional.empty();
            }

            LOG.debug("Completing request {} with {}", request, envelope);
            it.remove();
            return Optional.of(e);
        }

        return null;
    }

    ClientActorBehavior complete(final ClientActorBehavior current, final ResponseEnvelope<?> envelope) {
        Optional<SequencedQueueEntry> maybeEntry = findMatchingEntry(currentInflight, envelope);
        if (maybeEntry == null) {
            maybeEntry = findMatchingEntry(lastInflight, envelope);
        }

        if (maybeEntry == null || !maybeEntry.isPresent()) {
            LOG.debug("No request matching {} found", envelope);
            return current;
        }

        lastProgress = ticker.read();
        final ClientActorBehavior ret = maybeEntry.get().complete(envelope.getMessage());

        // We have freed up a slot, try to transmit something
        if (backend != null) {
            final int toSend = lastTxLimit - currentInflight.size();
            if (toSend > 0) {
                runTransmit(toSend);
            }
        }

        return ret;
    }

    private int transmitEntries(final Queue<SequencedQueueEntry> queue, final int count) {
        int toSend = count;

        while (toSend > 0) {
            final SequencedQueueEntry e = lastInflight.poll();
            if (e == null) {
                break;
            }

            LOG.debug("Transmitting entry {}", e);
            e.retransmit(backend, nextTxSequence(), lastProgress);
            toSend--;
        }

        return toSend;
    }

    private void runTransmit(final int count) {
        final int toSend;

        // Process lastInflight first, possibly clearing it
        if (!lastInflight.isEmpty()) {
            toSend = transmitEntries(lastInflight, count);
            if (lastInflight.isEmpty()) {
                // We won't be needing the queue anymore, change it to specialized implementation
                lastInflight = EmptyQueue.getInstance();
            }
        } else {
            toSend = count;
        }

        // Process pending next.
        transmitEntries(pending, toSend);
    }

    Optional<FiniteDuration> setBackendInfo(final CompletionStage<? extends BackendInfo> proof, final BackendInfo backend) {
        Preconditions.checkNotNull(backend);
        if (!proof.equals(backendProof)) {
            LOG.debug("Ignoring resolution {} while waiting for {}", proof, this.backendProof);
            return Optional.empty();
        }

        LOG.debug("Resolved backend {}",  backend);

        // We are un-blocking transmission, but we need to juggle the queues first to get retransmit order right
        // and also not to exceed new limits
        final Queue<SequencedQueueEntry> newLast = new ArrayDeque<>(currentInflight.size() + lastInflight.size());
        newLast.addAll(currentInflight);
        newLast.addAll(lastInflight);
        lastInflight = newLast.isEmpty() ? EmptyQueue.getInstance() : newLast;

        // Clear currentInflight, possibly compacting it
        final int txLimit = backend.getMaxMessages();
        if (lastTxLimit > txLimit) {
            currentInflight = new ArrayDeque<>();
        } else {
            currentInflight.clear();
        }

        // We are ready to roll
        this.backend = backend;
        backendProof = null;
        txSequence = 0;
        lastTxLimit = txLimit;
        lastProgress = ticker.read();

        // No pending requests, return
        if (lastInflight.isEmpty() && pending.isEmpty()) {
            return Optional.empty();
        }

        LOG.debug("Sending up to {} requests to backend {}", txLimit, backend);

        runTransmit(lastTxLimit);

        // Calculate next timer if necessary
        if (expectingTimer == null) {
            // Request transmission may have cost us some time. Recalculate timeout.
            final long nextTicks = ticker.read() + REQUEST_TIMEOUT_NANOS;
            expectingTimer = nextTicks;
            return Optional.of(FiniteDuration.apply(nextTicks - lastProgress, TimeUnit.NANOSECONDS));
        } else {
            return Optional.empty();
        }
    }

    boolean expectProof(final CompletionStage<? extends BackendInfo> proof) {
        if (!proof.equals(backendProof)) {
            LOG.debug("Setting resolution handle to {}", proof);
            backendProof = proof;
            return true;
        } else {
            LOG.trace("Already resolving handle {}", proof);
            return false;
        }
    }

    boolean hasCompleted() {
        return !notClosed && currentInflight.isEmpty() && lastInflight.isEmpty() && pending.isEmpty();
    }

    /**
     * Check queue timeouts and return true if a timeout has occured.
     *
     * @return True if a timeout occured
     * @throws NoProgressException if the queue failed to make progress for an extended
     *                             time.
     */
    boolean runTimeout() throws NoProgressException {
        expectingTimer = null;
        final long now = ticker.read();

        if (!currentInflight.isEmpty()) {
            final long ticksSinceProgress = now - lastProgress;
            if (ticksSinceProgress >= NO_PROGRESS_TIMEOUT_NANOS) {
                LOG.error("Queue {} has not seen progress in {} seconds, failing all requests", this,
                    TimeUnit.NANOSECONDS.toSeconds(ticksSinceProgress));

                final NoProgressException ex = new NoProgressException(ticksSinceProgress);
                poison(ex);
                throw ex;
            }
        }

        // We always schedule requests in sequence, hence any timeouts really just mean checking the head of the queue
        final SequencedQueueEntry head = currentInflight.peek();
        if (head != null && head.isTimedOut(now, REQUEST_TIMEOUT_NANOS)) {
            backend = null;
            LOG.debug("Queue {} invalidated backend info", this);
            return true;
        } else {
            return false;
        }
    }

    private static void poisonQueue(final Queue<SequencedQueueEntry> queue, final RequestException cause) {
        queue.forEach(e -> e.poison(cause));
        queue.clear();
    }

    void poison(final RequestException cause) {
        close();

        poisonQueue(currentInflight, cause);
        poisonQueue(lastInflight, cause);
        poisonQueue(pending, cause);
    }

    // FIXME: add a caller from ClientSingleTransaction
    void close() {
        notClosed = false;
    }
}
