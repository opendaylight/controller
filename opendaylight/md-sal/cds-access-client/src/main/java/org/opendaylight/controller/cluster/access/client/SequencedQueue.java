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
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
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

    /**
     * We need to keep the sequence of operations towards the backend, hence we use a queue. Since targets can
     * progress at different speeds, these may be completed out of order.
     *
     * TODO: The combination of target and sequence uniquely identifies a particular request, we will need to
     *       figure out a more efficient lookup mechanism to deal with responses which do not match the queue
     *       order.
     */
    private final Semaphore txSemaphore = new Semaphore(DEFAULT_TX_LIMIT);
    private final Deque<SequencedQueueEntry> queue = new LinkedList<>();
    private final Ticker ticker;
    private final Long cookie;

    // Updated/consulted from actor context only
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

        queue.add(e);
        LOG.debug("Enqueued request {} to queue {}", request, this);

        if (backend == null) {
            LOG.debug("No backend available, request resolution");
            return Optional.empty();
        }

        if (!txSemaphore.tryAcquire()) {
            LOG.debug("Queue is at capacity, delayed sending of request {}", request);
            return null;
        }
        try {
            e.retransmit(backend, nextTxSequence(), now);
            if (expectingTimer == null) {
                expectingTimer = now + REQUEST_TIMEOUT_NANOS;
                return Optional.of(INITIAL_REQUEST_TIMEOUT);
            } else {
                return null;
            }
        } finally {
            txSemaphore.release();
        }
    }

    ClientActorBehavior complete(final ClientActorBehavior current, final ResponseEnvelope<?> response) {
        // Responses to different targets may arrive out of order, hence we use an iterator
        final Iterator<SequencedQueueEntry> it = queue.iterator();
        while (it.hasNext()) {
            final SequencedQueueEntry e = it.next();
            if (e.matchesResponse(response.getMessage())) {
                if (e.matchesSequence(response.getSessionId(), response.getTxSequence())) {
                    lastProgress = ticker.read();
                    it.remove();
                    LOG.debug("Completing request {} with {}", e, response);
                    return e.complete(response.getMessage());
                }

                return current;
            }
        }

        LOG.debug("No request matching {} found", response);
        return current;
    }

    private void adjustSemaphore(final int newTxLimit) {
        final int diff = lastTxLimit - newTxLimit;
        LOG.debug("Adjustin semaphore {} from {} to {}", txSemaphore, lastTxLimit, newTxLimit);

        if (diff > 0) {
            // Blocking here, we are not transmitting, so this should be fine
            txSemaphore.acquireUninterruptibly(diff);
        } else {
            // This is non-blocking
            txSemaphore.release(-diff);
        }

        lastTxLimit = newTxLimit;
        LOG.debug("Adjusted semaphore {} to {}", txSemaphore, newTxLimit);
    }

    Optional<FiniteDuration> setBackendInfo(final CompletionStage<? extends BackendInfo> proof, final BackendInfo backend) {
        Preconditions.checkNotNull(backend);
        if (!proof.equals(backendProof)) {
            LOG.debug("Ignoring resolution {} while waiting for {}", proof, this.backendProof);
            return Optional.empty();
        }

        // We are unblocking transmission. Make sure to update transmit limit if needed
        final int txLimit = backend.getMaxMessages();
        if (lastTxLimit != txLimit) {
            adjustSemaphore(txLimit);
        }

        // This prevents any further transmissions until we finish processing here
        int permits = txSemaphore.drainPermits();

        this.backend = backend;
        backendProof = null;
        txSequence = 0;
        lastProgress = ticker.read();
        LOG.debug("Resolved backend {}",  backend);

        if (queue.isEmpty()) {
            // No pending requests, hence no need for a timer, remember to return our permits
            txSemaphore.release(permits);
            return Optional.empty();
        }

        LOG.debug("Resending up to {} requests to backend {}", permits, backend);
        final long now = ticker.read();

        for (final Iterator<SequencedQueueEntry> it = queue.iterator(); permits > 0 && it.hasNext(); --permits) {
            final SequencedQueueEntry e = it.next();

            LOG.debug("Transmitting entry {}", e);
            e.retransmit(backend, nextTxSequence(), now);
        }

        txSemaphore.release(permits);

        if (expectingTimer != null) {
            // We already have a timer going, no need to schedule a new one
            return Optional.empty();
        }

        // Above loop may have cost us some time. Recalculate timeout.
        final long nextTicks = ticker.read() + REQUEST_TIMEOUT_NANOS;
        expectingTimer = nextTicks;
        return Optional.of(FiniteDuration.apply(nextTicks - now, TimeUnit.NANOSECONDS));
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
        return !notClosed && queue.isEmpty();
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

        if (!queue.isEmpty()) {
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
        final SequencedQueueEntry head = queue.peek();
        if (head != null && head.isTimedOut(now, REQUEST_TIMEOUT_NANOS)) {
            backend = null;
            LOG.debug("Queue {} invalidated backend info", this);
            return true;
        } else {
            return false;
        }
    }

    void poison(final RequestException cause) {
        close();

        SequencedQueueEntry e = queue.poll();
        while (e != null) {
            e.poison(cause);
            e = queue.poll();
        }
    }

    // FIXME: add a caller from ClientSingleTransaction
    void close() {
        notClosed = false;
    }
}
