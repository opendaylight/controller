/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
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
     * We allow request completion strictly in sequence order, but need access to the last sequence for diagnostic
     * purposes, hence we use an internal queue. Entries enqueued in this queue must be ordered by sequence number.
     *
     * FIXME: this means that once we assign sequence numbers, sequence number allocation and enqueue must be atomic
     *        from the point of view of the client actor. This queue and the counter will need to be guarded with
     *        a synchronized block.
     */
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

    /**
     * Last scheduled timer. We use this to prevent multiple timers from being scheduled for this queue.
     */
    private Object expectingTimer;

    private long lastProgress;
    private long sequence;

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

    /**
     * Enqueue, and possibly transmit a request. Results of this method are tri-state, indicating to the caller
     * the following scenarios:
     * 1) The request has been enqueued and transmitted. No further actions are necessary
     * 2) The request has been enqueued and transmitted, but the caller needs to schedule a new timer
     * 3) The request has been enqueued,but the caller needs to request resolution of backend information and that
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
        // FIXME: enable this once we are the ones assigning the sequence number
        //final SequencedQueueEntry last = entries.peekLast();
        //if (last != null) {
        //    Preconditions.checkArgument(Long.compareUnsigned(last.getSequence(), request.getSequence()) < 0,
        //        "Mis-sequenced request %s, tail is %s", request, last);
        //}

        // We could have check first, but argument checking needs to happen first
        checkNotClosed();

        final long now = ticker.read();
        final SequencedQueueEntry e = new SequencedQueueEntry(request, callback, sequence++, now);

        queue.add(e);
        LOG.debug("Enqueued request {} to queue {}", request, this);

        if (backend == null) {
            return Optional.empty();
        }

        e.retransmit(backend, now);
        if (expectingTimer == null) {
            expectingTimer = now + REQUEST_TIMEOUT_NANOS;
            return Optional.of(INITIAL_REQUEST_TIMEOUT);
        } else {
            return null;
        }
    }

    ClientActorBehavior complete(final ClientActorBehavior current, final ResponseEnvelope<?> response) {
        // Responses to different targets may arrive out of order, hence we use an iterator
        final Iterator<SequencedQueueEntry> it = queue.iterator();
        while (it.hasNext()) {
            final SequencedQueueEntry e = it.next();
            if (e.acceptsResponse(response)) {
                lastProgress = ticker.read();
                it.remove();
                LOG.debug("Completing request {} with {}", e, response);
                return e.complete(response.getMessage());
            }
        }

        LOG.debug("No request matching {} found", response);
        return current;

// FIXME: reactivate when we assign sequences
        // No forward progress, this is just detailed logging. It involves some CPU cycles, hence we guard it
        // with a check if logging is enabled at all.
//        if (LOG.isDebugEnabled()) {
//
//
//            if (Long.compareUnsigned(sequence, queue.peekFirst().getSequence()) > 0) {
//                final SequencedQueueEntry tail = queue.peekLast();
//                if (Long.compareUnsigned(sequence, tail.getSequence()) > 0) {
//                    LOG.debug("Ignoring unknown response {}, last request is {}", response, tail);
//                } else {
//                    LOG.debug("Ignoring out-of-sequence response {}, next expected is {}", response, queue.peek());
//                }
//            } else {
//                LOG.debug("Ignoring duplicate response {}", response);
//            }
//        }
    }

    Optional<FiniteDuration> setBackendInfo(final CompletionStage<? extends BackendInfo> proof, final BackendInfo backend) {
        if (!proof.equals(backendProof)) {
            LOG.debug("Ignoring resolution {} while waiting for {}", proof, this.backendProof);
            return Optional.empty();
        }

        this.backend = Preconditions.checkNotNull(backend);
        backendProof = null;
        LOG.debug("Resolved backend {}",  backend);

        if (queue.isEmpty()) {
            // No pending requests, hence no need for a timer
            return Optional.empty();
        }

        LOG.debug("Resending requests to backend {}", backend);
        final long now = ticker.read();
        for (SequencedQueueEntry e : queue) {
            e.retransmit(backend, now);
        }

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
     * @return
     * @throws NoProgressException
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
