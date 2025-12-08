/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Verify;
import java.util.ArrayDeque;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.IncrementTransactionSequenceRequest;
import org.opendaylight.controller.cluster.access.commands.IncrementTransactionSequenceSuccess;
import org.opendaylight.controller.cluster.access.commands.OutOfOrderRequestException;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend common transaction state as observed by the shard leader. This class is NOT thread-safe.
 */
abstract sealed class FrontendTransaction implements Identifiable<TransactionIdentifier>
        permits FrontendReadOnlyTransaction, FrontendReadWriteTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(FrontendTransaction.class);

    /**
     * It is possible that after we process a request and send a response that response gets lost and the client
     * initiates a retry. Since subsequent requests can mutate transaction state we need to retain the response until
     * it is acknowledged by the client.
     */
    private final ArrayDeque<Object> replayQueue = new ArrayDeque<>();
    private final @NonNull AbstractFrontendHistory history;
    private final @NonNull TransactionIdentifier id;

    private long firstReplaySequence;
    private Long lastPurgedSequence;
    private long expectedSequence;
    private RequestException previousFailure;

    FrontendTransaction(final AbstractFrontendHistory history, final TransactionIdentifier id) {
        this.history = requireNonNull(history);
        this.id = requireNonNull(id);
    }

    @Override
    public final TransactionIdentifier getIdentifier() {
        return id;
    }

    final @NonNull AbstractFrontendHistory history() {
        return history;
    }

    final String persistenceId() {
        return history.persistenceId();
    }

    final Optional<TransactionSuccess<?>> replaySequence(final long sequence) throws RequestException {
        // Fast path check: if the requested sequence is the next request, bail early
        if (expectedSequence == sequence) {
            return Optional.empty();
        }

        // Check sequencing: we do not need to bother with future requests
        if (Long.compareUnsigned(expectedSequence, sequence) < 0) {
            throw new OutOfOrderRequestException(expectedSequence);
        }

        // Sanity check: if we have purged sequences, this has to be newer
        if (lastPurgedSequence != null && Long.compareUnsigned(lastPurgedSequence, sequence) >= 0) {
            // Client has sent a request sequence, which has already been purged. This is a hard error, which should
            // never occur. Throwing an IllegalArgumentException will cause it to be wrapped in a
            // RuntimeRequestException (which is not retriable) and report it back to the client.
            throw new IllegalArgumentException(String.format("Invalid purged sequence %s (last purged is %s)",
                sequence, lastPurgedSequence));
        }

        // At this point we have established that the requested sequence lies in the open interval
        // (lastPurgedSequence, expectedSequence). That does not actually mean we have a response, as the commit
        // machinery is asynchronous, hence a reply may be in the works and not available.

        long replaySequence = firstReplaySequence;
        for (Object replay : replayQueue) {
            if (replaySequence == sequence) {
                if (replay instanceof RequestException) {
                    throw (RequestException) replay;
                }

                Verify.verify(replay instanceof TransactionSuccess);
                return Optional.of((TransactionSuccess<?>) replay);
            }

            replaySequence++;
        }

        // Not found
        return Optional.empty();
    }

    final void purgeSequencesUpTo(final long sequence) {
        // FIXME: implement this

        lastPurgedSequence = sequence;
    }

    // Request order has already been checked by caller and replaySequence()
    @SuppressWarnings("checkstyle:IllegalCatch")
    final @Nullable TransactionSuccess<?> handleRequest(final TransactionRequest<?> request,
            final RequestEnvelope envelope, final long now) throws RequestException {
        if (request instanceof final IncrementTransactionSequenceRequest incr) {
            expectedSequence += incr.getIncrement();

            return recordSuccess(incr.getSequence(),
                    new IncrementTransactionSequenceSuccess(incr.getTarget(), incr.getSequence()));
        }

        if (previousFailure != null) {
            LOG.debug("{}: Rejecting request {} due to previous failure", persistenceId(), request, previousFailure);
            throw previousFailure;
        }

        try {
            return doHandleRequest(request, envelope, now);
        } catch (RuntimeException e) {
            /*
             * The request failed to process, we should not attempt to ever
             * apply it again. Furthermore we cannot accept any further requests
             * from this connection, simply because the transaction state is
             * undefined.
             */
            LOG.debug("{}: Request {} failed to process", persistenceId(), request, e);
            previousFailure = new RuntimeRequestException("Request " + request + " failed to process", e);
            throw previousFailure;
        }
    }

    abstract @Nullable TransactionSuccess<?> doHandleRequest(TransactionRequest<?> request, RequestEnvelope envelope,
            long now) throws RequestException;

    abstract void retire();

    @NonNullByDefault
    private void recordResponse(final long sequence, final Object response) {
        if (replayQueue.isEmpty()) {
            firstReplaySequence = sequence;
        }
        replayQueue.add(response);
        expectedSequence++;
    }

    final <T extends TransactionSuccess<?>> @NonNull T recordSuccess(final long sequence, final @NonNull T success) {
        recordResponse(sequence, success);
        return success;
    }

    private long executionTime(final long startTime) {
        return history.readTime() - startTime;
    }

    @NonNullByDefault
    final void recordAndSendSuccess(final RequestEnvelope envelope, final long startTime,
            final TransactionSuccess<?> success) {
        recordResponse(success.getSequence(), success);
        envelope.sendSuccess(success, executionTime(startTime));
    }

    @NonNullByDefault
    final void recordAndSendFailure(final RequestEnvelope envelope, final long startTime,
            final RuntimeRequestException failure) {
        recordResponse(envelope.getMessage().getSequence(), failure);
        envelope.sendFailure(failure, executionTime(startTime));
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues().add("identifier", getIdentifier())
                .add("expectedSequence", expectedSequence).add("firstReplaySequence", firstReplaySequence)
                .add("lastPurgedSequence", lastPurgedSequence)
                .toString();
    }
}
