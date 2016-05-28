/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Deque;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.WritableIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
final class SequencedQueue {
    private static final class RequestEntry<I extends WritableIdentifier> {
        final Request<I, ?> request;
        final RequestCallback<I> callback;
        final long started;

        RequestEntry(final Request<I, ?> request, final RequestCallback<I> callback, final long started) {
            this.request = Preconditions.checkNotNull(request);
            this.callback = Preconditions.checkNotNull(callback);
            this.started = started;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SequencedQueue.class);
    private static final long RETRY_NANOS = TimeUnit.SECONDS.toNanos(30);

    /**
     * We allow request completion strictly in sequence order, but need access to the last sequence for diagnostic
     * purposes.
     *
     * TODO: this could benefit from a more relaxed MPSC queue than {@link ConcurrentLinkedDeque}.
     */
    private final Deque<RequestEntry<?>> entries = new ConcurrentLinkedDeque<>();
    private volatile boolean notClosed = true;
    private long lastProgress;

    SequencedQueue(final Ticker ticker) {
        lastProgress = ticker.read();
    }

    private void checkNotClosed() {
        Preconditions.checkState(notClosed, "Queue %s is closed", this);
    }

    <I extends WritableIdentifier, T extends Request<I, T>> void add(final T request, final RequestCallback<I> callback,
            final long now) {
        final RequestEntry<I> e = new RequestEntry<>(request, callback, now);
        final RequestEntry<?> last = entries.peekLast();
        if (last != null) {
            Preconditions.checkArgument(Long.compareUnsigned(last.request.getSequence(), request.getSequence()) < 0,
                "Mis-sequenced request %s, tail is %s", request, last);
        }

        // We could have check first, but argument checking needs to happen first
        checkNotClosed();
        entries.add(e);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    ClientActorBehavior complete(final ClientActorBehavior current, final Response<?, ?> response) {
        final RequestEntry<?> head = entries.peekFirst();
        if (head == null) {
            LOG.debug("No outstanding requests, ignoring response {}", response);
            return current;
        }

        // Happy path
        final long sequence = response.getSequence();
        if (sequence == head.request.getSequence()) {
            LOG.debug("Completing request {} with {}", head.request, response);
            lastProgress = current.context().ticker().read();
            return entries.pollFirst().callback.complete((Response)response);
        }

        // No forward progress, this is just detailed logging. It involves some CPU cycles, hence we guard it
        // with a check if logging is enabled at all.
        if (LOG.isDebugEnabled()) {
            if (Long.compareUnsigned(sequence, head.request.getSequence()) > 0) {
                final RequestEntry<?> tail = entries.peekLast();
                if (Long.compareUnsigned(sequence, tail.request.getSequence()) > 0) {
                    LOG.debug("Ignoring unknown response {}, last request is {}", response, tail.request);
                } else {
                    LOG.debug("Ignoring out-of-sequence response {}, next expected is {}", response, head.request);
                }
            } else {
                LOG.debug("Ignoring duplicate response {}", response);
            }
        }

        return current;
    }

    // FIXME: add a caller in ClientSingleTransaction
    void close() {
        notClosed = false;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    void poison(final RequestException cause) {
        close();

        RequestEntry<?> e = entries.poll();
        while (e != null) {
            LOG.trace("Poisoning request {}", e.request);
            e.callback.complete((Response) e.request.toRequestFailure(cause));
            e = entries.poll();
        }
    }

    boolean hasCompleted() {
        return !notClosed && entries.isEmpty();
    }

    // FIXME: semantics is not right ...
    long processTimeouts(final long now, final long maxElapsed, final Consumer<Entry<Request<?, ?>, RequestCallback<?>>> callback) {
        final RequestEntry<?> head = entries.peekFirst();
        if (head == null) {
            // We have no outstanding items.
            return Long.MAX_VALUE;
        }

        final long elapsed = now - head.started;
        if (maxElapsed <= elapsed) {
            LOG.debug("Request {} timed out", head.request);
            callback.accept(new SimpleImmutableEntry<>(head.request, head.callback));
            return Long.MIN_VALUE;
        }

        return maxElapsed;
    }

    void retryRequest(final RequestFailure<?, ?> failure, final BackendInfoResolver<?> resolver) {
        // TODO Auto-generated method stub
    }
}
